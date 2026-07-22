package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Gerador de estruturas assustadoras, Casas de Lava, monumento de Glowstone e túneis.
 */
public class StructureGenerator {

    private final HerobrinePlugin plugin;
    private final WorldEditManager worldEditManager;
    private final MessagesManager messagesManager;
    private final Random random = new Random();

    public StructureGenerator(HerobrinePlugin plugin, WorldEditManager worldEditManager) {
        this.plugin = plugin;
        this.worldEditManager = worldEditManager;
        this.messagesManager = plugin.getMessagesManager();
    }

    /**
     * Estrutura Clássica: Casa de Lava 5x5 de Cobblestone com teto de vidro e poço de lava.
     */
    public void generateLavaHouse(Location baseLoc) {
        if (baseLoc == null || baseLoc.getWorld() == null) return;

        // Paredes e estrutura 5x4x5
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = baseLoc.clone().add(x, y, z);
                    boolean isWall = (x == -2 || x == 2 || z == -2 || z == 2);
                    boolean isRoof = (y == 3);
                    boolean isFloor = (y == 0);

                    if (isRoof) {
                        worldEditManager.queueBlockChange(loc, Material.GLASS);
                    } else if (isWall) {
                        // Deixa entrada na parede frontal
                        if (x == 0 && z == 2 && (y == 1 || y == 2)) {
                            worldEditManager.queueBlockChange(loc, Material.AIR);
                        } else {
                            worldEditManager.queueBlockChange(loc, Material.COBBLESTONE);
                        }
                    } else if (isFloor) {
                        // Poço central de lava
                        if (x == 0 && z == 0) {
                            worldEditManager.queueBlockChange(loc, Material.LAVA);
                        } else {
                            worldEditManager.queueBlockChange(loc, Material.NETHERRACK);
                        }
                    } else {
                        // Interior
                        if (x == 0 && z == 0 && y == 2) {
                            worldEditManager.queueBlockChange(loc, Material.LAVA); // Lava caindo do teto
                        } else {
                            worldEditManager.queueBlockChange(loc, Material.AIR);
                        }
                    }
                }
            }
        }

        // Tochas de redstone nos 4 cantos internos
        worldEditManager.queueBlockChange(baseLoc.clone().add(1, 1, 1), Material.REDSTONE_TORCH);
        worldEditManager.queueBlockChange(baseLoc.clone().add(-1, 1, 1), Material.REDSTONE_TORCH);
        worldEditManager.queueBlockChange(baseLoc.clone().add(1, 1, -1), Material.REDSTONE_TORCH);
        worldEditManager.queueBlockChange(baseLoc.clone().add(-1, 1, -1), Material.REDSTONE_TORCH);
    }

    /**
     * 3.6: Escreve o nome "HEROBRINE" usando blocos de GLOWSTONE em um local visível.
     */
    public void generateGlowstoneNameMonument(Location startLoc) {
        Material mat = Material.GLOWSTONE;
        int[][] patternH = {{1,0,1},{1,0,1},{1,1,1},{1,0,1},{1,0,1}};
        int[][] patternE = {{1,1,1},{1,0,0},{1,1,1},{1,0,0},{1,1,1}};
        int[][] patternR = {{1,1,1},{1,0,1},{1,1,1},{1,1,0},{1,0,1}};
        int[][] patternO = {{1,1,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1}};
        int[][] patternB = {{1,1,1},{1,0,1},{1,1,0},{1,0,1},{1,1,1}};
        int[][] patternI = {{1,1,1},{0,1,0},{0,1,0},{0,1,0},{1,1,1}};
        int[][] patternN = {{1,0,1},{1,1,1},{1,0,1},{1,0,1},{1,0,1}};

        Location current = startLoc.clone();
        for (int[][][] letter : new int[][][][]{{patternH}, {patternE}, {patternR}, {patternO}, {patternB}, {patternR}, {patternI}, {patternN}, {patternE}}) {
            int[][] p = letter[0];
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (p[row][col] == 1) {
                        Location blockLoc = current.clone().add(col, 4 - row, 0);
                        worldEditManager.queueBlockChange(blockLoc, mat);
                    }
                }
            }
            current.add(4, 0, 0);
        }
    }

    public void generateTunnel(Location startLoc, Vector direction, int length) {
        Location current = startLoc.clone();
        for (int i = 0; i < length; i++) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    Location blockLoc = current.clone().add(dx, dy, 0);
                    worldEditManager.queueBlockChange(blockLoc, Material.AIR);
                }
            }
            current.add(direction);
        }
    }

    public void generateSandPyramid(Location baseCenterLoc) {
        int height = 4;
        for (int y = 0; y < height; y++) {
            int radius = height - 1 - y;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = baseCenterLoc.clone().add(x, y, z);
                    worldEditManager.queueBlockChange(blockLoc, Material.SAND);
                }
            }
        }
    }

    public void stripForestLeaves(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int x = cX - radius; x <= cX + radius; x++) {
            for (int y = cY - 10; y <= cY + 15; y++) {
                for (int z = cZ - radius; z <= cZ + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String name = block.getType().name();
                    if (name.contains("LEAVES")) {
                        worldEditManager.queueBlockChange(block.getLocation(), Material.AIR);
                    }
                }
            }
        }
    }

    public void placeRedstoneTorchesNearPlayer(Player player) {
        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) return;

        for (int i = 0; i < 4; i++) {
            int offsetX = random.nextInt(10) - 5;
            int offsetZ = random.nextInt(10) - 5;
            Location checkLoc = pLoc.clone().add(offsetX, 0, offsetZ);

            Location highest = world.getHighestBlockAt(checkLoc).getLocation().add(0, 1, 0);
            worldEditManager.queueBlockChange(highest, Material.REDSTONE_TORCH);
        }
    }

    public void generateWoodenCross(Location groundLocation) {
        for (int y = 0; y < 4; y++) {
            worldEditManager.queueBlockChange(groundLocation.clone().add(0, y, 0), Material.OAK_LOG);
        }
        worldEditManager.queueBlockChange(groundLocation.clone().add(1, 2, 0), Material.OAK_LOG);
        worldEditManager.queueBlockChange(groundLocation.clone().add(-1, 2, 0), Material.OAK_LOG);
    }

    public void generateDiamondBaitTunnel(Location startLoc) {
        generateTunnel(startLoc, new Vector(1, 0, 0), 12);
        Location endLoc = startLoc.clone().add(12, 0, 0);
        worldEditManager.queueBlockChange(endLoc, Material.DIAMOND_ORE);
        worldEditManager.queueBlockChange(endLoc.clone().add(0, 1, 0), Material.DIAMOND_ORE);
    }

    public void placeCrypticSign(Location location) {
        List<String> messages = messagesManager.getSignMessages();
        if (messages == null || messages.isEmpty()) return;

        String randomMsg = messages.get(random.nextInt(messages.size()));
        worldEditManager.queueBlockChange(location, Material.OAK_SIGN);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block block = location.getBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                sign.setLine(1, "§c" + randomMsg);
                sign.update();
            }
        }, 5L);
    }

    public void triggerGhostDoorsNearPlayer(Player player) {
        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) return;

        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    Block block = world.getBlockAt(pLoc.clone().add(x, y, z));
                    if (block.getBlockData() instanceof Door) {
                        Door door = (Door) block.getBlockData();
                        door.setOpen(!door.isOpen());
                        block.setBlockData(door);
                    }
                }
            }
        }
    }

    public void triggerControlledExplosion(Location location, float power) {
        if (location == null || location.getWorld() == null) return;
        if (plugin.getHookManager().isLocationProtected(location)) return;

        location.getWorld().createExplosion(location, power, false, false);
    }
}
