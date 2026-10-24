package com.euandrelucas.herobrine.listener;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.director.Events;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

/**
 * Altar clássico: base 3x3 de blocos de ouro, netherrack no centro, 4 tochas de redstone
 * ao redor dele, e fogo aceso em cima.
 */
public final class AltarListener implements Listener {

    private final HerobrinePlugin plugin;

    public AltarListener(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        if (!plugin.getConfig().getBoolean("altar.enabled", true)) return;
        Player p = e.getPlayer();
        if (p == null || !p.hasPermission("herobrine.altar")) return;

        Block fire = e.getBlock();
        Block netherrack = fire.getRelative(BlockFace.DOWN);
        if (netherrack.getType() != Material.NETHERRACK || !isAltar(netherrack)) return;
        if (plugin.apparitions().has(p)) return;

        Location spot = Events.viewSpot(p, 8, 14, 360);
        if (spot == null) return;

        if (plugin.getConfig().getBoolean("altar.lightning", true)) {
            p.getWorld().strikeLightningEffect(netherrack.getLocation().add(0.5, 0, 0.5));
        }
        p.sendMessage(Util.color("§4§oVocê o chamou."));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) plugin.apparitions().spawn(p, Apparition.Mode.HOSTILE, spot);
        }, 40L);
    }

    private boolean isAltar(Block netherrack) {
        Block base = netherrack.getRelative(BlockFace.DOWN);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (base.getRelative(x, 0, z).getType() != Material.GOLD_BLOCK) return false;
            }
        }
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (netherrack.getRelative(f).getType() != Material.REDSTONE_TORCH) return false;
        }
        return true;
    }
}
