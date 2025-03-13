package com.yoursunucu;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordBot extends ListenerAdapter implements Listener {
    private static final String TOKEN = "YOUR BOT TOKEN";
    private static final String LOG_CHANNEL_ID = "DEFAULT LOG CHANNEL ID (DISCORD)";
    
    private JDA jda;
    private TextChannel logChannel;
    public final JavaPlugin plugin;

    public DiscordBot(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            // 1. Discord Bağlantısını Kur
            jda = JDABuilder.createDefault(TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordListener(this))
                .build();
            
            // 2. Minecraft Eventlerini Kaydet
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        logChannel = jda.getTextChannelById(LOG_CHANNEL_ID);
        if(logChannel != null) {
        }
    }

    // 3. Minecraft Eventleri
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String log = String.format("[JOIN] %s | IP: %s | UUID: %s",
            player.getName(),
            player.getAddress().getAddress().getHostAddress(),
            player.getUniqueId());
        
        sendLog(log);
    }
    @EventHandler
    public void onPlayerQuit(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String log = String.format("[QUIT] %s | IP: %s | UUID: %s",
                player.getName(),
                player.getAddress().getAddress().getHostAddress(),
                player.getUniqueId());

        sendLog(log);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String log = String.format("[COMMAND] %s: %s",
            e.getPlayer().getName(),
            e.getMessage());
        
        sendLog(log);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        String log = String.format("[CHAT] %s: %s",
            e.getPlayer().getName(),
            e.getMessage());
        
        sendLog(log);
    }

    public void sendLog(String message) {
        TextChannel channel = jda.getTextChannelById(LOG_CHANNEL_ID);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    public void shutdown() {
        if(jda != null) jda.shutdown();
    }
}