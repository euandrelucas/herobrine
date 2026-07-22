package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia pinturas assombradas que mudam de arte quando o jogador não está olhando diretamente.
 */
public class HauntedPaintingManager implements Listener {

    private final HerobrinePlugin plugin;
    private final Map<UUID, Art> originalArts = new HashMap<>();

    public HauntedPaintingManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location eye = player.getEyeLocation();

        for (Entity entity : player.getNearbyEntities(10, 5, 10)) {
            if (entity instanceof Painting) {
                Painting painting = (Painting) entity;
                UUID paintingId = painting.getUniqueId();

                boolean isLookingDirectly = isPlayerLookingAtPainting(player, painting);

                if (isLookingDirectly) {
                    // Reverte para a pintura original se o jogador olhar diretamente
                    if (originalArts.containsKey(paintingId)) {
                        painting.setArt(originalArts.remove(paintingId), true);
                    }
                } else {
                    // Troca para uma arte assombrada (ex: SKULL_AND_ROSES) quando não está olhando diretamente
                    if (!originalArts.containsKey(paintingId) && painting.getArt() != Art.SKULL_AND_ROSES) {
                        originalArts.put(paintingId, painting.getArt());
                        painting.setArt(Art.SKULL_AND_ROSES, true);
                    }
                }
            }
        }
    }

    private boolean isPlayerLookingAtPainting(Player player, Painting painting) {
        Location eye = player.getEyeLocation();
        Location pLoc = painting.getLocation();
        org.bukkit.util.Vector toPainting = pLoc.toVector().subtract(eye.toVector()).normalize();
        org.bukkit.util.Vector playerDirection = eye.getDirection().normalize();

        return playerDirection.dot(toPainting) > 0.80;
    }
}
