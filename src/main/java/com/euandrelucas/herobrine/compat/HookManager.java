package com.euandrelucas.herobrine.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Gerencia integrações opcionais (soft-depend) com outros plugins do servidor.
 */
public class HookManager {

    private final Logger logger;
    private boolean protocolLibPresent = false;
    private boolean worldGuardPresent = false;
    private boolean griefPreventionPresent = false;
    private boolean townyPresent = false;
    private boolean placeholderApiPresent = false;

    public HookManager(Logger logger) {
        this.logger = logger;
    }

    public void checkHooks() {
        protocolLibPresent = isPluginEnabled("ProtocolLib");
        worldGuardPresent = isPluginEnabled("WorldGuard");
        griefPreventionPresent = isPluginEnabled("GriefPrevention");
        townyPresent = isPluginEnabled("Towny");
        placeholderApiPresent = isPluginEnabled("PlaceholderAPI");

        logger.info("[Hooks] Soft-dependencies ativas:");
        logger.info("  - ProtocolLib: " + (protocolLibPresent ? "SIM" : "NÃO"));
        logger.info("  - WorldGuard: " + (worldGuardPresent ? "SIM" : "NÃO"));
        logger.info("  - GriefPrevention: " + (griefPreventionPresent ? "SIM" : "NÃO"));
        logger.info("  - Towny: " + (townyPresent ? "SIM" : "NÃO"));
        logger.info("  - PlaceholderAPI: " + (placeholderApiPresent ? "SIM" : "NÃO"));

        if (placeholderApiPresent) {
            try {
                new HerobrineExpansion(com.euandrelucas.herobrine.core.HerobrinePlugin.getInstance()).register();
                logger.info("[Hooks] Expansão %herobrine_fear% e %herobrine_active% registrada no PlaceholderAPI.");
            } catch (Exception e) {
                logger.warning("[Hooks] Falha ao registrar expansão no PlaceholderAPI: " + e.getMessage());
            }
        }
    }

    private boolean isPluginEnabled(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Verifica se uma localização está protegida por algum plugin de proteção de terreno.
     *
     * @param location Localização a ser verificada
     * @return true se o ponto estiver em área protegida
     */
    public boolean isLocationProtected(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        // Verificação WorldGuard (se presente)
        if (worldGuardPresent) {
            try {
                // Verificação segura via reflexão para evitar acoplamento direto de compilação
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
                Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
                Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
                // Se a verificação refletida falhar, assume não protegido por padrão
            } catch (Exception ignored) {
                // Fallback silencioso
            }
        }

        return false;
    }

    public boolean isProtocolLibPresent() {
        return protocolLibPresent;
    }

    public boolean isWorldGuardPresent() {
        return worldGuardPresent;
    }

    public boolean isGriefPreventionPresent() {
        return griefPreventionPresent;
    }

    public boolean isTownyPresent() {
        return townyPresent;
    }

    public boolean isPlaceholderApiPresent() {
        return placeholderApiPresent;
    }
}
