package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.ai.HerobrineState;
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
import java.util.Random;

/**
 * Listener para eventos do jogador (Line-of-Sight inteligente, cama, etc).
 * O Herobrine não desaparece obrigatoriamente 100% das vezes ao ser visto, comportando-se como um jogador real.
 */
public class PlayerListener implements Listener {

    private final HerobrinePlugin plugin;
    private final MobManager mobManager;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private final Random random = new Random();

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
            mob.updateOrientationTowardsTarget();

            // Comportamento Humanoide Inteligente:
            // No estado WATCHING, tem 60% de chance de sumir ao ser olhado diretamente.
            // Nos demais estados (CREEPING, LURKER, HOSTILE), ele sustenta o olhar como um jogador real!
            if (mob.isLookedAtBy(player)) {
                HerobrineState state = mob.getStateMachine().getCurrentState();
                if (state == HerobrineState.WATCHING) {
                    if (random.nextDouble() < 0.60) {
                        mob.despawn();
                    } else {
                        // Permanece encarando e aumenta a raiva
                        mob.getStateMachine().addAnger(5);
                    }
                }
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
