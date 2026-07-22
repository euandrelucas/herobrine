package com.euandrelucas.herobrine.core;

import com.euandrelucas.herobrine.commands.HerobrineCommand;
import com.euandrelucas.herobrine.compat.HookManager;
import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.fear.FearManager;
import com.euandrelucas.herobrine.listeners.PlayerListener;
import com.euandrelucas.herobrine.listeners.PsychologicalHorrorManager;
import com.euandrelucas.herobrine.mobs.HerobrineMob;
import com.euandrelucas.herobrine.mobs.MobManager;
import com.euandrelucas.herobrine.mobs.PossessedMobManager;
import com.euandrelucas.herobrine.world.AltarListener;
import com.euandrelucas.herobrine.world.HauntedPaintingManager;
import com.euandrelucas.herobrine.world.NetherCorruptionManager;
import com.euandrelucas.herobrine.world.StructureGenerator;
import com.euandrelucas.herobrine.world.WorldEditManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Classe principal do plugin Herobrine.
 *
 * @author euandrelucas
 */
public class HerobrinePlugin extends JavaPlugin {

    private static HerobrinePlugin instance;

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private MobManager mobManager;
    private HookManager hookManager;
    private WorldEditManager worldEditManager;
    private StructureGenerator structureGenerator;
    private HauntedPaintingManager hauntedPaintingManager;
    private NetherCorruptionManager netherCorruptionManager;
    private FearManager fearManager;
    private PossessedMobManager possessedMobManager;
    private PsychologicalHorrorManager horrorManager;

    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("=========================================");
        getLogger().info("  Iniciando Herobrine Plugin v" + getDescription().getVersion());
        getLogger().info("  Autor: " + String.join(", ", getDescription().getAuthors()));
        getLogger().info("=========================================");

        // 1. Inicializa Gerenciadores Core
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.messagesManager = new MessagesManager(this);
        this.messagesManager.loadMessages();

        this.mobManager = new MobManager();

        this.hookManager = new HookManager(getLogger());
        this.hookManager.checkHooks();

        // 2. Inicializa Módulos do Mundo e Terror
        this.worldEditManager = new WorldEditManager(this);
        this.structureGenerator = new StructureGenerator(this, worldEditManager);
        this.hauntedPaintingManager = new HauntedPaintingManager(this);
        this.netherCorruptionManager = new NetherCorruptionManager(this, worldEditManager);
        this.fearManager = new FearManager(this);
        this.possessedMobManager = new PossessedMobManager(this);
        this.horrorManager = new PsychologicalHorrorManager(this);

        // 3. Registra Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AltarListener(this), this);
        Bukkit.getPluginManager().registerEvents(hauntedPaintingManager, this);
        Bukkit.getPluginManager().registerEvents(possessedMobManager, this);
        Bukkit.getPluginManager().registerEvents(horrorManager, this);

        // 4. Registra Comandos
        HerobrineCommand commandExecutor = new HerobrineCommand(this);
        if (getCommand("herobrine") != null) {
            getCommand("herobrine").setExecutor(commandExecutor);
            getCommand("herobrine").setTabCompleter(commandExecutor);
        }

        // 5. Inicia Tarefa de Tick da IA
        startTickTask();

        getLogger().info("Herobrine Plugin ativado com todas as funcionalidades!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Desativando Herobrine Plugin...");
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (worldEditManager != null) {
            worldEditManager.shutdown();
        }
        if (mobManager != null) {
            mobManager.banishAll();
        }
        if (fearManager != null) {
            fearManager.saveStorage();
        }
        getLogger().info("Herobrine Plugin desativado com sucesso.");
    }

    public void reloadAll() {
        configManager.loadConfig();
        messagesManager.loadMessages();
        hookManager.checkHooks();
    }

    private void startTickTask() {
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (mobManager != null) {
                for (HerobrineMob mob : mobManager.getActiveMobs().values()) {
                    if (mob != null && mob.isAlive()) {
                        mob.getStateMachine().update();
                    }
                }
            }
        }, 20L, 20L);
    }

    public static HerobrinePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public WorldEditManager getWorldEditManager() {
        return worldEditManager;
    }

    public StructureGenerator getStructureGenerator() {
        return structureGenerator;
    }

    public NetherCorruptionManager getNetherCorruptionManager() {
        return netherCorruptionManager;
    }

    public FearManager getFearManager() {
        return fearManager;
    }

    public PossessedMobManager getPossessedMobManager() {
        return possessedMobManager;
    }

    public PsychologicalHorrorManager getHorrorManager() {
        return horrorManager;
    }
}
