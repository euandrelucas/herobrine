package com.euandrelucas.herobrine.config;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Gerencia a leitura, recarga e validação do arquivo config.yml.
 */
public class ConfigManager {

    private final HerobrinePlugin plugin;
    private FileConfiguration config;

    public ConfigManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public boolean isDebug() {
        return config.getBoolean("plugin.debug", false);
    }

    public boolean isSpawnEnabled() {
        return config.getBoolean("spawn.enabled", true);
    }

    public int getSpawnIntervalMinutes() {
        return config.getInt("spawn.interval-minutes", 10);
    }

    public int getSpawnVarianceMinutes() {
        return config.getInt("spawn.random-variance-minutes", 3);
    }

    public double getSpawnChance() {
        return config.getDouble("spawn.chance", 0.45);
    }

    public int getMinSpawnDistance() {
        return config.getInt("spawn.min-distance", 15);
    }

    public int getMaxSpawnDistance() {
        return config.getInt("spawn.max-distance", 35);
    }

    public boolean isLimitOnePerWorld() {
        return config.getBoolean("spawn.limit-one-per-world", true);
    }

    public List<String> getDisabledWorlds() {
        return config.getStringList("spawn.disabled-worlds");
    }

    public boolean isWorldDisabled(String worldName) {
        List<String> disabled = getDisabledWorlds();
        return disabled != null && disabled.contains(worldName);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}
