package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o evento de Sequestro do Herobrine.
 * O jogador é transportado para uma sala escura de bedrock com uma janela de vidro,
 * através da qual vários Herobrines ficam parados no escuro encarando o jogador.
 */
public class KidnapManager {

    private final HerobrinePlugin plugin;
    private final Map<UUID, Location> savedLocations = new HashMap<>();
    private final Map<UUID, List<Zombie>> spectatorHerobrines = new HashMap<>();

    public KidnapManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void kidnapPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (savedLocations.containsKey(uuid)) return;

        Location originalLoc = player.getLocation().clone();
        savedLocations.put(uuid, originalLoc);

        World world = player.getWorld();
        Location chamberCenter = originalLoc.clone().add(0, 80, 0);
        if (chamberCenter.getY() > 250) {
            chamberCenter.setY(200);
        }

        buildChamberWithWindow(chamberCenter);

        Location insideChamber = chamberCenter.clone().add(1.5, 1, 1.5);
        player.teleport(insideChamber);

        // Spawna 3 Herobrines observadores atrás da janela de vidro
        List<Zombie> herobrines = spawnSpectatorHerobrines(chamberCenter, player);
        spectatorHerobrines.put(uuid, herobrines);

        // Efeitos de terror
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0));
        player.playSound(insideChamber, Sound.AMBIENT_CAVE, 2.0f, 0.5f);
        player.playSound(insideChamber, Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
        player.sendTitle("§c§lVOCÊ NÃO PODE ESCAPAR", "§8Vários Herobrines observam você através do vidro...", 10, 80, 10);

        // Retorna o jogador em segurança após 8 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(savedLocations.remove(uuid));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage("§c...Você foi devolvido ao mundo real.");
            }

            // Remove Herobrines espectadores e a sala
            List<Zombie> spects = spectatorHerobrines.remove(uuid);
            if (spects != null) {
                for (Zombie z : spects) {
                    if (z != null && z.isValid()) {
                        z.getWorld().spawnParticle(Particle.SMOKE_LARGE, z.getLocation(), 15);
                        z.remove();
                    }
                }
            }
            removeChamber(chamberCenter);
        }, 160L);
    }

    private void buildChamberWithWindow(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        // Constrói sala 5x4x5 de Bedrock
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    Location blockLoc = new Location(world, cX + x, cY + y, cZ + z);
                    boolean isInsideChamber = (x >= 1 && x <= 3) && (y >= 1 && y <= 2) && (z >= 1 && z <= 3);

                    if (isInsideChamber) {
                        blockLoc.getBlock().setType(Material.AIR);
                    } else {
                        blockLoc.getBlock().setType(Material.BEDROCK);
                    }
                }
            }
        }

        // Janela de Vidro Escuro na parede leste (x = 3)
        world.getBlockAt(cX + 3, cY + 1, cZ + 2).setType(Material.GLASS);
        world.getBlockAt(cX + 3, cY + 2, cZ + 2).setType(Material.GLASS);

        // Corredor externo de observação atrás do vidro (x = 4)
        world.getBlockAt(cX + 4, cY + 1, cZ + 1).setType(Material.AIR);
        world.getBlockAt(cX + 4, cY + 1, cZ + 2).setType(Material.AIR);
        world.getBlockAt(cX + 4, cY + 1, cZ + 3).setType(Material.AIR);
        world.getBlockAt(cX + 4, cY + 2, cZ + 2).setType(Material.AIR);

        // Tocha de redstone dentro da câmara do jogador
        world.getBlockAt(cX + 1, cY + 2, cZ + 1).setType(Material.REDSTONE_TORCH);
    }

    private List<Zombie> spawnSpectatorHerobrines(Location center, Player target) {
        World world = center.getWorld();
        List<Zombie> list = new ArrayList<>();
        if (world == null) return list;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int i = 1; i <= 3; i++) {
            Location loc = new Location(world, cX + 4.5, cY + 1, cZ + i + 0.5);
            Vector dir = target.getEyeLocation().toVector().subtract(loc.toVector()).normalize();
            loc.setDirection(dir);

            Zombie mob = (Zombie) world.spawnEntity(loc, EntityType.ZOMBIE);
            mob.setCustomName("§cHerobrine");
            mob.setCustomNameVisible(true);
            mob.setBaby(false);
            mob.setAI(false);
            mob.setGlowing(true);

            ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
                steveHead.setItemMeta(meta);
            }
            if (mob.getEquipment() != null) {
                mob.getEquipment().setHelmet(steveHead);
            }
            list.add(mob);
        }
        return list;
    }

    private void removeChamber(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    world.getBlockAt(cX + x, cY + y, cZ + z).setType(Material.AIR);
                }
            }
        }
    }
}
