package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia a Corrupção do Nether (elevação gradual do nível de lava e restauração).
 */
public class NetherCorruptionManager {

    private final HerobrinePlugin plugin;
    private final WorldEditManager worldEditManager;
    private final Map<Location, Material> snapshots = new HashMap<>();

    public NetherCorruptionManager(HerobrinePlugin plugin, WorldEditManager worldEditManager) {
        this.plugin = plugin;
        this.worldEditManager = worldEditManager;
    }

    /**
     * Inicia a sequência de corrupção do Nether perto do jogador.
     */
    public void startNetherCorruption(Player player) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NETHER) return;

        Location pLoc = player.getLocation();
        int radius = 15;
        int maxBlocks = plugin.getConfig().getInt("nether-mode.max-corrupted-blocks-per-session", 250);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= maxBlocks || !player.isOnline()) {
                    cancel();
                    return;
                }

                int x = pLoc.getBlockX() + (int) (Math.random() * radius * 2 - radius);
                int z = pLoc.getBlockZ() + (int) (Math.random() * radius * 2 - radius);
                int y = pLoc.getBlockY() - 1;

                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.NETHERRACK) {
                    // Salva snapshot para reversão
                    snapshots.putIfAbsent(block.getLocation(), block.getType());
                    worldEditManager.queueBlockChange(block.getLocation(), Material.LAVA);
                    count++;
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // 1 bloco de lava a cada 0.5s
    }

    /**
     * Reverte a área de corrupção do Nether restaurando os blocos originais.
     */
    public void restoreNetherSnapshot() {
        for (Map.Entry<Location, Material> entry : snapshots.entrySet()) {
            worldEditManager.queueBlockChange(entry.getKey(), entry.getValue());
        }
        snapshots.clear();
    }
}
