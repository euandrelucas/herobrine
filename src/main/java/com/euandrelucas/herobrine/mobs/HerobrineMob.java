package com.euandrelucas.herobrine.mobs;

import com.euandrelucas.herobrine.ai.HerobrineStateMachine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Entidade wrapper do Herobrine no mundo.
 * Nunca nada, não usa veículos nem portais (teleporta ou desaparece).
 */
public class HerobrineMob {

    private final UUID entityId;
    private final World world;
    private Location currentLocation;
    private final Player targetPlayer;
    private final HerobrineStateMachine stateMachine;
    private Zombie entityHandle;
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

        // Spawna uma entidade Zombie reskinnada de forma totalmente compatível com a API Bukkit
        Zombie zombie = (Zombie) world.spawnEntity(currentLocation, EntityType.ZOMBIE);
        zombie.setCustomName("§cHerobrine");
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);
        zombie.setCanPickupItems(false);
        zombie.setPersistent(false);

        // Equipamento visual (cabeça de Steve)
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

    public void updateOrientationTowardsTarget() {
        if (!isAlive || entityHandle == null || targetPlayer == null) return;

        Location targetLoc = targetPlayer.getLocation();
        Location mobLoc = entityHandle.getLocation();
        Vector direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize();

        if (direction.getX() != 0 || direction.getZ() != 0) {
            mobLoc.setDirection(direction);
            entityHandle.teleport(mobLoc);
        }
    }

    /**
     * Verifica se o jogador está olhando diretamente para a posição do Herobrine.
     */
    public boolean isLookedAtBy(Player player) {
        if (!isAlive || entityHandle == null || player == null) return false;

        Location eye = player.getEyeLocation();
        Vector toEntity = entityHandle.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        Vector playerDirection = eye.getDirection().normalize();

        // Produto escalar para verificar ângulo de visão (cos(theta) > 0.85 = ~30 graus)
        double dot = playerDirection.dot(toEntity);
        return dot > 0.85;
    }

    public void despawn() {
        if (entityHandle != null && entityHandle.isValid()) {
            entityHandle.remove();
        }
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
