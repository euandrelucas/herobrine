package com.euandrelucas.herobrine.command;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.director.Events;
import com.euandrelucas.herobrine.director.Presence;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class HerobrineCommand implements CommandExecutor, TabCompleter {

    private final HerobrinePlugin plugin;

    public HerobrineCommand(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            s.sendMessage("§c/hb <spawn|event|presence|banish|reload|info>");
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage("§aConfig recarregada.");
            }
            case "spawn" -> {
                if (a.length < 2) { s.sendMessage("§c/hb spawn <stare|window|stalk|chase|hostile> [jogador]"); return true; }
                Player t = target(s, a, 2);
                if (t == null) return true;
                Apparition.Mode mode;
                try {
                    mode = Apparition.Mode.valueOf(a[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    s.sendMessage("§cModo inválido.");
                    return true;
                }
                if (plugin.apparitions().has(t)) plugin.apparitions().remove(t);
                var spot = Events.viewSpot(t, mode == Apparition.Mode.HOSTILE ? 10 : 20, mode == Apparition.Mode.HOSTILE ? 16 : 32, 70);
                s.sendMessage(plugin.apparitions().spawn(t, mode, spot) ? "§aOk." : "§cSem local adequado.");
            }
            case "event" -> {
                if (a.length < 2) {
                    s.sendMessage("§cEventos: " + String.join(", ", Events.ALL.stream().map(Events.Def::id).toList()));
                    return true;
                }
                Events.Def d = Events.find(a[1]);
                if (d == null) { s.sendMessage("§cEvento desconhecido."); return true; }
                Player t = target(s, a, 2);
                if (t == null) return true;
                s.sendMessage(d.runner().run(plugin, t) ? "§aExecutado." : "§cNão achou lugar/condição adequada.");
            }
            case "presence" -> {
                Player t = target(s, a, 1);
                if (t == null) return true;
                if (a.length >= 4 && a[2].equalsIgnoreCase("set")) {
                    try {
                        plugin.presence().set(t, Double.parseDouble(a[3]));
                    } catch (NumberFormatException ex) {
                        s.sendMessage("§cNúmero inválido.");
                        return true;
                    }
                }
                double v = plugin.presence().get(t);
                s.sendMessage("§7Presença de " + t.getName() + ": §f" + String.format("%.1f", v) + " §7(estágio " + Presence.stage(v) + ")");
            }
            case "banish" -> {
                if (a.length >= 2 && !a[1].equalsIgnoreCase("all")) {
                    Player t = plugin.getServer().getPlayerExact(a[1]);
                    if (t != null) plugin.apparitions().remove(t);
                } else {
                    plugin.getServer().getOnlinePlayers().forEach(plugin.apparitions()::remove);
                }
                s.sendMessage("§aBanido.");
            }
            case "info" -> {
                s.sendMessage("§7Herobrine " + plugin.getPluginMeta().getVersion()
                        + " | aparições ativas: " + plugin.getServer().getOnlinePlayers().stream().filter(plugin.apparitions()::has).count());
            }
            default -> s.sendMessage("§cSubcomando desconhecido.");
        }
        return true;
    }

    private Player target(CommandSender s, String[] a, int idx) {
        if (a.length > idx) {
            Player t = plugin.getServer().getPlayerExact(a[idx]);
            if (t == null) s.sendMessage("§cJogador offline.");
            return t;
        }
        if (s instanceof Player p) return p;
        s.sendMessage("§cInforme um jogador.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("spawn", "event", "presence", "banish", "reload", "info"));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("spawn")) {
            Arrays.stream(Apparition.Mode.values()).forEach(m -> out.add(m.name().toLowerCase(Locale.ROOT)));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("event")) {
            Events.ALL.forEach(d -> out.add(d.id()));
        } else if (a.length == 3 && a[0].equalsIgnoreCase("presence")) {
            out.add("set");
        } else {
            plugin.getServer().getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String prefix = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
