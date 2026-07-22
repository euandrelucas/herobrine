package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o evento clássico de Sequestro/Teleporte temporário do Herobrine.
 * O jogador é transportado para uma sala escura de bedrock com tochas de redstone por alguns segundos,
 * e depois é retornado em segurança à sua localização original.
 */
public class KidnapManager {

    private final HerobrinePlugin plugin;
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public KidnapManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executa o evento de sequestro no jogador.
     */
    public void kidnapPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        if (savedLocations.containsKey(uuid)) return; // Já está em sequestro

        Location originalLoc = player.getLocation().clone();
        savedLocations.put(uuid, originalLoc);

        // Cria câmara subterrânea temporária de bedrock
        World world = player.getWorld();
        Location chamberCenter = originalLoc.clone().add(0, 80, 0); // Posição no alto ou subterrânea distante
        if (chamberCenter.getY() > 250) {
            chamberCenter.setY(200);
        }

        buildTemporaryChamber(chamberCenter);

        // Teleporta o jogador para dentro da sala escura
        Location insideChamber = chamberCenter.clone().add(1.5, 1, 1.5);
        player.teleport(insideChamber);

        // Efeitos de terror
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
        player.playSound(insideChamber, Sound.AMBIENT_CAVE, 1.5f, 0.5f);
        player.sendTitle("§c§lVOCÊ NÃO PODE ESCAPAR", "§8Herobrine capturou você...", 10, 60, 10);

        // Retorna o jogador em segurança após 8 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(savedLocations.remove(uuid));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage("§c...Você foi devolvido ao mundo real.");
            }
            removeTemporaryChamber(chamberCenter);
        }, 160L); // 8 segundos
    }

    private void buildTemporaryChamber(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    Location blockLoc = new Location(world, cX + x, cY + y, cZ + z);
                    boolean isHollow = (x > 0 && x < 3) && (y > 0 && y < 3) && (z > 0 && z < 3);

                    if (isHollow) {
                        blockLoc.getBlock().setType(Material.AIR);
                    } else {
                        blockLoc.getBlock().setType(Material.BEDROCK);
                    }
                }
            }
        }

        // Coloca uma tocha de redstone na parede
        world.getBlockAt(cX + 1, cY + 2, cZ + 1).setType(Material.REDSTONE_TORCH);
    }

    private void removeTemporaryChamber(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    world.getBlockAt(cX + x, cY + y, cZ + z).setType(Material.AIR);
                }
            }
        }
    }
}
