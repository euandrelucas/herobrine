package com.euandrelucas.herobrine.util;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.world.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/** Fila de alterações de blocos, aplicadas N por tick para não causar lag. */
public final class BlockQueue {

    private final HerobrinePlugin plugin;
    private final Queue<Placement> queue = new ArrayDeque<>();
    private final BukkitTask task;

    public BlockQueue(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::drain, 1L, 1L);
    }

    public void set(Block block, Material material) {
        queue.add(Placement.of(block, material));
    }

    public void set(Block block, BlockData data) {
        queue.add(Placement.of(block, data));
    }

    public void addAll(List<Placement> placements) {
        queue.addAll(placements);
    }

    private void drain() {
        int budget = Math.max(1, plugin.getConfig().getInt("world-edit.blocks-per-tick", 40));
        while (budget-- > 0 && !queue.isEmpty()) {
            Placement p = queue.poll();
            Block b = p.block();
            Location l = b.getLocation();
            if (l.getWorld() == null || !l.isChunkLoaded()) continue;
            b.setBlockData(p.data(), false);
            if (p.fake()) plugin.fakeBlocks().register(b, p.data().getMaterial());
            if (p.post() != null) p.post().accept(b);
        }
    }

    public void stop() {
        task.cancel();
        queue.clear();
    }
}
