package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import com.euandrelucas.herobrine.mobs.HerobrineMob;
import com.euandrelucas.herobrine.mobs.MobManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;

/**
 * Listener para eventos do jogador relacionados ao Herobrine (Line-of-Sight, cama, etc).
 */
public class PlayerListener implements Listener {

    private final HerobrinePlugin plugin;
    private final MobManager mobManager;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;

    public PlayerListener(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.mobManager = plugin.getMobManager();
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (configManager.isWorldDisabled(world.getName())) return;

        HerobrineMob mob = mobManager.getActiveMob(world);
        if (mob != null && mob.isAlive()) {
            // Atualiza orientação do Herobrine para encarar o jogador
            mob.updateOrientationTowardsTarget();

            // Se o jogador olhar diretamente para ele enquanto no estado WATCHING, Herobrine desaparece
            if (mob.isLookedAtBy(player)) {
                mob.despawn();
            }
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (mobManager.isHerobrineActive(world)) {
            HerobrineMob mob = mobManager.getActiveMob(world);
            if (mob != null && mob.getTargetPlayer().getUniqueId().equals(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(messagesManager.getFormattedMessage("horror.sleep-prevented", new HashMap<>()));
            }
        }
    }
}
