package com.euandrelucas.herobrine.config;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gerencia a leitura, recarga e formatação das mensagens de messages.yml.
 */
public class MessagesManager {

    private final HerobrinePlugin plugin;
    private File configFile;
    private FileConfiguration messagesConfig;

    public MessagesManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(configFile);

        // Carrega defaults do JAR
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public String getPrefix() {
        return colorize(messagesConfig.getString("prefix", "&8[&cHerobrine&8] &r"));
    }

    public String getRawMessage(String path) {
        return messagesConfig.getString(path, "&c[Mensagem ausente: " + path + "]");
    }

    public String getFormattedMessage(String path, Map<String, String> placeholders) {
        String msg = getRawMessage(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return getPrefix() + colorize(msg);
    }

    public List<String> getSignMessages() {
        return messagesConfig.getStringList("horror.sign-messages");
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
