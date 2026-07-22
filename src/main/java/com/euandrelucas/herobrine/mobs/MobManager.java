package com.euandrelucas.herobrine.mobs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia as instâncias de Herobrine ativas por mundo no servidor.
 * Garante a regra de no máximo UMA instância por mundo.
 */
public class MobManager {

    private final Map<String, HerobrineMob> activeMobs = new HashMap<>();

    public boolean spawnHerobrine(World world, Location location, Player target) {
        if (world == null || location == null || target == null) return false;

        String worldName = world.getName();
        if (activeMobs.containsKey(worldName)) {
            HerobrineMob existing = activeMobs.get(worldName);
            if (existing.isAlive()) {
                return false; // Já existe uma instância viva neste mundo
            } else {
                activeMobs.remove(worldName);
            }
        }

        HerobrineMob mob = new HerobrineMob(world, location, target);
        if (mob.spawn()) {
            activeMobs.put(worldName, mob);
            return true;
        }
        return false;
    }

    public boolean banishHerobrine(World world) {
        if (world == null) return false;
        String worldName = world.getName();

        HerobrineMob mob = activeMobs.remove(worldName);
        if (mob != null) {
            mob.despawn();
            return true;
        }
        return false;
    }

    public void banishAll() {
        for (HerobrineMob mob : activeMobs.values()) {
            if (mob != null) {
                mob.despawn();
            }
        }
        activeMobs.clear();
    }

    public HerobrineMob getActiveMob(World world) {
        if (world == null) return null;
        return activeMobs.get(world.getName());
    }

    public boolean isHerobrineActive(World world) {
        HerobrineMob mob = getActiveMob(world);
        return mob != null && mob.isAlive();
    }

    public Map<String, HerobrineMob> getActiveMobs() {
        return Collections.unmodifiableMap(activeMobs);
    }
}
