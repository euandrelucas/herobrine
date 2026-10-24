package com.euandrelucas.herobrine.apparition;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.util.Util;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Uma aparição do Herobrine, visível somente para o jogador alvo.
 * O Mannequin se move por teleporte; a IA é toda feita aqui.
 */
public final class Apparition {

    public enum Mode { STARE, WINDOW, STALK, CHASE, HOSTILE }

    private final HerobrinePlugin plugin;
    private final Player owner;
    private final Mode mode;
    private Mannequin body;
    private int age;
    private int lookTicks;
    private boolean everSeen;
    // STALK
    private int sightings;
    private int respawnAt = -1;
    private int lastMove;
    // CHASE / HOSTILE
    private int stuck;
    private int attackCooldown;
    private double health;

    public Apparition(HerobrinePlugin plugin, Player owner, Mode mode, Location at) {
        this.plugin = plugin;
        this.owner = owner;
        this.mode = mode;
        this.health = plugin.getConfig().getDouble("hostile.health", 200);
        this.body = spawnBody(at);
    }

    public Mode mode() { return mode; }
    public Player owner() { return owner; }
    public Mannequin body() { return body; }

    /** Cria o corpo, oculto para todos exceto o dono. */
    private Mannequin spawnBody(Location at) {
        Mannequin m = at.getWorld().spawn(at, Mannequin.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setGravity(false);
            e.setCollidable(false);
            e.setImmovable(true);
            e.setDescription(null);
            e.setProfile(buildProfile());
        });
        owner.showEntity(plugin, m);
        face(m);
        return m;
    }

    private ResolvableProfile buildProfile() {
        String value = plugin.getConfig().getString("skin.texture-value", "");
        if (value != null && !value.isBlank()) {
            PlayerProfile pp = Bukkit.createProfile(UUID.randomUUID(), "Herobrine");
            String sig = plugin.getConfig().getString("skin.texture-signature", "");
            pp.setProperty(sig == null || sig.isBlank()
                    ? new ProfileProperty("textures", value)
                    : new ProfileProperty("textures", value, sig));
            return ResolvableProfile.resolvableProfile(pp);
        }
        return ResolvableProfile.resolvableProfile()
                .name(plugin.getConfig().getString("skin.name", "Herobrine"))
                .build();
    }

    private void face(Mannequin m) {
        Location l = m.getLocation();
        Vector to = owner.getEyeLocation().toVector().subtract(l.toVector().add(new Vector(0, 1.6, 0)));
        if (to.lengthSquared() < 0.01) return;
        l.setDirection(to);
        m.setRotation(l.getYaw(), l.getPitch());
    }

    private Location head() {
        return body.getLocation().add(0, 1.6, 0);
    }

    /** @return false quando a aparição terminou. */
    public boolean tick() {
        age++;
        if (!owner.isOnline() || owner.isDead() || owner.getGameMode() == GameMode.SPECTATOR) return false;
        if (body != null && (!body.isValid() || body.getWorld() != owner.getWorld())) return false;

        return switch (mode) {
            case STARE -> tickStare(25, 9, 700);
            case WINDOW -> tickStare(70, 3, 500);
            case STALK -> tickStalk();
            case CHASE -> tickChase();
            case HOSTILE -> tickHostile();
        };
    }

    private boolean updateLooking() {
        boolean looked = Util.isLookingAt(owner, head(), 14);
        lookTicks = looked ? lookTicks + 1 : Math.max(0, lookTicks - 1);
        if (looked && !everSeen) {
            everSeen = true;
            plugin.presence().add(owner, plugin.getConfig().getDouble("presence.seen-bonus", 1.0));
        }
        return looked;
    }

    private boolean tickStare(int vanishAfterLook, double minDist, int maxAge) {
        face(body);
        updateLooking();
        double dist = body.getLocation().distance(owner.getLocation());
        if (lookTicks > vanishAfterLook || dist < minDist || age > maxAge) {
            vanish(false);
            return false;
        }
        return true;
    }

    private boolean tickStalk() {
        if (age > 2400 || sightings >= 4) {
            vanish(false);
            return false;
        }
        if (body == null) {
            if (age >= respawnAt) {
                Location l = hiddenSpot(12, 22);
                if (l != null) body = spawnBody(l);
                else respawnAt = age + 40;
            }
            return true;
        }
        face(body);
        updateLooking();
        if (lookTicks > 22) {
            sightings++;
            vanish(false);
            body = null;
            lookTicks = 0;
            respawnAt = age + 80 + Util.rng().nextInt(160);
            return true;
        }
        // Se o jogador não olha há um tempo, muda de lugar para ficar mais perto.
        if (age - lastMove > 100 && lookTicks == 0 && Util.viewAngle(owner, head()) > 70) {
            Location l = hiddenSpot(9, 16);
            if (l != null) {
                body.teleport(l);
                lastMove = age;
            }
        }
        return true;
    }

    private boolean tickChase() {
        face(body);
        if (age < 50) {
            updateLooking();
            return true;
        }
        if (age > 700) {
            vanish(true);
            return false;
        }
        double dist = body.getLocation().distance(owner.getLocation());
        if (dist < 2.3) {
            Apparitions.jumpscare(plugin, owner);
            vanish(false);
            return false;
        }
        if (!stepTowards(0.55)) {
            if (++stuck > 15) {
                stuck = 0;
                Location l = Util.pointAround(owner.getLocation(),
                        owner.getLocation().getYaw() + 180 + Util.rng().nextInt(-60, 60), 7);
                Location floor = Util.findFloor(l.getWorld(), l.getBlockX(), l.getBlockZ(), owner.getLocation().getBlockY());
                if (floor != null) body.teleport(floor);
            }
        } else {
            stuck = 0;
        }
        return true;
    }

    private boolean tickHostile() {
        face(body);
        var cfg = plugin.getConfig();
        double dist = body.getLocation().distance(owner.getLocation());
        if (age > cfg.getInt("hostile.duration-seconds", 45) * 20
                || health <= cfg.getDouble("hostile.health", 200) * 0.15) {
            owner.sendMessage(Util.color("§c§o...eu volto."));
            vanish(true);
            return false;
        }
        attackCooldown = Math.max(0, attackCooldown - 1);
        if (dist < 2.4) {
            if (attackCooldown == 0) {
                attackCooldown = 20;
                owner.damage(cfg.getDouble("hostile.damage", 5.0), body);
                Vector kb = owner.getLocation().toVector().subtract(body.getLocation().toVector()).setY(0);
                if (kb.lengthSquared() > 0) owner.setVelocity(kb.normalize().multiply(0.5).setY(0.25));
                body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.6f);
            }
        } else if (!stepTowards(0.32) && ++stuck > 20) {
            stuck = 0;
            Location floor = Util.findFloor(owner.getWorld(),
                    owner.getLocation().getBlockX() + Util.rng().nextInt(-4, 5),
                    owner.getLocation().getBlockZ() + Util.rng().nextInt(-4, 5), owner.getLocation().getBlockY());
            if (floor != null) body.teleport(floor);
        }
        return true;
    }

    /** O dono bateu no corpo. */
    public void onHit(double damage) {
        if (mode == Mode.HOSTILE) {
            health -= damage;
            body.getWorld().spawnParticle(Particle.CRIT, body.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
            body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
        } else {
            // Qualquer outro modo: ele some ao ser tocado.
            age = Integer.MAX_VALUE / 2;
        }
    }

    /** Anda uma fração de bloco na direção do jogador, grudado no chão. */
    private boolean stepTowards(double speed) {
        Location cur = body.getLocation();
        Vector dir = owner.getLocation().toVector().subtract(cur.toVector()).setY(0);
        if (dir.lengthSquared() < 0.01) return true;
        dir.normalize().multiply(speed);
        Location next = cur.clone().add(dir);
        Location floor = Util.findFloor(next.getWorld(), next.getBlockX(), next.getBlockZ(), cur.getBlockY());
        if (floor == null || Math.abs(floor.getY() - cur.getY()) > 1.01) return false;
        next.setY(floor.getY());
        body.teleport(next);
        return true;
    }

    /** Ponto fora do campo de visão do jogador, de preferência atrás dele. */
    private Location hiddenSpot(double min, double max) {
        Location p = owner.getLocation();
        for (int i = 0; i < 25; i++) {
            double yaw = p.getYaw() + 180 + Util.rng().nextDouble(-90, 90);
            Location c = Util.pointAround(p, yaw, Util.rng().nextDouble(min, max));
            Location floor = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getBlockY());
            if (floor != null && Util.viewAngle(owner, floor.clone().add(0, 1.6, 0)) > 75) return floor;
        }
        return null;
    }

    public void vanish(boolean smoke) {
        if (body != null && body.isValid()) {
            if (smoke) {
                body.getWorld().spawnParticle(Particle.LARGE_SMOKE, body.getLocation().add(0, 1, 0), 25, 0.3, 0.7, 0.3, 0.02);
            }
            body.remove();
        }
    }

    public void removeNow() {
        vanish(false);
    }
}
