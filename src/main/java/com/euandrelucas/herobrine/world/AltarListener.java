package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import com.euandrelucas.herobrine.mobs.MobManager;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * Listener que detecta o ritual do Altar do Herobrine.
 * Estrutura: Netherrack central + tochas de redstone + 8 blocos de ouro/cobblestone musgoso.
 * Ao acender fogo sobre a netherrack, dispara raio e invoca Herobrine.
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
        Block block = event.getBlock(); // Onde o fogo será criado
        Block netherrack = block.getRelative(0, -1, 0);

        if (netherrack.getType() != Material.NETHERRACK) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // Verifica permissão para o altar se configurado
        if (plugin.getConfig().getBoolean("altar.require-permission", true) && !player.hasPermission("herobrine.usealtar")) {
            return;
        }

        World world = block.getWorld();
        if (configManager.isWorldDisabled(world.getName())) return;

        // Checa padrão de blocos ao redor
        if (isAltarStructure(netherrack)) {
            Location spawnLoc = netherrack.getLocation().add(0.5, 1, 0.5);

            // Efeito visual de raio
            if (plugin.getConfig().getBoolean("altar.strike-lightning", true)) {
                if (plugin.getConfig().getBoolean("altar.lightning-damage", false)) {
                    world.strikeLightning(spawnLoc);
                } else {
                    world.strikeLightningEffect(spawnLoc);
                }
            }

            // Invoca Herobrine no altar
            boolean spawned = mobManager.spawnHerobrine(world, spawnLoc, player);

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

    private boolean isAltarStructure(Block netherrack) {
        World world = netherrack.getWorld();
        int nX = netherrack.getX();
        int nY = netherrack.getY();
        int nZ = netherrack.getZ();

        boolean allowMossy = plugin.getConfig().getBoolean("altar.allow-mossy-cobblestone-substitute", true);

        // Verifica base 3x3 sob a netherrack
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Pula a netherrack central
                Material type = world.getBlockAt(nX + x, nY, nZ + z).getType();
                boolean isGold = type == Material.GOLD_BLOCK;
                boolean isMossy = allowMossy && type == Material.MOSSY_COBBLESTONE;
                if (!isGold && !isMossy) {
                    return false;
                }
            }
        }

        // Verifica tochas de redstone nos 4 cantos ou lados superiores
        int torchCount = 0;
        int[][] torchOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : torchOffsets) {
            Block top = world.getBlockAt(nX + offset[0], nY + 1, nZ + offset[1]);
            if (top.getType() == Material.REDSTONE_TORCH) {
                torchCount++;
            }
        }

        return torchCount >= 2; // Requer ao menos 2 tochas de redstone para ativar
    }
}
