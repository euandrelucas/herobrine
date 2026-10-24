package com.euandrelucas.herobrine.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public final class Util {

    private Util() {}

    public static Component color(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    public static boolean free(Block b) {
        return b.isPassable() && !b.isLiquid();
    }

    /** Procura um chão onde caiba uma pessoa em volta de aroundY. Não carrega chunks. */
    public static Location findFloor(World w, int x, int z, int aroundY) {
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
        int top = Math.min(w.getMaxHeight() - 3, aroundY + 5);
        int bottom = Math.max(w.getMinHeight() + 1, aroundY - 8);
        for (int y = top; y >= bottom; y--) {
            Block floor = w.getBlockAt(x, y, z);
            if (floor.isSolid() && !floor.isLiquid()
                    && free(w.getBlockAt(x, y + 1, z)) && free(w.getBlockAt(x, y + 2, z))) {
                return new Location(w, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    public static Location pointAround(Location center, double yawDegrees, double dist) {
        double rad = Math.toRadians(yawDegrees);
        // Convenção do Bukkit: yaw 0 = +Z, yaw 90 = -X
        return center.clone().add(-Math.sin(rad) * dist, 0, Math.cos(rad) * dist);
    }

    /** Ângulo (graus, 0-180) entre onde o jogador olha e o alvo. */
    public static double viewAngle(Player p, Location target) {
        Vector look = p.getEyeLocation().getDirection();
        Vector to = target.toVector().subtract(p.getEyeLocation().toVector()).normalize();
        double dot = Math.max(-1, Math.min(1, look.dot(to)));
        return Math.toDegrees(Math.acos(dot));
    }

    public static boolean lineOfSight(Player p, Location target) {
        Location eye = p.getEyeLocation();
        Vector dir = target.toVector().subtract(eye.toVector());
        double len = dir.length();
        if (len < 0.5) return true;
        return p.getWorld().rayTraceBlocks(eye, dir.normalize(), len, FluidCollisionMode.NEVER, true) == null;
    }

    public static boolean isLookingAt(Player p, Location target, double maxAngle) {
        return viewAngle(p, target) <= maxAngle && lineOfSight(p, target);
    }

    public static boolean underground(Player p) {
        Location l = p.getLocation();
        return l.getWorld().getHighestBlockYAt(l) - l.getBlockY() > 8;
    }

    public static boolean indoors(Player p) {
        Location l = p.getLocation().add(0, 2, 0);
        for (int i = 0; i < 6; i++) {
            if (l.getBlock().isSolid()) return !underground(p);
            l.add(0, 1, 0);
        }
        return false;
    }

    public static boolean night(World w) {
        long t = w.getTime();
        return t >= 13000 && t <= 23000;
    }

    public static ThreadLocalRandom rng() {
        return ThreadLocalRandom.current();
    }
}
