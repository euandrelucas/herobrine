package com.euandrelucas.herobrine;

import com.euandrelucas.herobrine.apparition.Apparitions;
import com.euandrelucas.herobrine.command.HerobrineCommand;
import com.euandrelucas.herobrine.director.Director;
import com.euandrelucas.herobrine.director.Presence;
import com.euandrelucas.herobrine.listener.AltarListener;
import com.euandrelucas.herobrine.listener.PlayerListener;
import com.euandrelucas.herobrine.util.BlockQueue;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HerobrinePlugin extends JavaPlugin {

    private Presence presence;
    private BlockQueue blockQueue;
    private Apparitions apparitions;
    private Director director;

    @Override
    public void onEnable() {
        try {
            Class.forName("org.bukkit.entity.Mannequin");
        } catch (ClassNotFoundException e) {
            getLogger().severe("Este plugin precisa de Paper 1.21.9 ou superior (entidade Mannequin). Desativando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        presence = new Presence(this);
        blockQueue = new BlockQueue(this);
        apparitions = new Apparitions(this);
        director = new Director(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarListener(this), this);

        PluginCommand cmd = getCommand("herobrine");
        if (cmd != null) {
            HerobrineCommand handler = new HerobrineCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        apparitions.start();
        director.start();
        getLogger().info("Herobrine " + getPluginMeta().getVersion() + " ativo.");
    }

    @Override
    public void onDisable() {
        if (director != null) director.stop();
        if (apparitions != null) apparitions.removeAll();
        if (blockQueue != null) blockQueue.stop();
    }

    public void reload() {
        reloadConfig();
    }

    public void debug(String msg) {
        if (getConfig().getBoolean("debug")) getLogger().info("[debug] " + msg);
    }

    public Presence presence() { return presence; }
    public BlockQueue blockQueue() { return blockQueue; }
    public Apparitions apparitions() { return apparitions; }
    public Director director() { return director; }
}
