package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import com.euandrelucas.herobrine.mobs.MobManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener que detecta a construção e ativação do Altar/Totem do Herobrine.
 * Suporta o Altar Clássico (Ouro/Netherrack/Redstone) e o Santuário do Nether (Blackstone/Soul Soil/Soul Torches).
 * Dispara raio, partículas e trovão no totem ao ser aceso.
 */
public class AltarListener implements Listener {

    private final HerobrinePlugin plugin;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private final MobManager mobManager;

    public AltarListener(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
        this.mobManager = plugin.getMobManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock(); // Onde o fogo foi aceso
        Block centralBlock = block.getRelative(0, -1, 0);

        if (centralBlock.getType() != Material.NETHERRACK && centralBlock.getType() != Material.SOUL_SOIL) return;

        Player player = event.getPlayer();
        if (player == null) return;

        if (plugin.getConfig().getBoolean("altar.require-permission", true) && !player.hasPermission("herobrine.usealtar")) {
            return;
        }

        World world = block.getWorld();
        if (configManager.isWorldDisabled(world.getName())) return;

        boolean isClassicAltar = isClassicAltarStructure(centralBlock);
        boolean isNetherSanctuary = isNetherSanctuaryStructure(centralBlock);

        if (isClassicAltar || isNetherSanctuary) {
            Location totemTopLoc = centralBlock.getLocation().add(0.5, 1.0, 0.5);

            // 1. Dispara o RAIO EXATAMENTE sobre o totem
            if (plugin.getConfig().getBoolean("altar.strike-lightning", true)) {
                if (plugin.getConfig().getBoolean("altar.lightning-damage", false)) {
                    world.strikeLightning(totemTopLoc);
                } else {
                    world.strikeLightningEffect(totemTopLoc);
                }
            }

            // 2. Partículas extras de fumaça, chamas e som de trovão retumbante
            world.spawnParticle(Particle.FLAME, totemTopLoc, 50, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.SMOKE_LARGE, totemTopLoc, 30, 0.5, 0.5, 0.5, 0.05);
            world.playSound(totemTopLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);

            // 3. Invoca o Herobrine no totem
            boolean spawned = mobManager.spawnHerobrine(world, totemTopLoc, player);

            Map<String, String> ph = new HashMap<>();
            ph.put("player", player.getName());
            ph.put("world", world.getName());

            if (spawned) {
                player.sendMessage(messagesManager.getFormattedMessage("spawn.success", ph));
            } else {
                player.sendMessage(messagesManager.getFormattedMessage("spawn.already-active", ph));
            }
        }
    }

    /**
     * Valida o Altar Clássico: Netherrack central + 8 Ouro (ou Mossy Cobblestone) + Tochas de Redstone.
     */
    private boolean isClassicAltarStructure(Block netherrack) {
        if (netherrack.getType() != Material.NETHERRACK) return false;
        World world = netherrack.getWorld();
        int nX = netherrack.getX();
        int nY = netherrack.getY();
        int nZ = netherrack.getZ();

        boolean allowMossy = plugin.getConfig().getBoolean("altar.allow-mossy-cobblestone-substitute", true);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                Material type = world.getBlockAt(nX + x, nY, nZ + z).getType();
                boolean isGold = type == Material.GOLD_BLOCK;
                boolean isMossy = allowMossy && type == Material.MOSSY_COBBLESTONE;
                if (!isGold && !isMossy) {
                    return false;
                }
            }
        }

        int torchCount = 0;
        int[][] torchOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : torchOffsets) {
            Block top = world.getBlockAt(nX + offset[0], nY + 1, nZ + offset[1]);
            if (top.getType() == Material.REDSTONE_TORCH) {
                torchCount++;
            }
        }

        return torchCount >= 2;
    }

    /**
     * Valida o Santuário do Nether (Seção 4.3): Soul Soil / Netherrack central + Blackstone + Soul Torches.
     */
    private boolean isNetherSanctuaryStructure(Block central) {
        World world = central.getWorld();
        if (world.getEnvironment() != World.Environment.NETHER && !plugin.getConfig().getBoolean("nether-mode.allow-sanctuary-everywhere", false)) {
            return false;
        }

        int nX = central.getX();
        int nY = central.getY();
        int nZ = central.getZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                Material type = world.getBlockAt(nX + x, nY, nZ + z).getType();
                if (type != Material.BLACKSTONE && type != Material.POLISHED_BLACKSTONE) {
                    return false;
                }
            }
        }

        int torchCount = 0;
        int[][] torchOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : torchOffsets) {
            Block top = world.getBlockAt(nX + offset[0], nY + 1, nZ + offset[1]);
            if (top.getType() == Material.SOUL_TORCH) {
                torchCount++;
            }
        }

        return torchCount >= 2;
    }
}
