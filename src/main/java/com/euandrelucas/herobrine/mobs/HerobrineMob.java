package com.euandrelucas.herobrine.mobs;

import com.euandrelucas.herobrine.ai.HerobrineStateMachine;
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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade wrapper do Herobrine no mundo.
 * Suporta clones ilusórios, orientação, pegadas de redstone e detecção de olhar.
 */
public class HerobrineMob {

    private final UUID entityId;
    private final World world;
    private Location currentLocation;
    private final Player targetPlayer;
    private final HerobrineStateMachine stateMachine;
    private Zombie entityHandle;
    private final List<Zombie> illusionClones = new ArrayList<>();
    private boolean isAlive;

    public HerobrineMob(World world, Location spawnLocation, Player targetPlayer) {
        this.entityId = UUID.randomUUID();
        this.world = world;
        this.currentLocation = spawnLocation.clone();
        this.targetPlayer = targetPlayer;
        this.stateMachine = new HerobrineStateMachine(targetPlayer);
        this.isAlive = false;
    }

    public boolean spawn() {
        if (world == null || currentLocation == null) return false;

        Zombie zombie = (Zombie) world.spawnEntity(currentLocation, EntityType.ZOMBIE);
        zombie.setCustomName("§cHerobrine");
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);
        zombie.setCanPickupItems(false);
        zombie.setPersistent(false);

        ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
            steveHead.setItemMeta(meta);
        }
        if (zombie.getEquipment() != null) {
            zombie.getEquipment().setHelmet(steveHead);
        }

        this.entityHandle = zombie;
        this.isAlive = true;
        updateOrientationTowardsTarget();
        return true;
    }

    /**
     * Ideia 1: Deixa uma trilha temporária de tochas de redstone por onde caminha.
     */
    public void leaveRedstoneFootprint() {
        if (!isAlive || entityHandle == null) return;
        Location loc = entityHandle.getLocation();
        if (loc.getBlock().getType() == Material.AIR) {
            loc.getBlock().setType(Material.REDSTONE_TORCH);
            Bukkit.getScheduler().runTaskLater(com.euandrelucas.herobrine.core.HerobrinePlugin.getInstance(), () -> {
                if (loc.getBlock().getType() == Material.REDSTONE_TORCH) {
                    loc.getBlock().setType(Material.AIR);
                    world.spawnParticle(Particle.SMOKE_NORMAL, loc, 5);
                }
            }, 160L); // Some após 8 segundos
        }
    }

    /**
     * Ideia 2: Invoca 3 clones de ilusão ao redor do jogador.
     */
    public void spawnIllusionClones() {
        if (!isAlive || targetPlayer == null) return;
        Location pLoc = targetPlayer.getLocation();

        for (int i = 0; i < 3; i++) {
            double angle = (2 * Math.PI / 3) * i;
            double x = pLoc.getX() + 5 * Math.cos(angle);
            double z = pLoc.getZ() + 5 * Math.sin(angle);
            Location cloneLoc = new Location(world, x, pLoc.getY(), z);

            Zombie clone = (Zombie) world.spawnEntity(cloneLoc, EntityType.ZOMBIE);
            clone.setCustomName("§cHerobrine");
            clone.setCustomNameVisible(true);
            clone.setBaby(false);
            clone.setGlowing(true);

            ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
                steveHead.setItemMeta(meta);
            }
            if (clone.getEquipment() != null) {
                clone.getEquipment().setHelmet(steveHead);
            }

            illusionClones.add(clone);
        }
    }

    public boolean isIllusionClone(Zombie zombie) {
        return illusionClones.contains(zombie);
    }

    public void removeIllusionClone(Zombie zombie) {
        if (illusionClones.remove(zombie)) {
            zombie.getWorld().spawnParticle(Particle.SMOKE_LARGE, zombie.getLocation(), 20);
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.5f);
            zombie.remove();
        }
    }

    public void updateOrientationTowardsTarget() {
        if (!isAlive || entityHandle == null || targetPlayer == null) return;

        Location targetLoc = targetPlayer.getLocation();
        Location mobLoc = entityHandle.getLocation();
        Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();

        if (direction.getX() != 0 || direction.getZ() != 0) {
            mobLoc.setDirection(direction);
            entityHandle.teleport(mobLoc);
        }

        // Deixa pegadas ocasionais de redstone
        if (Math.random() < 0.15) {
            leaveRedstoneFootprint();
        }
    }

    public boolean isLookedAtBy(Player player) {
        if (!isAlive || entityHandle == null || player == null) return false;

        Location eye = player.getEyeLocation();
        Vector toEntity = entityHandle.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        Vector playerDirection = eye.getDirection().normalize();

        double dot = playerDirection.dot(toEntity);
        return dot > 0.85;
    }

    public void despawn() {
        if (entityHandle != null && entityHandle.isValid()) {
            entityHandle.remove();
        }
        for (Zombie clone : illusionClones) {
            if (clone != null && clone.isValid()) {
                clone.remove();
            }
        }
        illusionClones.clear();
        this.isAlive = false;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public World getWorld() {
        return world;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public HerobrineStateMachine getStateMachine() {
        return stateMachine;
    }

    public Zombie getEntityHandle() {
        return entityHandle;
    }

    public boolean isAlive() {
        return isAlive && entityHandle != null && entityHandle.isValid();
    }
}
