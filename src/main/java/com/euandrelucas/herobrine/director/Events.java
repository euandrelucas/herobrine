package com.euandrelucas.herobrine.director;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Todos os eventos que o Herobrine pode causar. Cada um devolve true se aconteceu. */
public final class Events {

    public interface Runner {
        boolean run(HerobrinePlugin plugin, Player p);
    }

    public record Def(String id, int minPresence, Runner runner) {}

    public static final List<Def> ALL = List.of(
            new Def("sounds", 0, Events::sounds),
            new Def("footsteps", 5, Events::footsteps),
            new Def("animals-stare", 8, Events::animalsStare),
            new Def("sighting", 10, Events::sighting),
            new Def("extinguish", 12, Events::extinguish),
            new Def("doors", 12, Events::doors),
            new Def("window", 15, Events::window),
            new Def("strip-tree", 15, Events::stripTree),
            new Def("tunnel", 20, Events::tunnel),
            new Def("sign", 25, Events::sign),
            new Def("fake-join", 25, Events::fakeJoin),
            new Def("storm", 30, Events::storm),
            new Def("cross", 35, Events::cross),
            new Def("netherrack-fire", 35, Events::netherrackFire),
            new Def("stalk", 40, Events::stalk),
            new Def("pyramid", 45, Events::pyramid),
            new Def("chase", 55, Events::chase),
            new Def("hostile", 75, Events::hostile)
    );

    private static final Set<Material> STONEISH = Set.of(Material.STONE, Material.DEEPSLATE, Material.ANDESITE,
            Material.DIORITE, Material.GRANITE, Material.TUFF, Material.DIRT, Material.GRAVEL, Material.NETHERRACK);

    private Events() {}

    public static Def find(String id) {
        for (Def d : ALL) if (d.id().equalsIgnoreCase(id)) return d;
        return null;
    }

    // ------------------------------------------------------------------ sons

