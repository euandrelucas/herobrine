package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia a Posse de Jogador pelo Herobrine por 1 minuto.
 * O Herobrine assume temporariamente a entidade do jogador, aplicando brilho, rastro de partículas e frases macabras.
 */
public class PlayerPossessionManager {

    private final HerobrinePlugin plugin;
    private final Map<UUID, BukkitTask> activePossessions = new HashMap<>();
    private final Map<UUID, ItemStack> previousHelmets = new HashMap<>();

    public PlayerPossessionManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia a posse de 1 minuto sobre um jogador.
     */
    public void possessPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (activePossessions.containsKey(uuid)) return; // Já está possuído

        // Salva capacete anterior
        ItemStack currentHelmet = player.getInventory().getHelmet();
        if (currentHelmet != null) {
            previousHelmets.put(uuid, currentHelmet.clone());
        }

        // Equipamento visual e efeitos do Herobrine
        player.setGlowing(true);
        player.setCustomName("§c§l[POSSUÍDO] Herobrine");
        player.setCustomNameVisible(true);

        ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
            steveHead.setItemMeta(meta);
        }
        player.getInventory().setHelmet(steveHead);

        // Aplica efeitos de controle
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1200, 0)); // 60s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 1));

        player.sendTitle("§c§lPOSSUÍDO", "§8Herobrine tomou controle do seu corpo por 1 minuto!", 10, 80, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

        // Tarefa de 1 minuto (60 segundos) com partículas e frases no chat
        BukkitTask task = new BukkitRunnable() {
            int seconds = 0;

            @Override
            public void run() {
                seconds++;
                if (!player.isOnline() || seconds >= 60) {
                    releasePlayer(player);
                    cancel();
                    return;
                }

                // Partículas macabras de fumaça e fogo ao redor do jogador possuído
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);

                // Mensagens automáticas no chat a cada 15 segundos
                if (seconds % 15 == 0) {
                    player.chat("§c§lEU SOU O HEROBRINE.");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        activePossessions.put(uuid, task);
    }

    /**
     * Libera o jogador do controle do Herobrine.
     */
    public void releasePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        BukkitTask task = activePossessions.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        if (player.isOnline()) {
            player.setGlowing(false);
            player.setCustomName(player.getName());
            player.setCustomNameVisible(false);

            // Restaura capacete original
            if (previousHelmets.containsKey(uuid)) {
                player.getInventory().setHelmet(previousHelmets.remove(uuid));
            } else {
                player.getInventory().setHelmet(null);
            }

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);

            player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage("§aO controle do Herobrine se dissipou... Você recuperou o controle do seu corpo.");
        }
    }

    public boolean isPlayerPossessed(Player player) {
        return player != null && activePossessions.containsKey(player.getUniqueId());
    }
}
