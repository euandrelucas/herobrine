package com.euandrelucas.herobrine.compat;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expansão do PlaceholderAPI para o plugin Herobrine.
 * Expõe %herobrine_fear% e %herobrine_active%.
 */
public class HerobrineExpansion extends PlaceholderExpansion {

    private final HerobrinePlugin plugin;

    public HerobrineExpansion(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "herobrine";
    }

    @Override
    public @NotNull String getAuthor() {
        return "euandrelucas";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("fear")) {
            if (player != null && player.isOnline()) {
                Player p = player.getPlayer();
                if (p != null) {
                    return String.valueOf(plugin.getFearManager().getFear(p));
                }
            }
            return "0";
        }

        if (params.equalsIgnoreCase("active")) {
            if (player != null && player.isOnline()) {
                Player p = player.getPlayer();
                if (p != null) {
                    boolean active = plugin.getMobManager().isHerobrineActive(p.getWorld());
                    return active ? "true" : "false";
                }
            }
            return "false";
        }

        return null;
    }
}
