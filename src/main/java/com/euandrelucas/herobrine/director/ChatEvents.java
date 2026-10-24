package com.euandrelucas.herobrine.director;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Eventos "meta": mensagens falsas de chat/log, títulos, sons, dano aleatório, baú com recado. */
public final class ChatEvents {

    private static final Map<UUID, Long> awaitingReply = new HashMap<>();

    private static final String[] REPLIES = {
            "i see you", "you should not have come here", "behind you", "hello, " + "friend", "do not look", "leave"
    };

    private ChatEvents() {}

    private static void say(Player p, String s) {
        p.sendMessage(Util.color(s));
    }

    private static void later(HerobrinePlugin plugin, long ticks, Runnable r) {
        plugin.getServer().getScheduler().runTaskLater(plugin, r, ticks);
    }

    // ------------------------------------------------------------------ chat falso

    public static boolean fakeChat(HerobrinePlugin plugin, Player p) {
        switch (Util.rng().nextInt(4)) {
            case 0 -> say(p, "§cNo player was found");
            case 1 -> {
                List<String> errors = plugin.getConfig().getStringList("messages.fake-errors");
                if (errors.isEmpty()) return false;
                say(p, errors.get(Util.rng().nextInt(errors.size())));
            }
            case 2 -> {
                say(p, "§eHerobrine joined the game");
                later(plugin, 60 + Util.rng().nextInt(100), () -> {
                    if (p.isOnline()) say(p, "§eHerobrine left the game");
                });
            }
            default -> {
                say(p, "§cCould not find player: Herobrine");
            }
        }
        return true;
    }

    public static boolean fakeLogs(HerobrinePlugin plugin, Player p) {
        List<String> lines = plugin.getConfig().getStringList("messages.fake-logs");
        if (lines.isEmpty()) return false;
        int n = 1 + Util.rng().nextInt(3);
        for (int i = 0; i < n; i++) {
            String l = lines.get(Util.rng().nextInt(lines.size()));
            later(plugin, 8L * i, () -> {
                if (p.isOnline()) say(p, l);
            });
        }
        return true;
    }

