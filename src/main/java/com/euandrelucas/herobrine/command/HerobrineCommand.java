package com.euandrelucas.herobrine.command;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.director.Events;
import com.euandrelucas.herobrine.director.Presence;
import com.euandrelucas.herobrine.item.Items;
import org.bukkit.Location;
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

    private static final double[] PHASE_PRESENCE = {0, 15, 35, 55, 75};

    private final HerobrinePlugin plugin;

    public HerobrineCommand(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            s.sendMessage("§c/hb <spawn|event|presence|phase|give|world|banish|reload|info>");
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage("§aConfig recarregada.");
            }
            case "spawn" -> spawn(s, a);
            case "event" -> event(s, a);
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
                s.sendMessage("§7Presença de " + t.getName() + ": §f" + String.format("%.1f", v) + " §7(fase " + Presence.stage(v) + ")");
            }
            case "phase" -> {
                // /hb phase <jogador> <0-4> | add | reset
                Player t = target(s, a, 1);
                if (t == null) return true;
                if (a.length < 3) {
                    s.sendMessage("§7Fase de " + t.getName() + ": §f" + Presence.stage(plugin.presence().get(t)));
                    return true;
                }
                int cur = Presence.stage(plugin.presence().get(t));
                int next;
                switch (a[2].toLowerCase(Locale.ROOT)) {
                    case "add" -> next = Math.min(4, cur + 1);
                    case "reset" -> next = 0;
                    default -> {
                        try {
                            next = Math.max(0, Math.min(4, Integer.parseInt(a[2])));
                        } catch (NumberFormatException ex) {
                            s.sendMessage("§cUse 0-4, add ou reset.");
                            return true;
                        }
                    }
                }
                plugin.presence().set(t, PHASE_PRESENCE[next]);
                s.sendMessage("§aFase de " + t.getName() + " agora é " + next + ".");
            }
            case "give" -> give(s, a);
            case "world" -> world(s, a);
            case "banish" -> {
                if (a.length >= 2 && !a[1].equalsIgnoreCase("all")) {
                    Player t = plugin.getServer().getPlayerExact(a[1]);
                    if (t != null) plugin.apparitions().remove(t);
                } else {
                    plugin.getServer().getOnlinePlayers().forEach(plugin.apparitions()::remove);
                }
                s.sendMessage("§aBanido.");
            }
            case "info" -> s.sendMessage("§7Herobrine " + plugin.getPluginMeta().getVersion()
                    + " | aparições ativas: " + plugin.apparitions().all().size());
            default -> s.sendMessage("§cSubcomando desconhecido.");
        }
        return true;
    }

    private void spawn(CommandSender s, String[] a) {
        if (a.length < 2) {
            s.sendMessage("§c/hb spawn <" + String.join("|", Arrays.stream(Apparition.Mode.values())
                    .filter(m -> m != Apparition.Mode.BUILDER).map(m -> m.name().toLowerCase(Locale.ROOT)).toList()) + "> [jogador]");
            return;
        }
        Player t = target(s, a, 2);
        if (t == null) return;
        Apparition.Mode mode;
        try {
            mode = Apparition.Mode.valueOf(a[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            s.sendMessage("§cModo inválido.");
            return;
        }
        if (mode == Apparition.Mode.BUILDER) {
            s.sendMessage("§cO construtor aparece sozinho ao construir estruturas (/hb event mossy-pyramid).");
            return;
        }
        if (plugin.apparitions().has(t)) plugin.apparitions().remove(t);
        Location spot = switch (mode) {
            case CIRCLE -> t.getLocation();
            case HOSTILE, MAGE, BOSS -> Events.viewSpot(t, 10, 16, 70);
            case FLASH -> Events.viewSpot(t, 4, 6, 40);
            case WEEPING -> Events.viewSpot(t, 18, 28, 70);
            default -> Events.viewSpot(t, 20, 32, 70);
        };
        s.sendMessage(plugin.apparitions().spawn(t, mode, spot) ? "§aOk." : "§cSem local adequado.");
    }

    private void event(CommandSender s, String[] a) {
        if (a.length < 2) {
            s.sendMessage("§cEventos: " + String.join(", ", Events.ALL.stream().map(Events.Def::id).toList()));
            return;
        }
        Events.Def d = Events.find(a[1]);
        if (d == null) {
            s.sendMessage("§cEvento desconhecido.");
            return;
        }
        Player t = target(s, a, 2);
        if (t == null) return;
        s.sendMessage(d.runner().run(plugin, t) ? "§aExecutado." : "§cNão achou lugar/condição adequada.");
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 2) {
            s.sendMessage("§c/hb give <item> [jogador] [quantidade]. Itens: "
                    + String.join(", ", Arrays.stream(Items.Id.values()).map(Items.Id::key).toList()));
            return;
        }
        Items.Id id;
        try {
            id = Items.Id.valueOf(a[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            s.sendMessage("§cItem desconhecido.");
            return;
        }
        Player t = target(s, a, 2);
        if (t == null) return;
        int amount = 1;
        if (a.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(a[3])));
            } catch (NumberFormatException ignored) {
            }
        }
        t.getInventory().addItem(plugin.items().create(id, amount)).values()
                .forEach(left -> t.getWorld().dropItemNaturally(t.getLocation(), left));
        s.sendMessage("§aDado " + amount + "x " + id.key() + " a " + t.getName() + ".");
    }

    private void world(CommandSender s, String[] a) {
        if (!(s instanceof Player p) && a.length < 3) {
            s.sendMessage("§cInforme o mundo.");
            return;
        }
        org.bukkit.World w = a.length >= 3 ? plugin.getServer().getWorld(a[2]) : ((Player) s).getWorld();
        if (w == null) {
            s.sendMessage("§cMundo desconhecido.");
            return;
        }
        String sub = a.length >= 2 ? a[1].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "summon" -> plugin.worldState().setSummoned(w, true);
            case "purify" -> plugin.worldState().setPurified(w, true);
            case "reset" -> {
                plugin.worldState().setPurified(w, false);
                plugin.worldState().setSummoned(w, false);
            }
            default -> {
            }
        }
        s.sendMessage("§7Mundo " + w.getName() + ": invocado=" + plugin.worldState().isSummoned(w)
                + ", purificado=" + plugin.worldState().isPurified(w)
                + ", ativo=" + plugin.worldState().active(w));
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
        String sub = a.length >= 2 ? a[0].toLowerCase(Locale.ROOT) : "";
        if (a.length == 1) {
            out.addAll(List.of("spawn", "event", "presence", "phase", "give", "world", "banish", "reload", "info"));
        } else if (a.length == 2 && sub.equals("spawn")) {
            Arrays.stream(Apparition.Mode.values()).forEach(m -> out.add(m.name().toLowerCase(Locale.ROOT)));
        } else if (a.length == 2 && sub.equals("event")) {
            Events.ALL.forEach(d -> out.add(d.id()));
        } else if (a.length == 2 && sub.equals("give")) {
            Arrays.stream(Items.Id.values()).forEach(i -> out.add(i.key()));
        } else if (a.length == 2 && sub.equals("world")) {
            out.addAll(List.of("status", "summon", "purify", "reset"));
        } else if (a.length == 3 && sub.equals("presence")) {
            out.add("set");
        } else if (a.length == 3 && sub.equals("phase")) {
            out.addAll(List.of("0", "1", "2", "3", "4", "add", "reset"));
        } else if (a.length == 3 && sub.equals("world")) {
            plugin.getServer().getWorlds().forEach(w -> out.add(w.getName()));
        } else {
            plugin.getServer().getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String prefix = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
