package com.euandrelucas.herobrine.listener;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.director.Events;
import com.euandrelucas.herobrine.item.Items;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Altar: base 3x3 de blocos de ouro, netherrack no centro e 4 tochas de redstone ao redor dele.
 * - Acender o fogo em cima: invoca o chefe (ritual clássico).
 * - Clique direito no netherrack com Diamante Amaldiçoado: desperta o Herobrine no mundo.
 * - Clique direito com Diamante Purificado: purifica o mundo e expulsa o Herobrine.
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

        Block netherrack = e.getBlock().getRelative(BlockFace.DOWN);
        if (netherrack.getType() != Material.NETHERRACK || !isAltar(netherrack)) return;
        if (plugin.worldState().isPurified(p.getWorld())) {
            p.sendMessage(Util.color("§7Nada responde. Este mundo foi purificado."));
            return;
        }
        if (plugin.presence().get(p) < plugin.getConfig().getDouble("altar.boss-min-presence", 0)) {
            p.sendMessage(Util.color("§7O fogo apenas queima."));
            return;
        }
        if (plugin.apparitions().has(p)) return;

        Location spot = Events.viewSpot(p, 9, 15, 360);
        if (spot == null) return;
        strike(netherrack);
        p.sendMessage(Util.color("§4§oVocê o chamou."));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) plugin.apparitions().spawn(p, Apparition.Mode.BOSS, spot);
        }, 40L);
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.NETHERRACK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        Items.Id id = plugin.items().idOf(hand);
        if (id != Items.Id.CURSED_DIAMOND && id != Items.Id.PURIFIED_DIAMOND) return;
        e.setCancelled(true);
        if (!p.hasPermission("herobrine.altar")) return;
        if (!isAltar(clicked)) {
            p.sendMessage(Util.color("§7O diamante pulsa, mas não há altar completo aqui."));
            return;
        }
        World w = p.getWorld();
        hand.setAmount(hand.getAmount() - 1);

        if (id == Items.Id.CURSED_DIAMOND) {
            plugin.worldState().setSummoned(w, true);
            strike(clicked);
            for (Player pl : w.getPlayers()) pl.sendMessage(Util.color("§4§oAlgo despertou neste mundo."));
            Location spot = Events.viewSpot(p, 14, 22, 360);
            if (spot != null) plugin.apparitions().spawn(p, Apparition.Mode.STARE, spot);
        } else {
            plugin.worldState().setPurified(w, true);
            for (Player pl : w.getPlayers()) {
                plugin.apparitions().remove(pl);
                plugin.presence().set(pl, 0);
                pl.playSound(pl.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
                pl.sendMessage(Util.color("§f§oO Herobrine foi expulso deste mundo."));
            }
            w.spawnParticle(Particle.END_ROD, clicked.getLocation().add(0.5, 1, 0.5), 80, 1, 1, 1, 0.1);
        }
    }

    private void strike(Block b) {
        if (plugin.getConfig().getBoolean("altar.lightning", true)) {
            b.getWorld().strikeLightningEffect(b.getLocation().add(0.5, 0, 0.5));
        }
    }

    private boolean isAltar(Block netherrack) {
        Block base = netherrack.getRelative(BlockFace.DOWN);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Material m = base.getRelative(x, 0, z).getType();
                if (m != Material.GOLD_BLOCK && !(x == 0 && z == 0 && m == Material.MOSSY_COBBLESTONE)) return false;
            }
        }
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (netherrack.getRelative(f).getType() != Material.REDSTONE_TORCH) return false;
        }
        return true;
    }
}
