package com.euandrelucas.herobrine.director;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.util.Util;
import com.euandrelucas.herobrine.world.Placement;
import com.euandrelucas.herobrine.world.Structures;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.List;
import java.util.function.Function;

/** Variantes do Herobrine e eventos que constroem estruturas. */
public final class WorldEvents {

    private WorldEvents() {}

    // ------------------------------------------------------------------ variantes

    private static boolean variant(HerobrinePlugin plugin, Player p, Apparition.Mode mode, double min, double max, double angle) {
        if (plugin.apparitions().has(p)) return false;
        return plugin.apparitions().spawn(p, mode, Events.viewSpot(p, min, max, angle));
    }

    public static boolean weeping(HerobrinePlugin plugin, Player p) {
        return variant(plugin, p, Apparition.Mode.WEEPING, 20, 32, 70);
    }

    public static boolean sky(HerobrinePlugin plugin, Player p) {
        return variant(plugin, p, Apparition.Mode.SKY, 18, 30, 55);
    }

    public static boolean knockback(HerobrinePlugin plugin, Player p) {
        return variant(plugin, p, Apparition.Mode.KNOCKBACK, 20, 30, 40);
    }

    public static boolean mage(HerobrinePlugin plugin, Player p) {
        return variant(plugin, p, Apparition.Mode.MAGE, 10, 16, 60);
    }

    public static boolean circle(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        return plugin.apparitions().spawn(p, Apparition.Mode.CIRCLE, p.getLocation());
    }

    /** Aparece de repente na frente ou atrás do jogador. */
    public static boolean flash(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        boolean behind = Util.rng().nextBoolean();
        for (int i = 0; i < 10; i++) {
            Location c = Util.pointAround(p.getLocation(), p.getLocation().getYaw() + (behind ? 180 : 0) + Util.rng().nextDouble(-20, 20),
                    Util.rng().nextDouble(3.5, 6));
            Location f = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getLocation().getBlockY());
            if (f != null) return plugin.apparitions().spawn(p, Apparition.Mode.FLASH, f);
        }
        return false;
    }

    /** Possui um animal ou aldeão próximo. */
    public static boolean infect(HerobrinePlugin plugin, Player p) {
        for (Entity e : p.getNearbyEntities(30, 12, 30)) {
            if ((e instanceof Animals || e instanceof Villager) && e instanceof LivingEntity le
                    && Util.viewAngle(p, e.getLocation()) > 50 && plugin.infected().infect(le)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ estruturas

    /**
     * Procura um terreno adequado e coloca a estrutura. Se o jogador estiver vendo o local, só constrói
     * (com o Herobrine visível) quando o modo Construtor está liberado; senão constrói quando ele não olha.
     */
    private static boolean build(HerobrinePlugin plugin, Player p, double min, double max, int w, int d,
                                 Function<Block, List<Placement>> fn, boolean allowBuilder) {
        Location pl = p.getLocation();
        boolean builderOk = allowBuilder && !plugin.apparitions().has(p)
                && plugin.getConfig().getBoolean("builder.enabled", true)
                && plugin.presence().get(p) >= plugin.getConfig().getDouble("builder.min-presence", 45)
                && Util.rng().nextDouble() < 0.5;
        for (int i = 0; i < 40; i++) {
            Location c = Util.pointAround(pl, Util.rng().nextDouble(360), Util.rng().nextDouble(min, max));
            Location f = Util.findFloor(pl.getWorld(), c.getBlockX(), c.getBlockZ(), pl.getBlockY());
            if (f == null) continue;
            Block corner = f.getBlock().getRelative(-w / 2, 0, -d / 2);
            List<Placement> list = fn.apply(corner);
            if (list == null || list.isEmpty()) continue;

            Location eyeLevel = f.clone().add(0, 2, 0);
            boolean seen = Util.viewAngle(p, eyeLevel) < 70 && Util.lineOfSight(p, eyeLevel);
            if (seen) {
                if (!builderOk) continue;
                Location stand = Util.findFloor(f.getWorld(), corner.getX() - 2, corner.getZ(), f.getBlockY());
                if (stand == null) continue;
                Apparition a = plugin.apparitions().spawn(p, Apparition.Mode.BUILDER, stand, ap -> ap.setPlacements(list));
                if (a != null) return true;
                continue;
            }
            plugin.blockQueue().addAll(list);
            return true;
        }
        return false;
    }

    public static boolean mossyPyramid(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 22, 42, 7, 7, o -> Structures.pyramid(o, Material.MOSSY_COBBLESTONE), true);
    }

    public static boolean sandPyramid(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 24, 44, 7, 7, o -> Structures.pyramid(o, Material.SANDSTONE), true);
    }

    public static boolean bedrockCross(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 16, 36, 3, 1, Structures::bedrockCross, true);
    }

    public static boolean floatingBedrock(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 14, 30, 3, 1, Structures::floatingBedrock, false);
    }

    public static boolean woodenCross(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 16, 36, 3, 1, Structures::woodenCross, true);
    }

    public static boolean monument(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 14, 34, 3, 3, Structures::monument, true);
    }

    public static boolean statue(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 18, 40, 5, 1, Structures::statue, true);
    }

    public static boolean letters(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 20, 36, 54, 5, Structures::letters, false);
    }

    public static boolean ruinedShrine(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 16, 36, 3, 3, o -> {
            Biome b = o.getBiome();
            String n = b.getKey().getKey();
            return n.contains("forest") || n.contains("taiga") ? Structures.ruinedShrine(o) : null;
        }, false);
    }

    public static boolean trappedHouse(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 24, 48, 7, 7, o -> Structures.trappedHouse(plugin, o), true);
    }

    public static boolean survivorBase(HerobrinePlugin plugin, Player p) {
        return build(plugin, p, 30, 60, 11, 11, o -> Structures.survivorBase(plugin, o), false);
    }

    public static boolean fakeDiamond(HerobrinePlugin plugin, Player p) {
        for (int i = 0; i < 20; i++) {
            Location c = Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), Util.rng().nextDouble(6, 14));
            Location f = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getLocation().getBlockY());
            if (f != null && f.getBlock().getType().isAir()) {
                plugin.blockQueue().addAll(List.of(Placement.fake(f.getBlock(), Material.DIAMOND_BLOCK)));
                return true;
            }
        }
        return false;
    }
}
