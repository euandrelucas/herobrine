package com.euandrelucas.herobrine.fear;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o Sistema de Medo e Paranoia (0-100) por jogador com persistência.
 */
public class FearManager {

    private final HerobrinePlugin plugin;
    private final Map<UUID, Integer> playerFearMap = new HashMap<>();
    private File storageFile;
    private FileConfiguration storageConfig;

    public FearManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        loadStorage();
        startDecayTask();
    }

    private void loadStorage() {
        storageFile = new File(plugin.getDataFolder(), "fear_data.yml");
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Não foi possível criar fear_data.yml: " + e.getMessage());
            }
        }
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        for (String key : storageConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int fear = storageConfig.getInt(key, 0);
                playerFearMap.put(uuid, fear);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveStorage() {
        for (Map.Entry<UUID, Integer> entry : playerFearMap.entrySet()) {
            storageConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Erro ao salvar fear_data.yml: " + e.getMessage());
        }
    }

    public int getFear(Player player) {
        return playerFearMap.getOrDefault(player.getUniqueId(), 0);
    }

    public void setFear(Player player, int amount) {
        int clamped = Math.min(100, Math.max(0, amount));
        playerFearMap.put(player.getUniqueId(), clamped);
        saveStorage();
    }

    public void addFear(Player player, int amount) {
        setFear(player, getFear(player) + amount);
    }

    private void startDecayTask() {
        // Reduz 1 ponto de medo a cada 60 segundos
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : playerFearMap.keySet()) {
                int current = playerFearMap.get(uuid);
                if (current > 0) {
                    playerFearMap.put(uuid, current - 1);
                }
            }
        }, 1200L, 1200L);
    }
}
