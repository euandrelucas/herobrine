package com.euandrelucas.herobrine.commands;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import com.euandrelucas.herobrine.fear.FearManager;
import com.euandrelucas.herobrine.mobs.HerobrineMob;
import com.euandrelucas.herobrine.mobs.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Executor de todos os subcomandos do /herobrine (/hb).
 */
public class HerobrineCommand implements CommandExecutor, TabCompleter {

    private final HerobrinePlugin plugin;
    private final MobManager mobManager;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private final FearManager fearManager;
    private final Set<UUID> possessedStaff = new HashSet<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "spawn", "banish", "jumpscare", "disarm", "swarm", "kidnap", "possess", "clones", "jukebox", "fear", "setfear", "frequency", "state", "reload", "restorenether", "help"
    );

    public HerobrineCommand(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.mobManager = plugin.getMobManager();
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
        this.fearManager = plugin.getFearManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("herobrine.admin")) {
            sender.sendMessage(messagesManager.getFormattedMessage("no-permission", new HashMap<>()));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn":
                return handleSpawn(sender, args);
            case "banish":
                return handleBanish(sender, args);
            case "state":
                return handleState(sender, args);
            case "reload":
                return handleReload(sender);
            case "jumpscare":
                return handleJumpscare(sender, args);
            case "disarm":
                return handleDisarm(sender, args);
            case "swarm":
                return handleSwarm(sender, args);
            case "kidnap":
                return handleKidnap(sender, args);
            case "possess":
                return handlePossess(sender, args);
            case "clones":
                return handleClones(sender, args);
            case "jukebox":
                return handleJukebox(sender, args);
            case "fear":
                return handleFear(sender, args);
            case "setfear":
                return handleSetFear(sender, args);
            case "frequency":
                return handleFrequency(sender, args);
            case "restorenether":
                plugin.getNetherCorruptionManager().restoreNetherSnapshot();
                sender.sendMessage(MessagesManager.colorize("&aÁrea de corrupção do Nether restaurada com sucesso!"));
                return true;
            default:
                sender.sendMessage(messagesManager.getFormattedMessage("invalid-subcommand", new HashMap<>()));
                return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            sender.sendMessage(messagesManager.getFormattedMessage("only-players", new HashMap<>()));
            return true;
        }

        World world = target.getWorld();
        if (configManager.isWorldDisabled(world.getName())) {
            Map<String, String> ph = new HashMap<>();
            ph.put("world", world.getName());
            sender.sendMessage(messagesManager.getFormattedMessage("spawn.disabled-world", ph));
            return true;
        }

        if (mobManager.isHerobrineActive(world)) {
            Map<String, String> ph = new HashMap<>();
            ph.put("world", world.getName());
            sender.sendMessage(messagesManager.getFormattedMessage("spawn.already-active", ph));
            return true;
        }

        Location loc = target.getLocation().add(target.getLocation().getDirection().multiply(-10));
        boolean success = mobManager.spawnHerobrine(world, loc, target);

        // Ideia 5: Céu de Tempestade Sombria
        world.setStorm(true);
        world.setThundering(true);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());
        ph.put("world", world.getName());

        if (success) {
            sender.sendMessage(messagesManager.getFormattedMessage("spawn.success", ph));
        }
        return true;
    }

    /**
     * Ideia 4: Modo Evento / Staff Possuído (/hb possess <admin>).
     */
    private boolean handlePossess(CommandSender sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            sender.sendMessage(MessagesManager.colorize("&cUso: /herobrine possess [admin]"));
            return true;
        }

        UUID uuid = target.getUniqueId();
        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());

        if (possessedStaff.contains(uuid)) {
            possessedStaff.remove(uuid);
            target.setCustomName(target.getName());
            target.setCustomNameVisible(false);
            target.setGlowing(false);
            sender.sendMessage(messagesManager.getFormattedMessage("possess.disabled", ph));
        } else {
            possessedStaff.add(uuid);
            target.setCustomName("§cHerobrine");
            target.setCustomNameVisible(true);
            target.setGlowing(true);

            ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
                steveHead.setItemMeta(meta);
            }
            target.getInventory().setHelmet(steveHead);

            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            sender.sendMessage(messagesManager.getFormattedMessage("possess.enabled", ph));
        }
        return true;
    }

    private boolean handleClones(CommandSender sender, String[] args) {
        World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
        HerobrineMob mob = mobManager.getActiveMob(world);

        if (mob != null && mob.isAlive()) {
            mob.spawnIllusionClones();
            sender.sendMessage(MessagesManager.colorize("&aClones de ilusão do Herobrine invocados!"));
        } else {
            sender.sendMessage(MessagesManager.colorize("&cHerobrine precisa estar ativo no mundo para invocar clones."));
        }
        return true;
    }

    private boolean handleJukebox(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return true;

        plugin.getHorrorManager().triggerHauntedJukebox(target);
        sender.sendMessage(MessagesManager.colorize("&aToca-discos amaldiçoado ativado perto de &f" + target.getName()));
        return true;
    }

    private boolean handleBanish(CommandSender sender, String[] args) {
        World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
        boolean banished = mobManager.banishHerobrine(world);
        Map<String, String> ph = new HashMap<>();
        ph.put("world", world.getName());

        if (banished) {
            sender.sendMessage(messagesManager.getFormattedMessage("banish.success", ph));
        } else {
            sender.sendMessage(messagesManager.getFormattedMessage("banish.not-active", ph));
        }
        return true;
    }

    private boolean handleState(CommandSender sender, String[] args) {
        World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
        HerobrineMob mob = mobManager.getActiveMob(world);

        sender.sendMessage(MessagesManager.colorize("&8=== &cEstado Atual do Herobrine &8==="));
        if (mob != null && mob.isAlive()) {
            sender.sendMessage(MessagesManager.colorize("&eMundo: &f" + world.getName()));
            sender.sendMessage(MessagesManager.colorize("&eAlvo: &f" + mob.getTargetPlayer().getName()));
            sender.sendMessage(MessagesManager.colorize("&eEstado: &c" + mob.getStateMachine().getCurrentState()));
            sender.sendMessage(MessagesManager.colorize("&eNível de Raiva: &c" + mob.getStateMachine().getAngerLevel() + "/100"));
        } else {
            sender.sendMessage(MessagesManager.colorize("&eStatus: &cInativo no mundo " + world.getName()));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(messagesManager.getFormattedMessage("plugin-reloaded", new HashMap<>()));
        return true;
    }

    private boolean handleJumpscare(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return true;

        plugin.getHorrorManager().triggerJumpscare(target);
        plugin.getFearManager().addFear(target, 15);
        sender.sendMessage(MessagesManager.colorize("&aJumpscare disparado contra &f" + target.getName()));
        return true;
    }

    private boolean handleDisarm(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return true;

        plugin.getHorrorManager().triggerDisarmJumpscare(target);
        plugin.getFearManager().addFear(target, 20);
        sender.sendMessage(MessagesManager.colorize("&aDisarm Jumpscare (Drop Total) disparado contra &f" + target.getName()));
        return true;
    }

    private boolean handleSwarm(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return true;

        plugin.getHorrorManager().triggerStaringMobSwarm(target);
        plugin.getFearManager().addFear(target, 10);
        sender.sendMessage(MessagesManager.colorize("&aManada de mobs encarando enviada para &f" + target.getName()));
        return true;
    }

    private boolean handleKidnap(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return true;

        plugin.getKidnapManager().kidnapPlayer(target);
        plugin.getFearManager().addFear(target, 25);
        sender.sendMessage(MessagesManager.colorize("&aEvento de Sequestro disparado contra &f" + target.getName()));
        return true;
    }

    private Player getTargetPlayer(CommandSender sender, String[] args) {
        if (args.length > 1) {
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null) {
                sender.sendMessage(MessagesManager.colorize("&cJogador &f" + args[1] + " &cnão foi encontrado."));
            }
            return p;
        } else if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(MessagesManager.colorize("&cUso: /herobrine " + args[0] + " <jogador>"));
        return null;
    }

    private boolean handleFear(CommandSender sender, String[] args) {
        Player target = (args.length > 1) ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            sender.sendMessage(MessagesManager.colorize("&cUso: /herobrine fear <jogador>"));
            return true;
        }
        int fear = fearManager.getFear(target);
        Map<String, String> ph = new HashMap<>();
        ph.put("player", target.getName());
        ph.put("fear", String.valueOf(fear));
        sender.sendMessage(messagesManager.getFormattedMessage("fear.get", ph));
        return true;
    }

    private boolean handleSetFear(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessagesManager.colorize("&cUso: /herobrine setfear <jogador> <0-100>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessagesManager.colorize("&cJogador não encontrado."));
            return true;
        }
        try {
            int amount = Integer.parseInt(args[2]);
            fearManager.setFear(target, amount);
            Map<String, String> ph = new HashMap<>();
            ph.put("player", target.getName());
            ph.put("fear", String.valueOf(amount));
            sender.sendMessage(messagesManager.getFormattedMessage("fear.set", ph));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessagesManager.colorize("&cValor numérico inválido."));
        }
        return true;
    }

    private boolean handleFrequency(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessagesManager.colorize("&cUso: /herobrine frequency <minutos>"));
            return true;
        }
        try {
            int minutes = Integer.parseInt(args[1]);
            plugin.getConfig().set("spawn.interval-minutes", minutes);
            plugin.saveConfig();
            Map<String, String> ph = new HashMap<>();
            ph.put("minutes", String.valueOf(minutes));
            sender.sendMessage(messagesManager.getFormattedMessage("frequency.set", ph));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessagesManager.colorize("&cValor numérico inválido."));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessagesManager.colorize("&8=== &cComandos do Plugin Herobrine &8==="));
        sender.sendMessage(MessagesManager.colorize("&f/hb spawn [jogador] &7- Força manifestação do Herobrine."));
        sender.sendMessage(MessagesManager.colorize("&f/hb banish [mundo] &7- Remove o Herobrine do mundo."));
        sender.sendMessage(MessagesManager.colorize("&f/hb jumpscare <jogador> &7- Dispara susto em um jogador."));
        sender.sendMessage(MessagesManager.colorize("&f/hb disarm <jogador> &7- Susto que dropa TODO o inventário."));
        sender.sendMessage(MessagesManager.colorize("&f/hb swarm <jogador> &7- Spawna manada de mobs encarando."));
        sender.sendMessage(MessagesManager.colorize("&f/hb kidnap <jogador> &7- Sequestra o jogador para sala de bedrock."));
        sender.sendMessage(MessagesManager.colorize("&f/hb possess [admin] &7- Transforma staff em Herobrine (Modo Evento)."));
        sender.sendMessage(MessagesManager.colorize("&f/hb clones &7- Invoca clones ilusórios do Herobrine."));
        sender.sendMessage(MessagesManager.colorize("&f/hb jukebox <jogador> &7- Ativa toca-discos amaldiçoado."));
        sender.sendMessage(MessagesManager.colorize("&f/hb fear <jogador> &7- Exibe o nível de medo do jogador."));
        sender.sendMessage(MessagesManager.colorize("&f/hb setfear <jogador> <0-100> &7- Define o nível de medo."));
        sender.sendMessage(MessagesManager.colorize("&f/hb frequency <minutos> &7- Ajusta intervalo de spawn."));
        sender.sendMessage(MessagesManager.colorize("&f/hb restorenether &7- Restaura áreas de corrupção do Nether."));
        sender.sendMessage(MessagesManager.colorize("&f/hb state &7- Exibe estado e raiva atual do Herobrine."));
        sender.sendMessage(MessagesManager.colorize("&f/hb reload &7- Recarrega configurações e mensagens."));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
