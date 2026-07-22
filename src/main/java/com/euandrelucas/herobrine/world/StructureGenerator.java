package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Gerador de estruturas assustadoras e edições de mundo do Herobrine.
 * Utiliza o WorldEditManager para paginar edições de blocos sem travar o servidor.
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
     * 3.1: Cava túneis retos de seção 2x2 em terrenos rochosos.
     */
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

    /**
     * 3.2: Constrói pequenas pirâmides de areia em biomas oceânicos.
     */
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

    /**
     * 3.3: Remove todas as folhas em um raio de floresta (deixando só os troncos).
     */
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

    /**
     * 3.5: Coloca tochas de redstone próximas a estruturas do jogador.
     */
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

    /**
     * 3.7: Cria cruzes de madeira em florestas.
     */
    public void generateWoodenCross(Location groundLocation) {
        // Tronco vertical de 4 blocos
        for (int y = 0; y < 4; y++) {
            worldEditManager.queueBlockChange(groundLocation.clone().add(0, y, 0), Material.OAK_LOG);
        }
        // Braço horizontal
        worldEditManager.queueBlockChange(groundLocation.clone().add(1, 2, 0), Material.OAK_LOG);
        worldEditManager.queueBlockChange(groundLocation.clone().add(-1, 2, 0), Material.OAK_LOG);
    }

    /**
     * 3.8: Cria um túnel-isca que termina em blocos de diamante (armadilha/chamariz).
     */
    public void generateDiamondBaitTunnel(Location startLoc) {
        generateTunnel(startLoc, new Vector(1, 0, 0), 12);
        Location endLoc = startLoc.clone().add(12, 0, 0);
        worldEditManager.queueBlockChange(endLoc, Material.DIAMOND_ORE);
        worldEditManager.queueBlockChange(endLoc.clone().add(0, 1, 0), Material.DIAMOND_ORE);
    }

    /**
     * 3.10: Deixa placas com mensagens crípticas em um local visitado.
     */
    public void placeCrypticSign(Location location) {
        List<String> messages = messagesManager.getSignMessages();
        if (messages == null || messages.isEmpty()) return;

        String randomMsg = messages.get(random.nextInt(messages.size()));
        worldEditManager.queueBlockChange(location, Material.OAK_SIGN);

        // Atualização da placa após criação do bloco no tick principal
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Block block = location.getBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                sign.setLine(1, "§c" + randomMsg);
                sign.update();
            }
        }, 5L);
    }

    /**
     * 3.11: Abre/fecha portas próximas ao jogador sozinho ("Ghost Doors").
     */
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

    /**
     * 3.12: Executa explosões controladas sem destruir regiões protegidas.
     */
    public void triggerControlledExplosion(Location location, float power) {
        if (location == null || location.getWorld() == null) return;
        if (plugin.getHookManager().isLocationProtected(location)) return;

        location.getWorld().createExplosion(location, power, false, false);
    }
}