    private static boolean sounds(HerobrinePlugin plugin, Player p) {
        Sound[] pool = {Sound.AMBIENT_CAVE, Sound.ENTITY_PLAYER_BREATH, Sound.BLOCK_CHEST_CLOSE,
                Sound.BLOCK_WOODEN_DOOR_CLOSE, Sound.ENTITY_ENDERMAN_AMBIENT, Sound.BLOCK_GRAVEL_BREAK,
                Sound.ENTITY_ZOMBIE_AMBIENT};
        Sound s = pool[Util.rng().nextInt(pool.length)];
        Location at = Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), Util.rng().nextDouble(5, 14));
        at.setY(p.getLocation().getY());
        p.playSound(at, s, 1.1f, s == Sound.AMBIENT_CAVE ? 1f : (float) Util.rng().nextDouble(0.5, 0.8));
        return true;
    }

    private static boolean footsteps(HerobrinePlugin plugin, Player p) {
        Block under = p.getLocation().subtract(0, 0.5, 0).getBlock();
        if (!under.isSolid()) return false;
        Sound step = under.getBlockData().getSoundGroup().getStepSound();
        double yaw = p.getLocation().getYaw() + 180 + Util.rng().nextDouble(-25, 25);
        int steps = 7 + Util.rng().nextInt(5);
        for (int i = 0; i < steps; i++) {
            final double dist = 7 - i * 0.45;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                Location at = Util.pointAround(p.getLocation(), yaw, Math.max(2.5, dist));
                p.playSound(at, step, 1f, 0.9f);
            }, 9L * i);
        }
        return true;
    }

    private static boolean fakeJoin(HerobrinePlugin plugin, Player p) {
        String msg = plugin.getConfig().getString("messages.fake-join", "§e{name} entrou no jogo");
        p.sendMessage(Util.color(msg.replace("{name}", "Herobrine")));
        return true;
    }

    // ------------------------------------------------------------ mobs / mundo próximo

    private static boolean animalsStare(HerobrinePlugin plugin, Player p) {
        List<Mob> mobs = new ArrayList<>();
        for (Entity e : p.getNearbyEntities(22, 10, 22)) {
            if (e instanceof Animals a && a.hasAI()) mobs.add(a);
        }
        if (mobs.size() < 2) return false;
        for (Mob m : mobs) m.setAI(false);
        int[] ticks = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            ticks[0] += 4;
            boolean done = ticks[0] > 240 || !p.isOnline();
            for (Mob m : mobs) {
                if (!m.isValid()) continue;
                if (done) m.setAI(true);
                else m.lookAt(p.getEyeLocation());
            }
            if (done) task.cancel();
        }, 1L, 4L);
        return true;
    }

    private static boolean extinguish(HerobrinePlugin plugin, Player p) {
        List<Block> torches = new ArrayList<>();
        Location c = p.getLocation();
        for (int x = -14; x <= 14; x++) for (int y = -5; y <= 6; y++) for (int z = -14; z <= 14; z++) {
            Block b = c.getWorld().getBlockAt(c.getBlockX() + x, c.getBlockY() + y, c.getBlockZ() + z);
            Material t = b.getType();
            if ((t == Material.TORCH || t == Material.WALL_TORCH || t == Material.SOUL_TORCH
                    || t == Material.SOUL_WALL_TORCH) && Util.viewAngle(p, b.getLocation().add(0.5, 0.5, 0.5)) > 70) {
                torches.add(b);
            }
        }
        if (torches.isEmpty()) return false;
        java.util.Collections.shuffle(torches);
        int n = Math.min(torches.size(), 2 + Util.rng().nextInt(4));
        for (int i = 0; i < n; i++) {
            Block b = torches.get(i);
            b.breakNaturally();
            p.playSound(b.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1f);
        }
        return true;
    }

    private static boolean doors(HerobrinePlugin plugin, Player p) {
        List<Block> doors = new ArrayList<>();
        Location c = p.getLocation();
        for (int x = -14; x <= 14; x++) for (int y = -3; y <= 4; y++) for (int z = -14; z <= 14; z++) {
            Block b = c.getWorld().getBlockAt(c.getBlockX() + x, c.getBlockY() + y, c.getBlockZ() + z);
            if (Tag.WOODEN_DOORS.isTagged(b.getType()) && b.getBlockData() instanceof Openable o && !o.isOpen()
                    && Util.viewAngle(p, b.getLocation().add(0.5, 0.5, 0.5)) > 60) {
                doors.add(b);
            }
        }
        if (doors.isEmpty()) return false;
        Block d = doors.get(Util.rng().nextInt(doors.size()));
        toggleDoor(d, true);
        p.playSound(d.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            toggleDoor(d, false);
            p.playSound(d.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 0.8f);
        }, 60L + Util.rng().nextInt(80));
        return true;
    }

    private static void toggleDoor(Block b, boolean open) {
        if (!(b.getBlockData() instanceof org.bukkit.block.data.type.Door door)) return;
        Block bottom = door.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM ? b : b.getRelative(BlockFace.DOWN);
        for (Block part : new Block[]{bottom, bottom.getRelative(BlockFace.UP)}) {
            if (part.getBlockData() instanceof Openable o) {
                o.setOpen(open);
                part.setBlockData(o);
            }
        }
    }

    // ------------------------------------------------------------ aparições

    private static boolean sighting(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        Location l = viewSpot(p, 22, 40, 55);
        return plugin.apparitions().spawn(p, Apparition.Mode.STARE, l);
    }

    private static boolean stalk(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        Location l = viewSpot(p, 20, 30, 60);
        return plugin.apparitions().spawn(p, Apparition.Mode.STALK, l);
    }

    private static boolean chase(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        Location l = viewSpot(p, 26, 34, 35);
        return plugin.apparitions().spawn(p, Apparition.Mode.CHASE, l);
    }

    private static boolean hostile(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p)) return false;
        Location l = viewSpot(p, 12, 18, 80);
        if (l != null) p.getWorld().strikeLightningEffect(l);
        return plugin.apparitions().spawn(p, Apparition.Mode.HOSTILE, l);
    }

    /** Ponto no campo de visão do jogador (ângulo máximo) com linha de visão limpa. */
    public static Location viewSpot(Player p, double min, double max, double maxAngle) {
        Location pl = p.getLocation();
        for (int i = 0; i < 30; i++) {
            double yaw = pl.getYaw() + Util.rng().nextDouble(-maxAngle, maxAngle);
            Location c = Util.pointAround(pl, yaw, Util.rng().nextDouble(min, max));
            Location floor = Util.findFloor(pl.getWorld(), c.getBlockX(), c.getBlockZ(), pl.getBlockY());
            if (floor != null && Util.lineOfSight(p, floor.clone().add(0, 1.6, 0))) return floor;
        }
        return null;
    }

    private static boolean window(HerobrinePlugin plugin, Player p) {
        if (plugin.apparitions().has(p) || !Util.indoors(p)) return false;
        Location eye = p.getEyeLocation();
        List<Location> spots = new ArrayList<>();
        for (int a = 0; a < 360; a += 20) {
            Vector dir = Util.pointAround(new Location(p.getWorld(), 0, 0, 0), a, 1).toVector().setY(0);
            RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, 10, FluidCollisionMode.NEVER, true);
            if (r == null || r.getHitBlock() == null) continue;
            String n = r.getHitBlock().getType().name();
            if (!n.endsWith("GLASS") && !n.endsWith("GLASS_PANE")) continue;
            Location out = r.getHitBlock().getLocation().add(0.5, 0, 0.5).add(dir.clone().multiply(2));
            Location floor = Util.findFloor(p.getWorld(), out.getBlockX(), out.getBlockZ(), p.getLocation().getBlockY());
            if (floor != null && Math.abs(floor.getY() - p.getLocation().getY()) <= 2) spots.add(floor);
        }
        if (spots.isEmpty()) return false;
        return plugin.apparitions().spawn(p, Apparition.Mode.WINDOW, spots.get(Util.rng().nextInt(spots.size())));
    }

    // ------------------------------------------------------------ alterações no mundo

    private static boolean stripTree(HerobrinePlugin plugin, Player p) {
        Location c = p.getLocation();
        World w = c.getWorld();
        Block log = null;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < 400; i++) {
            int x = c.getBlockX() + Util.rng().nextInt(-40, 41);
            int z = c.getBlockZ() + Util.rng().nextInt(-40, 41);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = w.getHighestBlockYAt(x, z);
            for (int dy = 0; dy < 12; dy++) {
                Block b = w.getBlockAt(x, y - dy, z);
                if (Tag.LOGS.isTagged(b.getType())) {
                    double d = b.getLocation().distanceSquared(c);
                    if (d > 144 && d < best) {
                        best = d;
                        log = b;
                    }
                    break;
                }
            }
        }
        if (log == null) return false;
        // Flood fill por toras e folhas conectadas
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> todo = new ArrayDeque<>();
        todo.add(log);
        seen.add(log);
        List<Block> leaves = new ArrayList<>();
        while (!todo.isEmpty() && seen.size() < 600) {
            Block b = todo.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                Block n = b.getRelative(dx, dy, dz);
                if (Math.abs(n.getX() - log.getX()) > 8 || Math.abs(n.getZ() - log.getZ()) > 8) continue;
                if (!seen.add(n)) continue;
                if (Tag.LEAVES.isTagged(n.getType())) {
                    leaves.add(n);
                    todo.add(n);
                } else if (Tag.LOGS.isTagged(n.getType())) {
                    todo.add(n);
                }
            }
        }
        if (leaves.isEmpty()) return false;
        for (Block l : leaves) plugin.blockQueue().set(l, Material.AIR);
        return true;
    }

    private static boolean tunnel(HerobrinePlugin plugin, Player p) {
        if (!Util.underground(p)) return false;
        Location c = p.getLocation();
        World w = c.getWorld();
        int[][] axes = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int attempt = 0; attempt < 12; attempt++) {
            int[] a = axes[Util.rng().nextInt(4)];
            int perpX = a[1], perpZ = a[0];
            int sx = c.getBlockX() + a[0] * Util.rng().nextInt(8, 16) + perpX * Util.rng().nextInt(-6, 7);
            int sz = c.getBlockZ() + a[1] * Util.rng().nextInt(8, 16) + perpZ * Util.rng().nextInt(-6, 7);
            int y = c.getBlockY();
            if (!w.isChunkLoaded(sx >> 4, sz >> 4)) continue;
            // O túnel corre perpendicular à direção do jogador (corredor ao lado).
            int dx = perpX == 0 ? 0 : 1, dz = perpZ == 0 ? 0 : 1;
            if (dx == 0 && dz == 0) continue;
            List<Block> carve = new ArrayList<>();
            int len = Util.rng().nextInt(10, 19);
            for (int i = 0; i < len; i++) {
                Block feet = w.getBlockAt(sx + dx * i, y, sz + dz * i);
                Block head = feet.getRelative(BlockFace.UP);
                boolean feetOk = STONEISH.contains(feet.getType()) || feet.getType().isAir();
                boolean headOk = STONEISH.contains(head.getType()) || head.getType().isAir();
                if (!feetOk || !headOk) break;
                // não corta perto demais do jogador
                if (feet.getLocation().distanceSquared(c) < 25) break;
                carve.add(feet);
                carve.add(head);
            }
            if (carve.size() < 12) continue;
            for (Block b : carve) plugin.blockQueue().set(b, Material.AIR);
            return true;
        }
        return false;
    }

    private static boolean sign(HerobrinePlugin plugin, Player p) {
        Location spot = null;
        for (int i = 0; i < 20 && spot == null; i++) {
            double yaw = p.getLocation().getYaw() + 180 + Util.rng().nextDouble(-100, 100);
            Location c = Util.pointAround(p.getLocation(), yaw, Util.rng().nextDouble(5, 11));
            Location floor = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getLocation().getBlockY());
            if (floor != null && floor.getBlock().getType().isAir() && Util.viewAngle(p, floor) > 80) spot = floor;
        }
        if (spot == null) return false;
        return placeSign(plugin, spot.getBlock(), p.getLocation());
    }

    /** Coloca uma placa com texto sinistro virada para {@code facing}. Usado também ao acordar. */
    public static boolean placeSign(HerobrinePlugin plugin, Block at, Location facing) {
        if (!at.getType().isAir() || !at.getRelative(BlockFace.DOWN).isSolid()) return false;
        List<?> options = plugin.getConfig().getList("messages.sign-lines");
        if (options == null || options.isEmpty()) return false;
        Object pick = options.get(Util.rng().nextInt(options.size()));
        if (!(pick instanceof List<?> lines)) return false;

        at.setType(Material.OAK_SIGN, false);
        if (at.getBlockData() instanceof Rotatable rot) {
            Vector to = facing.toVector().subtract(at.getLocation().toVector());
            rot.setRotation(yawToFace(to));
            at.setBlockData(rot, false);
        }
        if (at.getState() instanceof Sign sign) {
            for (int i = 0; i < 4 && i < lines.size(); i++) {
                sign.getSide(Side.FRONT).line(i, Component.text(String.valueOf(lines.get(i))));
            }
            sign.setWaxed(true);
            sign.update(true, false);
        }
        return true;
    }

    private static BlockFace yawToFace(Vector dir) {
        BlockFace[] faces = {BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST,
                BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST};
        double yaw = Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        int idx = (int) Math.round(((yaw % 360) + 360) % 360 / 45.0) % 8;
        return faces[idx];
    }

    private static Location groundNear(Player p, double min, double max) {
        for (int i = 0; i < 25; i++) {
            Location c = Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), Util.rng().nextDouble(min, max));
            Location floor = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getLocation().getBlockY());
            if (floor != null && floor.getBlock().getType().isAir()
                    && !floor.clone().subtract(0, 1, 0).getBlock().isLiquid()) return floor;
        }
        return null;
    }

    private static boolean cross(HerobrinePlugin plugin, Player p) {
        Location f = groundNear(p, 16, 34);
        if (f == null) return false;
        Material under = f.clone().subtract(0, 1, 0).getBlock().getType();
        if (under != Material.GRASS_BLOCK && under != Material.DIRT && under != Material.PODZOL) return false;
        Block base = f.getBlock();
        for (int i = 0; i < 4; i++) {
            if (!Util.free(base.getRelative(0, i, 0))) return false;
        }
        for (int i = 0; i < 4; i++) plugin.blockQueue().set(base.getRelative(0, i, 0), Material.OAK_LOG);
        int[][] arms = {{1, 0}, {-1, 0}};
        boolean alongX = Util.rng().nextBoolean();
        for (int[] a : arms) {
            Block arm = base.getRelative(alongX ? a[0] : a[1], 2, alongX ? a[1] : a[0]);
            if (Util.free(arm)) plugin.blockQueue().set(arm, Material.OAK_LOG);
        }
        plugin.blockQueue().set(base.getRelative(0, 4, 0), Material.REDSTONE_TORCH);
        return true;
    }

    private static boolean netherrackFire(HerobrinePlugin plugin, Player p) {
        Location f = groundNear(p, 15, 32);
        if (f == null) return false;
        Block top = f.getBlock();
        // nada inflamável por perto, para o fogo ficar contido
        for (int x = -2; x <= 2; x++) for (int y = -1; y <= 3; y++) for (int z = -2; z <= 2; z++) {
            if (top.getRelative(x, y, z).getType().isBurnable() || Tag.LEAVES.isTagged(top.getRelative(x, y, z).getType())) {
                return false;
            }
        }
        Block ground = top.getRelative(BlockFace.DOWN);
        plugin.blockQueue().set(ground, Material.NETHERRACK);
        plugin.blockQueue().set(top, Material.FIRE);
        return true;
    }

    private static boolean pyramid(HerobrinePlugin plugin, Player p) {
        Location f = groundNear(p, 24, 40);
        if (f == null) return false;
        int size = 7;
        Block origin = f.getBlock().getRelative(-size / 2, 0, -size / 2);
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            Block b = origin.getRelative(x, 0, z);
            if (!Util.free(b) || !b.getRelative(BlockFace.DOWN).isSolid()) return false; // terreno precisa ser plano
        }
        for (int layer = 0; layer < 4; layer++) {
            for (int x = layer; x < size - layer; x++) for (int z = layer; z < size - layer; z++) {
                plugin.blockQueue().set(origin.getRelative(x, layer, z), Material.SANDSTONE);
            }
        }
        plugin.blockQueue().set(origin.getRelative(size / 2, 4, size / 2), Material.REDSTONE_TORCH);
        return true;
    }

    private static boolean storm(HerobrinePlugin plugin, Player p) {
        World w = p.getWorld();
        if (w.hasStorm() || w.getEnvironment() != World.Environment.NORMAL) return false;
        w.setStorm(true);
        w.setThundering(true);
        w.setWeatherDuration(4000);
        w.setThunderDuration(4000);
        Location at = Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), 40);
        w.strikeLightningEffect(at);
        return true;
    }

    /** Ao acordar, o Herobrine mexeu na casa. Chamado pelo listener de cama. */
    public static void wakeTrick(HerobrinePlugin plugin, Player p) {
        switch (Util.rng().nextInt(3)) {
            case 0 -> extinguish(plugin, p);
            case 1 -> doors(plugin, p);
            default -> {
                for (int i = 0; i < 10; i++) {
                    Location c = Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), Util.rng().nextDouble(2, 4));
                    Block b = c.getBlock();
                    Location fl = Util.findFloor(p.getWorld(), b.getX(), b.getZ(), p.getLocation().getBlockY());
                    if (fl != null && placeSign(plugin, fl.getBlock(), p.getLocation())) return;
                }
            }
        }
    }
}
