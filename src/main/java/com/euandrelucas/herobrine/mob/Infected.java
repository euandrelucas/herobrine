package com.euandrelucas.herobrine.mob;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.item.Items;
import com.euandrelucas.herobrine.util.Util;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mobs infectados (animais/aldeões possuídos que perseguem o jogador) e monstros possuídos.
 * Água Benta cura os infectados. Ambos soltam Pó Amaldiçoado.
 */
public final class Infected implements Listener {

    private final HerobrinePlugin plugin;
    private final NamespacedKey infectedKey;
    private final NamespacedKey possessedKey;
    private final Map<UUID, Integer> cooldown = new HashMap<>();
    private final Map<UUID, Boolean> charging = new HashMap<>();
    private BukkitTask task;

    public Infected(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.infectedKey = new NamespacedKey(plugin, "infected");
        this.possessedKey = new NamespacedKey(plugin, "possessed");
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    public boolean isInfected(Entity e) {
        return e.getPersistentDataContainer().has(infectedKey, PersistentDataType.BYTE);
    }

    public boolean infect(LivingEntity e) {
        if (isInfected(e) || !(e instanceof Pig || e instanceof Cow || e instanceof Villager
                || e instanceof org.bukkit.entity.Sheep || e instanceof org.bukkit.entity.Chicken)) return false;
        e.getPersistentDataContainer().set(infectedKey, PersistentDataType.BYTE, (byte) 1);
        e.getWorld().spawnParticle(Particle.SOUL, e.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.05);
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.8f, 0.5f);
        return true;
    }

    public void cure(LivingEntity e) {
        if (!isInfected(e)) return;
        e.getPersistentDataContainer().remove(infectedKey);
        e.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, e.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.05);
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1.2f);
        if (e instanceof Mob m) m.getPathfinder().stopPathfinding();
    }

    @EventHandler
    public void onAdd(EntityAddToWorldEvent e) {
        // nada a registrar: o scan usa as entidades carregadas
    }

    private void tick() {
        for (var world : plugin.getServer().getWorlds()) {
            if (!plugin.worldState().active(world)) continue;
            for (LivingEntity e : world.getLivingEntities()) {
                if (!(e instanceof Mob mob) || !isInfected(e)) continue;
                tickInfected(mob);
            }
        }
        cooldown.replaceAll((k, v) -> Math.max(0, v - 5));
    }

    private void tickInfected(Mob mob) {
        Location loc = mob.getLocation();
        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, mob.getHeight() * 0.8, 0), 1, 0.15, 0.1, 0.15, 0.0);

        Player nearest = null;
        double best = 24 * 24;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.hasPermission("herobrine.bypass")) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest == null) return;
        double dist = Math.sqrt(best);
        UUID id = mob.getUniqueId();

        double speed;
        double damage;
        double knock;
        if (mob instanceof Cow) {
            // Vaca: encara em silêncio e só ataca quando você chega a 10 blocos.
            if (!charging.getOrDefault(id, false)) {
                mob.lookAt(nearest.getEyeLocation());
                mob.getPathfinder().stopPathfinding();
                if (dist > 10) return;
                charging.put(id, true);
                mob.getWorld().playSound(loc, Sound.ENTITY_COW_DEATH, 1f, 0.4f);
            }
            speed = 1.7;
            damage = 3.0;
            knock = 1.8;
        } else if (mob instanceof Villager) {
            speed = 2.0;
            damage = 3.0;
            knock = 0.6;
        } else if (mob instanceof Pig) {
            if (!Util.lineOfSight(nearest, mob.getEyeLocation())) return;
            speed = 1.6;
            damage = 3.0;
            knock = 0.7;
        } else {
            mob.lookAt(nearest.getEyeLocation());
            return;
        }

        mob.getPathfinder().moveTo(nearest.getLocation(), speed);
        if (dist < 1.8 && cooldown.getOrDefault(id, 0) == 0) {
            cooldown.put(id, 20);
            nearest.damage(damage, mob);
            Vector kb = nearest.getLocation().toVector().subtract(loc.toVector()).setY(0);
            if (kb.lengthSquared() > 0) nearest.setVelocity(kb.normalize().multiply(knock).setY(0.3));
            mob.getWorld().playSound(loc, Sound.ENTITY_GENERIC_HURT, 1f, 0.5f);
        }
    }

    // ------------------------------------------------------------ monstros possuídos

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        LivingEntity le = e.getEntity();
        if (!(le instanceof Zombie || le instanceof Skeleton || le instanceof Spider)) return;
        if (!plugin.worldState().active(le.getWorld())) return;
        double chance = plugin.getConfig().getDouble("possessed-monsters.chance", 0.05);
        if (Util.rng().nextDouble() >= chance) return;

        le.getPersistentDataContainer().set(possessedKey, PersistentDataType.BYTE, (byte) 1);
        var hp = le.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(hp.getBaseValue() * plugin.getConfig().getDouble("possessed-monsters.health-multiplier", 1.5));
            le.setHealth(hp.getValue());
        }
        var dmg = le.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * plugin.getConfig().getDouble("possessed-monsters.damage-multiplier", 1.3));
        le.customName(Util.color("§4Possuído"));
        le.setCustomNameVisible(false);
        le.setGlowing(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        boolean inf = isInfected(le);
        boolean pos = le.getPersistentDataContainer().has(possessedKey, PersistentDataType.BYTE);
        if (!inf && !pos) return;
        if (Util.rng().nextDouble() < (inf ? 0.5 : 0.3)) {
            e.getDrops().add(plugin.items().create(Items.Id.CURSED_DUST, 1 + Util.rng().nextInt(2)));
        }
        charging.remove(le.getUniqueId());
        cooldown.remove(le.getUniqueId());
    }
}
