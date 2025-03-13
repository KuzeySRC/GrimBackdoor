package com.yoursunucu;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import java.util.ArrayList;

public class DiscordListener extends ListenerAdapter {
    private final DiscordBot bot;
    private final ArrayList<String> consoleChannels = new ArrayList<>();
    private final ArrayList<String> logChannels = new ArrayList<>();


    public DiscordListener(DiscordBot bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || event.isWebhookMessage()) return;

        String message = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();

        try {
            // Konsol komut kanalı kontrolü
            if(consoleChannels.contains(channelId)) {
                Bukkit.getScheduler().runTask(bot.plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
                });
                event.getChannel().sendMessage("✅ **Komut gönderildi:** `" + message + "`").queue();
                return;
            }

            // Komut işleme
            if(message.startsWith("!")) {
                String[] args = message.split(" ");
                switch(args[0].toLowerCase()) {
                    case "!logekle":
                        if(args.length > 1) {
                            logChannels.add(args[1]);
                            event.getChannel().sendMessage("✅ **Log kanalı eklendi:** " + args[1]).queue();
                        }
                        break;
                    case "!konsolekle":
                        if(args.length > 1) {
                            consoleChannels.add(args[1]);
                            event.getChannel().sendMessage("✅ **Konsol kanalı eklendi:** " + args[1]).queue();
                        }
                        break;
                }
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("**Hata:** " + e.getMessage()).queue();
        }
    }
}