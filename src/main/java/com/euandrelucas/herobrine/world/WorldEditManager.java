package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.compat.HookManager;
import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Gerencia alterações em lote no mundo de forma assíncrona/paginada.
 * Garante zero travamentos no servidor alterando no máximo N blocos por tick
 * e respeitando regiões protegidas por plugins de proteção.
 */
public class WorldEditManager {

    private final HerobrinePlugin plugin;
    private final ConfigManager configManager;
    private final HookManager hookManager;
    private final Queue<BlockChangeRequest> queue = new LinkedList<>();
    private BukkitTask processTask;

    public static class BlockChangeRequest {
        private final Location location;
        private final Material newMaterial;

        public BlockChangeRequest(Location location, Material newMaterial) {
            this.location = location.clone();
            this.newMaterial = newMaterial;
        }

        public Location getLocation() {
            return location;
        }

        public Material getNewMaterial() {
            return newMaterial;
        }
    }

    public WorldEditManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.hookManager = plugin.getHookManager();
        startProcessingTask();
    }

    public void queueBlockChange(Location location, Material newMaterial) {
        if (location == null || location.getWorld() == null) return;

        // Ignora alterações em áreas protegidas por WorldGuard/GriefPrevention/Towny
        if (hookManager.isLocationProtected(location)) return;

        synchronized (queue) {
            queue.add(new BlockChangeRequest(location, newMaterial));
        }
    }

    private void startProcessingTask() {
        this.processTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int maxPerTick = plugin.getConfig().getInt("world-editing.max-blocks-per-tick", 20);
            int processed = 0;

            synchronized (queue) {
                while (!queue.isEmpty() && processed < maxPerTick) {
                    BlockChangeRequest request = queue.poll();
                    if (request != null && request.getLocation().getWorld() != null) {
                        Block block = request.getLocation().getBlock();
                        block.setType(request.getNewMaterial(), false);
                    }
                    processed++;
                }
            }
        }, 1L, 1L);
    }

    public void shutdown() {
        if (processTask != null) {
            processTask.cancel();
        }
        synchronized (queue) {
            queue.clear();
        }
    }

    public int getQueueSize() {
        synchronized (queue) {
            return queue.size();
        }
    }
}
