package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Gerencia a Posse de Jogador pelo Herobrine com controle 100% travado da câmera,
 * imunidade a danos, teleporte e devolução à posição exata.
 */
public class PlayerPossessionManager implements Listener {

    private final HerobrinePlugin plugin;
    private final Map<UUID, BukkitTask> activePossessions = new HashMap<>();
    private final Map<UUID, Location> savedOriginalLocations = new HashMap<>();
    private final Map<UUID, ItemStack> previousHelmets = new HashMap<>();
    private final Map<UUID, Location> forcedTargetLocations = new HashMap<>();
    private final Random random = new Random();

    public PlayerPossessionManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void possessPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (activePossessions.containsKey(uuid)) return;

        // 1. Salva posição exata original
        Location originalLoc = player.getLocation().clone();
        savedOriginalLocations.put(uuid, originalLoc);

        ItemStack currentHelmet = player.getInventory().getHelmet();
        if (currentHelmet != null) {
            previousHelmets.put(uuid, currentHelmet.clone());
        }

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

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1200, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
        player.sendTitle("§c§lPOSSUÍDO", "§8Herobrine tomou controle 100% do seu corpo!", 10, 80, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

        // 2. Tarefa de Controle Total (Movimentação, Câmera Travada e Teleportes)
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                int seconds = ticks / 20;

                if (!player.isOnline() || seconds >= 60) {
                    releasePlayer(player);
                    cancel();
                    return;
                }

                // 2.1 Rotação e olhar forçados (Câmera 100% controlada)
                Location pLoc = player.getLocation();
                float yaw = pLoc.getYaw() + (random.nextFloat() * 40 - 20);
                float pitch = random.nextFloat() * 20 - 10;
                pLoc.setYaw(yaw);
                pLoc.setPitch(pitch);

                forcedTargetLocations.put(uuid, pLoc.clone());
                player.teleport(pLoc); // Força a nova orientação da câmera

                Vector dir = pLoc.getDirection().normalize().multiply(0.35);
                player.setVelocity(dir.setY(0.05));

                player.swingMainHand();

                // 2.2 Teleporte aleatório curto durante a posse
                if (ticks % 100 == 0) { // A cada 5 segundos
                    double tpX = pLoc.getX() + (random.nextDouble() * 12 - 6);
                    double tpZ = pLoc.getZ() + (random.nextDouble() * 12 - 6);
                    Location tpLoc = new Location(pLoc.getWorld(), tpX, pLoc.getY(), tpZ, yaw, pitch);
                    player.getWorld().spawnParticle(Particle.FLAME, pLoc, 25, 0.3, 0.5, 0.3, 0.1);
                    player.teleport(tpLoc);
                    player.playSound(tpLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                }

                Block targetBlock = player.getTargetBlockExact(3);
                if (targetBlock != null && targetBlock.getType() != Material.BEDROCK && targetBlock.getType() != Material.AIR) {
                    if (random.nextDouble() < 0.20) {
                        targetBlock.setType(Material.NETHERRACK);
                    }
                }

                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);

                if (ticks % 300 == 0) {
                    String[] phrases = {
                        "EU SOU O HEROBRINE.",
                        "ESTE CORPO AGORA E MEU.",
                        "NAO HA ESCAPATORIA.",
                        "VOCES SERAO OS PROXIMOS."
                    };
                    String phrase = phrases[random.nextInt(phrases.length)];
                    Bukkit.broadcastMessage("§8[§cHerobrine§8] §c" + player.getName() + " diz: " + phrase);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);

        activePossessions.put(uuid, task);
    }

    /**
     * 100% Imunidade a qualquer dano enquanto possuído pelo Herobrine.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isPlayerPossessed(player)) {
                event.setCancelled(true);
            }
        }
    }

    public void releasePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        BukkitTask task = activePossessions.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        forcedTargetLocations.remove(uuid);

        if (player.isOnline()) {
            player.setGlowing(false);
            player.setCustomName(player.getName());
            player.setCustomNameVisible(false);

            if (previousHelmets.containsKey(uuid)) {
                player.getInventory().setHelmet(previousHelmets.remove(uuid));
            } else {
                player.getInventory().setHelmet(null);
            }

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SPEED);

            Location originalLoc = savedOriginalLocations.remove(uuid);
            if (originalLoc != null) {
                player.teleport(originalLoc);
            }

            player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 30, 0.5, 1.0, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage("§aO controle do Herobrine se dissipou... Você foi devolvido à sua posição original.");
        }
    }

    public boolean isPlayerPossessed(Player player) {
        return player != null && activePossessions.containsKey(player.getUniqueId());
    }
}
