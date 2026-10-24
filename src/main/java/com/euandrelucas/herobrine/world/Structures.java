package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.item.Items;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Jukebox;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Construtores de estruturas do Herobrine. Cada um recebe o canto (bloco logo acima do chão)
 * e devolve a lista de blocos, ou {@code null} se o terreno não serve.
 */
public final class Structures {

    private Structures() {}

    // ------------------------------------------------------------------ helpers

    private static Material mat(Block b) {
        return b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4) ? b.getType() : null;
    }

    private static boolean clear(Block b) {
        Material m = mat(b);
        return m != null && (m.isAir() || b.isReplaceable()) && !b.isLiquid();
    }

    private static boolean solid(Block b) {
        Material m = mat(b);
        return m != null && b.isSolid() && !b.isLiquid() && !Tag.LEAVES.isTagged(m);
    }

    /** Área w x d completamente plana: ar acima, chão sólido abaixo. */
    public static boolean flat(Block origin, int w, int d, int height) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                Block b = origin.getRelative(x, 0, z);
                if (!solid(b.getRelative(BlockFace.DOWN))) return false;
                for (int y = 0; y < height; y++) {
                    if (!clear(b.getRelative(0, y, 0))) return false;
                }
            }
        }
        return true;
    }

    private static Block at(Block o, int x, int y, int z) {
        return o.getRelative(x, y, z);
    }

    // ------------------------------------------------------------------ pirâmides

    public static List<Placement> pyramid(Block o, Material m) {
        int size = 7;
        if (!flat(o, size, size, 5)) return null;
        List<Placement> out = new ArrayList<>();
        for (int layer = 0; layer < 4; layer++) {
            for (int x = layer; x < size - layer; x++) {
                for (int z = layer; z < size - layer; z++) {
                    out.add(Placement.of(at(o, x, layer, z), m));
                }
            }
        }
        out.add(Placement.of(at(o, 3, 4, 3), Material.REDSTONE_TORCH));
        return out;
    }

    // ------------------------------------------------------------------ cruz e bedrock falso

    public static List<Placement> bedrockCross(Block o) {
        if (!flat(o, 3, 1, 6)) return null;
        List<Placement> out = new ArrayList<>();
        for (int y = 0; y < 5; y++) out.add(Placement.fake(at(o, 1, y, 0), Material.BEDROCK));
        out.add(Placement.fake(at(o, 0, 3, 0), Material.BEDROCK));
        out.add(Placement.fake(at(o, 2, 3, 0), Material.BEDROCK));
        return out;
    }

    public static List<Placement> floatingBedrock(Block o) {
        int h = 14 + Util.rng().nextInt(10);
        List<Placement> out = new ArrayList<>();
        int n = 1 + Util.rng().nextInt(3);
        for (int i = 0; i < n; i++) {
            Block b = at(o, i, h, 0);
            if (!clear(b)) return null;
            out.add(Placement.fake(b, Material.BEDROCK));
        }
        return out;
    }

    public static List<Placement> woodenCross(Block o) {
        if (!flat(o, 3, 1, 6)) return null;
        List<Placement> out = new ArrayList<>();
        for (int y = 0; y < 4; y++) out.add(Placement.of(at(o, 1, y, 0), Material.OAK_LOG));
        out.add(Placement.of(at(o, 0, 2, 0), Material.OAK_LOG));
        out.add(Placement.of(at(o, 2, 2, 0), Material.OAK_LOG));
        out.add(Placement.of(at(o, 1, 4, 0), Material.REDSTONE_TORCH));
        return out;
    }

    // ------------------------------------------------------------------ monumentos e estátua

    public static List<Placement> monument(Block o) {
        if (!flat(o, 3, 3, 7)) return null;
        List<Placement> out = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) out.add(Placement.of(at(o, x, 0, z), Material.COBBLESTONE));
        for (int y = 1; y < 5; y++) out.add(Placement.of(at(o, 1, y, 1), Material.STONE_BRICKS));
        out.add(Placement.of(at(o, 1, 5, 1), Material.REDSTONE_TORCH));
        return out;
    }

    /** Estátua de pedra com olhos brancos, de frente para o sul/norte. */
    public static List<Placement> statue(Block o) {
        if (!flat(o, 5, 1, 9)) return null;
        Material s = Material.STONE_BRICKS;
        List<Placement> out = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            out.add(Placement.of(at(o, 1, y, 0), s));
            out.add(Placement.of(at(o, 3, y, 0), s));
        }
        for (int y = 3; y < 6; y++) {
            for (int x = 1; x <= 3; x++) out.add(Placement.of(at(o, x, y, 0), s));
            out.add(Placement.of(at(o, 0, y, 0), s));
            out.add(Placement.of(at(o, 4, y, 0), s));
        }
        for (int y = 6; y < 8; y++) {
            for (int x = 1; x <= 3; x++) {
                boolean eye = y == 7 && (x == 1 || x == 3);
                out.add(Placement.of(at(o, x, y, 0), eye ? Material.WHITE_CONCRETE : s));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ letras

    private static final String[][] FONT = {
            {"X...X", "X...X", "XXXXX", "X...X", "X...X"}, // H
            {"XXXXX", "X....", "XXXX.", "X....", "XXXXX"}, // E
            {"XXXX.", "X...X", "XXXX.", "X..X.", "X...X"}, // R
            {".XXX.", "X...X", "X...X", "X...X", ".XXX."}, // O
            {"XXXX.", "X...X", "XXXX.", "X...X", "XXXX."}, // B
            {"XXXXX", "..X..", "..X..", "..X..", "XXXXX"}, // I
            {"X...X", "XX..X", "X.X.X", "X..XX", "X...X"}  // N
    };
    private static final int[] WORD = {0, 1, 2, 3, 4, 2, 5, 6, 1}; // HEROBRINE

    /** "HEROBRINE" em netherrack, acompanhando o relevo. */
    public static List<Placement> letters(Block o) {
        World w = o.getWorld();
        List<Placement> out = new ArrayList<>();
        int x0 = o.getX();
        int z0 = o.getZ();
        for (int li = 0; li < WORD.length; li++) {
            String[] g = FONT[WORD[li]];
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    if (g[r].charAt(c) != 'X') continue;
                    int x = x0 + li * 6 + c;
                    int z = z0 + r;
                    if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
                    Block top = w.getBlockAt(x, w.getHighestBlockYAt(x, z), z);
                    Material m = top.getType();
                    if (top.isLiquid() || Tag.LEAVES.isTagged(m) || Tag.LOGS.isTagged(m) || !top.isSolid()) continue;
                    out.add(Placement.of(top, Material.NETHERRACK));
                }
            }
        }
        return out.size() < 40 ? null : out;
    }

    // ------------------------------------------------------------------ santuário em ruínas

    public static List<Placement> ruinedShrine(Block o) {
        if (!flat(o, 3, 3, 3)) return null;
        List<Placement> out = new ArrayList<>();
        Block base = at(o, 0, -1, 0);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                boolean center = x == 1 && z == 1;
                Material m = center ? Material.MOSSY_COBBLESTONE
                        : Util.rng().nextDouble() < 0.4 ? Material.MOSSY_COBBLESTONE
                        : Util.rng().nextDouble() < 0.5 ? Material.COBBLESTONE : Material.GOLD_BLOCK;
                out.add(Placement.of(base.getRelative(x, 0, z), m));
            }
        }
        out.add(Placement.of(at(o, 1, 0, 1), Material.NETHERRACK));
        if (Util.rng().nextBoolean()) out.add(Placement.of(at(o, 0, 0, 1), Material.REDSTONE_TORCH));
        if (Util.rng().nextBoolean()) out.add(Placement.of(at(o, 2, 0, 1), Material.REDSTONE_TORCH));
        return out;
    }

    // ------------------------------------------------------------------ casa armadilhada

    public static List<Placement> trappedHouse(HerobrinePlugin plugin, Block o) {
        int s = 7;
        if (!flat(o, s, s, 4)) return null;
        List<Placement> out = new ArrayList<>();
        Material plank = Material.OAK_PLANKS;
        // piso (um bloco abaixo do chão visível) e teto
        for (int x = 0; x < s; x++) {
            for (int z = 0; z < s; z++) {
                out.add(Placement.of(at(o, x, -1, z), plank));
                out.add(Placement.of(at(o, x, 3, z), plank));
            }
        }
        // paredes
        for (int y = 0; y < 3; y++) {
            for (int i = 0; i < s; i++) {
                boolean corner = (i == 0 || i == s - 1);
                Material m = corner ? Material.OAK_LOG : plank;
                out.add(Placement.of(at(o, i, y, 0), m));
                out.add(Placement.of(at(o, i, y, s - 1), m));
                if (i != 0 && i != s - 1) {
                    out.add(Placement.of(at(o, 0, y, i), plank));
                    out.add(Placement.of(at(o, s - 1, y, i), plank));
                }
            }
        }
        // porta (parede z=0)
        Door lower = (Door) Material.OAK_DOOR.createBlockData();
        lower.setFacing(BlockFace.SOUTH);
        lower.setHalf(Bisected.Half.BOTTOM);
        Door upper = (Door) Material.OAK_DOOR.createBlockData();
        upper.setFacing(BlockFace.SOUTH);
        upper.setHalf(Bisected.Half.TOP);
        out.add(Placement.of(at(o, 3, 0, 0), lower));
        out.add(Placement.of(at(o, 3, 1, 0), upper));
        // janelas
        out.add(Placement.of(at(o, 0, 1, 3), Material.GLASS_PANE));
        out.add(Placement.of(at(o, s - 1, 1, 3), Material.GLASS_PANE));
        out.add(Placement.of(at(o, 3, 1, s - 1), Material.GLASS_PANE));
        // interior: cama, baú, tocha
        Bed foot = (Bed) Material.RED_BED.createBlockData();
        foot.setFacing(BlockFace.NORTH);
        foot.setPart(Bed.Part.FOOT);
        Bed head = (Bed) Material.RED_BED.createBlockData();
        head.setFacing(BlockFace.NORTH);
        head.setPart(Bed.Part.HEAD);
        out.add(Placement.of(at(o, 1, 0, 5), foot));
        out.add(Placement.of(at(o, 1, 0, 4), head));
        out.add(Placement.of(at(o, 5, 0, 5), Material.CHEST, b -> fillChest(plugin, b, false)));
        out.add(Placement.of(at(o, 1, 0, 1), Material.TORCH));
        // armadilha: placa de pressão sobre o piso, TNT embaixo do piso
        out.add(Placement.of(at(o, 3, -2, 1), Material.TNT));
        out.add(Placement.of(at(o, 3, 0, 1), Material.STONE_PRESSURE_PLATE));
        return out;
    }

    // ------------------------------------------------------------------ base de sobreviventes

    public static List<Placement> survivorBase(HerobrinePlugin plugin, Block o) {
        int s = 11;
        if (!flat(o, s, s, 4)) return null;
        List<Placement> out = new ArrayList<>();
        for (int i = 0; i < s; i++) {
            for (int y = 0; y < 3; y++) {
                for (int[] p : new int[][]{{i, 0}, {i, s - 1}, {0, i}, {s - 1, i}}) {
                    if (p[1] == 0 && i == 5 && y < 2) continue; // entrada
                    if (p[1] == 0 && i == 5 && y < 2) continue;
                    Material m = Util.rng().nextDouble() < 0.3 ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE;
                    out.add(Placement.of(at(o, p[0], y, p[1]), m));
                }
            }
            if (i % 3 == 0) {
                out.add(Placement.of(at(o, i, 3, 0), Material.TORCH));
                out.add(Placement.of(at(o, i, 3, s - 1), Material.TORCH));
                out.add(Placement.of(at(o, 0, 3, i), Material.TORCH));
                out.add(Placement.of(at(o, s - 1, 3, i), Material.TORCH));
            }
        }
        Bed foot = (Bed) Material.RED_BED.createBlockData();
        foot.setFacing(BlockFace.NORTH);
        foot.setPart(Bed.Part.FOOT);
        Bed head = (Bed) Material.RED_BED.createBlockData();
        head.setFacing(BlockFace.NORTH);
        head.setPart(Bed.Part.HEAD);
        out.add(Placement.of(at(o, 1, 0, 9), foot));
        out.add(Placement.of(at(o, 1, 0, 8), head));
        out.add(Placement.of(at(o, 9, 0, 9), Material.CRAFTING_TABLE));
        out.add(Placement.of(at(o, 9, 0, 8), Material.FURNACE));
        out.add(Placement.of(at(o, 9, 0, 7), Material.CHEST, b -> fillChest(plugin, b, true)));
        out.add(Placement.of(at(o, 1, 0, 1), Material.JUKEBOX, b -> {
            if (b.getState() instanceof Jukebox j) {
                j.setRecord(new ItemStack(Material.MUSIC_DISC_CAT));
                j.update();
            }
        }));
        out.add(Placement.of(at(o, 5, 0, 5), Material.AIR,
                b -> plugin.survivors().spawn(b.getLocation().add(0.5, 0, 0.5))));
        return out;
    }

    private static void fillChest(HerobrinePlugin plugin, Block b, boolean rich) {
        if (!(b.getState() instanceof Chest chest)) return;
        Inventory inv = chest.getBlockInventory();
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(Material.BREAD, 3 + Util.rng().nextInt(4)));
        loot.add(new ItemStack(Material.COAL, 4 + Util.rng().nextInt(7)));
        loot.add(new ItemStack(Material.IRON_INGOT, 2 + Util.rng().nextInt(4)));
        loot.add(new ItemStack(Material.TORCH, 8));
        loot.add(new ItemStack(Material.COOKED_BEEF, 3));
        if (rich) {
            loot.add(new ItemStack(Material.IRON_SWORD));
            loot.add(new ItemStack(Material.IRON_CHESTPLATE));
            loot.add(new ItemStack(Material.GOLDEN_APPLE, 1));
            loot.add(new ItemStack(Material.GOLD_INGOT, 3 + Util.rng().nextInt(5)));
        }
        if (Util.rng().nextDouble() < (rich ? 0.6 : 0.25)) loot.add(plugin.items().create(Items.Id.CURSED_DUST, 1 + Util.rng().nextInt(2)));
        if (Util.rng().nextDouble() < 0.08) loot.add(new ItemStack(Material.DIAMOND));
        for (ItemStack it : loot) inv.setItem(Util.rng().nextInt(inv.getSize()), it);
    }
}
