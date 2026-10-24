package com.euandrelucas.herobrine.director;

import com.euandrelucas.herobrine.HerobrinePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** "Presença" oculta (0-100) de cada jogador, guardada no próprio jogador (PDC). */
public final class Presence {

    private final HerobrinePlugin plugin;
    private final NamespacedKey level;
    private final NamespacedKey lastSeen;

    public Presence(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.level = new NamespacedKey(plugin, "presence");
        this.lastSeen = new NamespacedKey(plugin, "last_seen");
    }

    public double get(Player p) {
        return p.getPersistentDataContainer().getOrDefault(level, PersistentDataType.DOUBLE, 0.0);
    }

    public void set(Player p, double v) {
        p.getPersistentDataContainer().set(level, PersistentDataType.DOUBLE, Math.max(0, Math.min(100, v)));
    }

    public void add(Player p, double v) {
        set(p, get(p) + v);
    }

    /** Ao entrar: quem ficou muito tempo fora esfria um pouco. */
    public void onJoin(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Long last = pdc.get(lastSeen, PersistentDataType.LONG);
        if (last != null) {
            double hours = (System.currentTimeMillis() - last) / 3_600_000.0;
            add(p, -hours * plugin.getConfig().getDouble("presence.offline-decay-per-hour", 1.0));
        }
    }

    public void onQuit(Player p) {
        p.getPersistentDataContainer().set(lastSeen, PersistentDataType.LONG, System.currentTimeMillis());
    }

    /** 0 = nada, 1 = ruídos, 2 = mundo alterado, 3 = perseguição, 4 = encontros diretos. */
    public static int stage(double presence) {
        if (presence < 15) return 0;
        if (presence < 35) return 1;
        if (presence < 55) return 2;
        if (presence < 75) return 3;
        return 4;
    }
}
