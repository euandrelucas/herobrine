package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia efeitos de terror psicológico, pesadelos, jumpscares e simulações client-safe.
 */
public class PsychologicalHorrorManager implements Listener {

    private final HerobrinePlugin plugin;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;

    public PsychologicalHorrorManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    /**
     * 6.1: Executa jumpscare no jogador.
     */
    public void triggerJumpscare(Player player) {
        if (player == null || !player.isOnline()) return;

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);

        if (plugin.getConfig().getBoolean("horror.jumpscare.simulate-error-screen", false)) {
            // Tela preta cheia com aviso de erro simulado (client-safe)
            player.sendTitle("§0§l[FATAL ERROR]", "§cConnection Lost - Entity 303/Herobrine", 0, 40, 10);
        } else {
            player.sendTitle("§c§lHEROBRINE", "§8Ele encontrou você.", 5, 30, 5);
        }
    }

    /**
     * 6.3: Envia mensagem assustadora no chat com opção de inverter string.
     */
    public void sendCrypticChatMessage(Player player, String message) {
        if (player == null || !player.isOnline()) return;

        String formatted = message;
        if (plugin.getConfig().getBoolean("horror.backwards-messages", true)) {
            formatted = new StringBuilder(message).reverse().toString();
        }
        player.sendMessage("§c" + formatted);
    }

    /**
     * 6.4: Transmite mensagem falsa de entrada ("Eerie Entrance").
     */
    public void sendFakeJoinMessage(Player player) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", "Herobrine");
        String msg = messagesManager.getFormattedMessage("horror.eerie-entrance", ph);
        player.sendMessage(msg);
    }

    /**
     * 6.7: Aplica efeito de pesadelo ao acordar.
     */
    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!plugin.getConfig().getBoolean("horror.nightmares.enabled", true)) return;

        Player player = event.getPlayer();
        double chance = plugin.getConfig().getDouble("horror.nightmares.chance", 0.15);

        if (Math.random() < chance) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 140, 0));
            player.sendMessage(messagesManager.getFormattedMessage("horror.nightmare", new HashMap<>()));
        }
    }
}