    public static boolean glErrors(HerobrinePlugin plugin, Player p) {
        String[] lines = {"§c[Render thread/ERROR]: ########## GL ERROR ##########", "§c[Render thread/ERROR]: @ Post render",
                "§c[Render thread/ERROR]: 1282: Invalid operation"};
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            later(plugin, 4L * i, () -> {
                if (p.isOnline()) say(p, l);
            });
        }
        return true;
    }

    public static boolean fakeShutdown(HerobrinePlugin plugin, Player p) {
        say(p, "§c[Server] Shutting down internal server...");
        later(plugin, 50, () -> {
            if (p.isOnline()) say(p, "§7[Server] ...never mind.");
        });
        return true;
    }

    // ------------------------------------------------------------------ títulos

    private static void title(Player p, String main, int stayMs) {
        p.showTitle(Title.title(Util.color(main), Component.empty(),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(stayMs), Duration.ofMillis(400))));
    }

    public static boolean titleWho(HerobrinePlugin plugin, Player p) {
        title(p, "§c§lWho are you?", 2200);
        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.6f);
        return true;
    }

    public static boolean titleHide(HerobrinePlugin plugin, Player p) {
        title(p, "§4§lYou can't hide", 2200);
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
        return true;
    }

    public static boolean titleLeave(HerobrinePlugin plugin, Player p) {
        for (int i = 0; i < 4; i++) {
            later(plugin, 14L * i, () -> {
                if (p.isOnline()) {
                    title(p, "§4§lLEAVE", 500);
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
                }
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ sons e efeitos

    public static boolean creeperHiss(HerobrinePlugin plugin, Player p) {
        Location at = Util.pointAround(p.getLocation(), p.getLocation().getYaw() + 180 + Util.rng().nextDouble(-30, 30), 4);
        p.playSound(at, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
        return true;
    }

    public static boolean scream(HerobrinePlugin plugin, Player p) {
        p.playSound(Util.pointAround(p.getLocation(), Util.rng().nextDouble(360), 16), Sound.ENTITY_GHAST_SCREAM, 1.2f, 0.5f);
        return true;
    }

    public static boolean actionMusic(HerobrinePlugin plugin, Player p) {
        p.sendActionBar(Util.color("§dNow playing: C418 - 13"));
        p.playSound(p.getLocation(), Sound.MUSIC_DISC_13, 1f, 1f);
        return true;
    }

    public static boolean randomDamage(HerobrinePlugin plugin, Player p) {
        if (p.getHealth() <= 6) return false;
        p.damage(2.0);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
        return true;
    }

    public static boolean redstoneUnder(HerobrinePlugin plugin, Player p) {
        Block feet = p.getLocation().getBlock();
        Block target = feet.getRelative(Util.rng().nextInt(-1, 2), 0, Util.rng().nextInt(-1, 2));
        if (!target.getType().isAir() || !target.getRelative(0, -1, 0).isSolid()) return false;
        plugin.blockQueue().set(target, Material.REDSTONE_TORCH);
        return true;
    }

    // ------------------------------------------------------------------ baú com "Hi"

    private static final String[] HI = {
            "X.X.XXX.....",
            "XXX..X......",
            "X.X.XXX....."
    };

    public static boolean chestHi(HerobrinePlugin plugin, Player p) {
        Location spot = null;
        for (int i = 0; i < 20 && spot == null; i++) {
            Location c = Util.pointAround(p.getLocation(), p.getLocation().getYaw() + 180 + Util.rng().nextDouble(-90, 90),
                    Util.rng().nextDouble(5, 12));
            Location f = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getLocation().getBlockY());
            if (f != null && f.getBlock().getType().isAir() && Util.viewAngle(p, f) > 80) spot = f;
        }
        if (spot == null) return false;
        Block b = spot.getBlock();
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest chest) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    // desenha "HI" nos slots do baú: H (col 0-2) e I (col 4-6)
                    if (isHi(row, col)) chest.getBlockInventory().setItem(row * 9 + col, new ItemStack(Material.REDSTONE));
                }
            }
        }
        return true;
    }

    private static boolean isHi(int row, int col) {
        // H
        if (col == 0 || col == 2) return true;
        if (col == 1) return row == 1;
        // I
        if (col >= 4 && col <= 6) {
            if (col == 5) return true;
            return row == 0 || row == 2;
        }
        return false;
    }

    // ------------------------------------------------------------------ cumprimento (janela de resposta)

    public static boolean greet(HerobrinePlugin plugin, Player p) {
        say(p, "§eHerobrine joined the game");
        long deadline = System.currentTimeMillis() + 30_000;
        awaitingReply.put(p.getUniqueId(), deadline);
        later(plugin, 20 * 30, () -> {
            Long d = awaitingReply.get(p.getUniqueId());
            if (d == null || d != deadline) return;
            awaitingReply.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
            say(p, "§eHerobrine left the game");
            if (plugin.getConfig().getBoolean("fake.kick-on-ignore", false)) {
                p.kick(Util.color("§cDisconnected\n\n§7You ignored him."));
            }
        });
        return true;
    }

    /** Chamado pelo listener de chat: se o jogador estava sendo cumprimentado, o Herobrine responde. */
    public static boolean consumeReply(HerobrinePlugin plugin, Player p) {
        Long d = awaitingReply.get(p.getUniqueId());
        if (d == null || System.currentTimeMillis() > d) return false;
        awaitingReply.remove(p.getUniqueId());
        later(plugin, 30L + Util.rng().nextInt(40), () -> {
            if (!p.isOnline()) return;
            p.sendMessage(Util.color("<Herobrine> " + REPLIES[Util.rng().nextInt(REPLIES.length)]));
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.5f);
        });
        return true;
    }
}
