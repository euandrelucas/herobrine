package com.euandrelucas.herobrine.util;

import com.euandrelucas.herobrine.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Queue;

/** Fila de alterações de blocos, aplicadas N por tick para não causar lag. */
public final class BlockQueue {

    private record Change(Location loc, BlockData data) {}

    private final HerobrinePlugin plugin;
    private final Queue<Change> queue = new ArrayDeque<>();
    private final BukkitTask task;

    public BlockQueue(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::drain, 1L, 1L);
    }

    public void set(Block block, Material material) {
        queue.add(new Change(block.getLocation(), material.createBlockData()));
    }

    public void set(Block block, BlockData data) {
        queue.add(new Change(block.getLocation(), data));
    }

    private void drain() {
        int budget = Math.max(1, plugin.getConfig().getInt("world-edit.blocks-per-tick", 40));
        while (budget-- > 0 && !queue.isEmpty()) {
            Change c = queue.poll();
            if (c.loc().getWorld() == null || !c.loc().isChunkLoaded()) continue;
            c.loc().getBlock().setBlockData(c.data(), false);
        }
    }

    public void stop() {
        task.cancel();
        queue.clear();
    }
}
