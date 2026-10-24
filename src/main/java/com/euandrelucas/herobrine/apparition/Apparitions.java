package com.euandrelucas.herobrine.apparition;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gerencia as aparições ativas (no máximo uma por jogador). */
public final class Apparitions {

    private final HerobrinePlugin plugin;
    private final Map<UUID, Apparition> active = new HashMap<>();
    private BukkitTask task;

    public Apparitions(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (var e : new ArrayList<>(active.entrySet())) {
                boolean alive;
                try {
                    alive = e.getValue().tick();
                } catch (Exception ex) {
                    plugin.getLogger().warning("Erro na aparição: " + ex);
                    alive = false;
                }
                if (!alive) {
                    e.getValue().removeNow();
                    active.remove(e.getKey());
                }
            }
        }, 1L, 1L);
    }

    public boolean has(Player p) {
        return active.containsKey(p.getUniqueId());
    }

    public Apparition get(Player p) {
        return active.get(p.getUniqueId());
    }

    public Apparition find(Entity e) {
        if (!(e instanceof Mannequin)) return null;
        for (Apparition a : active.values()) {
            if (a.owns(e)) return a;
        }
        return null;
    }

    public java.util.Collection<Apparition> all() {
        return active.values();
    }

    /** @return false se o jogador já tem uma aparição ou o local é nulo. */
    public boolean spawn(Player p, Apparition.Mode mode, Location at) {
        return spawn(p, mode, at, null) != null;
    }

    /** Cria uma aparição e deixa o chamador configurá-la (ex.: lista de blocos do Builder). */
    public Apparition spawn(Player p, Apparition.Mode mode, Location at, java.util.function.Consumer<Apparition> configure) {
        if (has(p) || at == null) return null;
        Apparition a = new Apparition(plugin, p, mode, at);
        if (configure != null) configure.accept(a);
        active.put(p.getUniqueId(), a);
        plugin.debug(mode + " para " + p.getName());
        return a;
    }

    public void remove(Player p) {
        Apparition a = active.remove(p.getUniqueId());
        if (a != null) a.removeNow();
    }

    public void removeAll() {
        if (task != null) task.cancel();
        active.values().forEach(Apparition::removeNow);
        active.clear();
    }

    /** O susto final: grito, rosto a rosto, cegueira breve. Não destrói nada do jogador. */
    public static void jumpscare(HerobrinePlugin plugin, Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.6f, 0.5f);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 1f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false, false));
        p.showTitle(Title.title(Component.text(" "), Util.color("§4§k|§r §c§l...ELE ESTÁ AQUI §4§k|"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(400))));

        Location face = p.getLocation().add(p.getEyeLocation().getDirection().setY(0).normalize().multiply(1.3));
        Mannequin m = face.getWorld().spawn(face, Mannequin.class, e -> {
            e.setVisibleByDefault(false);
            e.setPersistent(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setGravity(false);
            e.setImmovable(true);
            e.setDescription(null);
        });
        p.showEntity(plugin, m);
        Location look = m.getLocation();
        look.setDirection(p.getEyeLocation().toVector().subtract(look.toVector().add(new Vector(0, 1.6, 0))));
        m.setRotation(look.getYaw(), look.getPitch());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (m.isValid()) {
                m.getWorld().spawnParticle(Particle.LARGE_SMOKE, m.getLocation().add(0, 1, 0), 30, 0.3, 0.8, 0.3, 0.02);
                m.remove();
            }
        }, 24L);
    }
}
