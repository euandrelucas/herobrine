package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.HerobrinePlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Estado por mundo: invocado (Herobrine despertou) e purificado (Herobrine removido). */
public final class WorldState {

    private final HerobrinePlugin plugin;
    private final File file;
    private final Set<String> summoned = new HashSet<>();
    private final Set<String> purified = new HashSet<>();

    public WorldState(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        summoned.addAll(yml.getStringList("summoned"));
        purified.addAll(yml.getStringList("purified"));
    }

    /** O Herobrine pode agir neste mundo? */
    public boolean active(World w) {
        if (purified.contains(w.getName())) return false;
        return !plugin.getConfig().getBoolean("summon.required", false) || summoned.contains(w.getName());
    }

    public boolean isSummoned(World w) { return summoned.contains(w.getName()); }
    public boolean isPurified(World w) { return purified.contains(w.getName()); }

    public void setSummoned(World w, boolean v) {
        if (v) {
            summoned.add(w.getName());
            purified.remove(w.getName());
        } else {
            summoned.remove(w.getName());
        }
        save();
    }

    public void setPurified(World w, boolean v) {
        if (v) {
            purified.add(w.getName());
            summoned.remove(w.getName());
        } else {
            purified.remove(w.getName());
        }
        save();
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("summoned", summoned.stream().toList());
        yml.set("purified", purified.stream().toList());
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui salvar state.yml: " + e.getMessage());
        }
    }
}
