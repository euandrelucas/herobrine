package com.euandrelucas.herobrine.director;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O "diretor de terror": a cada poucos segundos decide, por jogador, se algo acontece
 * e o quê, de acordo com a presença e o comportamento recente do jogador.
 */
public final class Director {

    private static final class State {
        long nextDecision;
        long lastEvent;
        long joinedAt = System.currentTimeMillis();
        double recentMining; // blocos minerados no subsolo, decai a cada decisão
        long lastMinuteTick = System.currentTimeMillis();
        double minuteAccum;
    }

    private final HerobrinePlugin plugin;
    private final Map<UUID, State> states = new HashMap<>();
    private BukkitTask task;

    public Director(HerobrinePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    public void forget(Player p) {
        states.remove(p.getUniqueId());
    }

    private State state(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new State());
    }

    /** Chamado pelo listener quando o jogador minera no subsolo. */
    public void noteMining(Player p) {
        State s = state(p);
        s.recentMining++;
        s.minuteAccum++;
    }

    private boolean eligible(Player p) {
        if (p.hasPermission("herobrine.bypass")) return false;
        if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) return false;
        if (plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName())) return false;
        return plugin.worldState().active(p.getWorld());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!eligible(p)) continue;
            State s = state(p);
            accumulatePresence(p, s, now);

            if (now < s.nextDecision) continue;
            long interval = plugin.getConfig().getLong("director.tick-seconds", 20) * 1000L;
            s.nextDecision = now + interval / 2 + Util.rng().nextLong(interval);
            decide(p, s, now);
        }
    }

    /** Sobe a presença continuamente: tempo jogado + comportamento observado. */
    private void accumulatePresence(Player p, State s, long now) {
        if (now - s.lastMinuteTick < 60_000) return;
        s.lastMinuteTick = now;
        var cfg = plugin.getConfig();
        double gain = cfg.getDouble("presence.per-minute", 0.35);
        if (Util.night(p.getWorld()) && Util.indoors(p)) gain += cfg.getDouble("presence.night-indoors-per-minute", 0.5);
        gain += s.minuteAccum / 100.0 * cfg.getDouble("presence.mining-underground-per-100-blocks", 1.5);
        s.minuteAccum = 0;
        plugin.presence().add(p, gain);
    }

    private void decide(Player p, State s, long now) {
        var cfg = plugin.getConfig();
        if (now - s.joinedAt < cfg.getLong("director.grace-minutes", 10) * 60_000L) return;
        if (now - s.lastEvent < cfg.getLong("director.event-cooldown-seconds", 90) * 1000L) return;
        if (plugin.apparitions().has(p)) return;

        double presence = plugin.presence().get(p);
        double chance = cfg.getDouble("director.base-chance", 0.35) * (0.3 + presence / 100.0 * 1.2);
        s.recentMining *= 0.5;
        if (Util.rng().nextDouble() > chance) return;

        List<Events.Def> pool = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (Events.Def d : Events.ALL) {
            if (presence < d.minPresence()) continue;
            if (!cfg.getBoolean("events." + d.id() + ".enabled", true)) continue;
            double w = cfg.getDouble("events." + d.id() + ".weight", 5);
            // Aprendizado: o Herobrine repete o que reflete o hábito do jogador.
            if (d.id().equals("tunnel") && s.recentMining > 20) w *= 4;
            if (d.id().equals("window") && Util.night(p.getWorld()) && Util.indoors(p)) w *= 3;
            if (d.id().equals("tunnel") && !Util.underground(p)) w = 0;
            // Eventos "fortes" ficam mais raros que os sutis, mesmo com presença alta.
            if (d.minPresence() >= 40) w *= 0.6;
            if (w <= 0) continue;
            pool.add(d);
            weights.add(w);
            total += w;
        }

        // Tenta até 4 eventos sorteados; alguns falham por falta de lugar adequado.
        for (int attempt = 0; attempt < 4 && total > 0; attempt++) {
            double r = Util.rng().nextDouble(total);
            for (int i = 0; i < pool.size(); i++) {
                r -= weights.get(i);
                if (r <= 0) {
                    Events.Def d = pool.get(i);
                    boolean ok;
                    try {
                        ok = d.runner().run(plugin, p);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Evento " + d.id() + " falhou: " + ex);
                        ok = false;
                    }
                    plugin.debug(d.id() + (ok ? " ok" : " sem local") + " para " + p.getName() + " (presença " + (int) presence + ")");
                    if (ok) {
                        s.lastEvent = now;
                        return;
                    }
                    total -= weights.get(i);
                    pool.remove(i);
                    weights.remove(i);
                    break;
                }
            }
        }
    }
}
