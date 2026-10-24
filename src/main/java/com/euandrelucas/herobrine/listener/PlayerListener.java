package com.euandrelucas.herobrine.listener;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.director.Events;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final HerobrinePlugin plugin;

    public PlayerListener(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.presence().onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.presence().onQuit(e.getPlayer());
        plugin.apparitions().remove(e.getPlayer());
        plugin.director().forget(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.apparitions().remove(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.apparitions().remove(e.getPlayer());
    }

    /** Aprendizado: quem escava muito no subsolo atrai túneis misteriosos. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (Util.underground(p)) plugin.director().noteMining(p);
    }

    /** Aprendizado: quem dorme acorda com a casa mexida. */
    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent e) {
        Player p = e.getPlayer();
        plugin.presence().add(p, plugin.getConfig().getDouble("presence.sleep", 2.0));
        if (plugin.presence().get(p) >= 20 && !p.hasPermission("herobrine.bypass") && Util.rng().nextDouble() < 0.5) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Events.wakeTrick(plugin, p);
            }, 20L);
        }
    }

    /** Bater no Herobrine: no modo hostil causa dano nele; nos outros ele some. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHitApparition(EntityDamageByEntityEvent e) {
        Apparition a = plugin.apparitions().find(e.getEntity());
        if (a == null) return;
        e.setCancelled(true);
        if (e.getDamager() instanceof Player p && p.equals(a.owner())) {
            double dmg = e.getDamage();
            if (a.mode() == Apparition.Mode.HOSTILE) dmg = Math.max(dmg, 2);
            a.onHit(dmg);
        }
    }
}
