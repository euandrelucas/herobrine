package com.euandrelucas.herobrine.apparition;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.item.Items;
import com.euandrelucas.herobrine.util.Util;
import com.euandrelucas.herobrine.world.Placement;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Uma aparição do Herobrine. Visível somente para o jogador alvo (exceto o BOSS, visível para todos).
 * O Mannequin se move por teleporte; toda a IA fica aqui.
 */
public final class Apparition {

    public enum Mode {
        /** Espião: observa de longe e some quando olhado ou abordado. */
        STARE,
        /** Observa por trás de uma janela. */
        WINDOW,
        /** Observa flutuando no ar. */
        SKY,
        /** Stalker: some quando olhado e reaparece mais perto. */
        STALK,
        /** Stalker que corre até você e some, deixando um susto. */
        CHASE,
        /** Só se mexe quando ninguém olha. */
        WEEPING,
        /** Vários Herobrines em círculo, que se fecha aos poucos. */
        CIRCLE,
        /** Aparece de repente (frente/atrás) e some quando visto. */
        FLASH,
        /** Corre e lança o jogador longe com um graveto. */
        KNOCKBACK,
        /** Guerreiro/Caçador: combate corpo a corpo, quebra blocos. */
        HOSTILE,
        /** Mago: ilusões, raios e teleporte. */
        MAGE,
        /** Construtor: constrói uma estrutura bloco a bloco. */
        BUILDER,
        /** Chefe invocado no altar. */
        BOSS
    }

    private final HerobrinePlugin plugin;
    private final Player owner;
    private final Mode mode;
    private Mannequin body;
    private final List<Mannequin> extras = new ArrayList<>();
    private final List<Zombie> minions = new ArrayList<>();
    private int age;
    private int lookTicks;
    private boolean everSeen;
    private Player focus;
    // STALK
    private int sightings;
    private int respawnAt = -1;
    private int lastMove;
    // movimento
    private int stuck;
    private int attackCooldown;
    // vida (HOSTILE / MAGE / BOSS)
    private double health;
    private double maxHealth;
    private boolean hunter;
    // MAGE / BOSS
    private int abilityCd = 100;
    private int illusionCd = 200;
    private int minionCd = 400;
    private int phase = 1;
    private int dodgeCd;
    private BossBar bar;
    private boolean defeated;
    // CIRCLE
    private Location circleCenter;
    private double circleRadius = 12;
    // BUILDER
    private Deque<Placement> placements = new ArrayDeque<>();

    public Apparition(HerobrinePlugin plugin, Player owner, Mode mode, Location at) {
        this.plugin = plugin;
        this.owner = owner;
        this.mode = mode;
        this.focus = owner;
        var cfg = plugin.getConfig();
        switch (mode) {
            case BOSS -> maxHealth = cfg.getDouble("boss.health", 300);
            case MAGE -> maxHealth = cfg.getDouble("mage.health", 60);
            default -> maxHealth = cfg.getDouble("hostile.health", 200);
        }
        this.health = maxHealth;
        this.hunter = mode == Mode.HOSTILE && Util.rng().nextBoolean();

        if (mode == Mode.CIRCLE) {
            circleCenter = owner.getLocation().clone();
            int n = 7;
            for (int i = 0; i < n; i++) {
                Location l = ringSpot(i, n);
                Mannequin m = spawnBody(l != null ? l : at);
                if (i == 0) body = m;
                else extras.add(m);
            }
        } else {
            Location spawnAt = mode == Mode.SKY ? at.clone().add(0, 8, 0) : at;
            this.body = spawnBody(spawnAt);
        }
        if (mode == Mode.BOSS) {
            bar = BossBar.bossBar(Util.color("§4§lHEROBRINE"), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
            at.getWorld().getPlayers().forEach(p -> {
                if (p.getLocation().distance(at) < 120) p.showBossBar(bar);
            });
        }
    }

    public Mode mode() { return mode; }
    public Player owner() { return owner; }
    public Mannequin body() { return body; }

    public void setPlacements(List<Placement> list) {
        this.placements = new ArrayDeque<>(list);
    }

    public boolean owns(Entity e) {
        if (body != null && body.equals(e)) return true;
        return extras.contains(e);
    }

    // ------------------------------------------------------------------ corpo

    private Mannequin spawnBody(Location at) {
        boolean shared = mode == Mode.BOSS;
        Mannequin m = at.getWorld().spawn(at, Mannequin.class, e -> {
            e.setVisibleByDefault(shared);
            e.setPersistent(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setGravity(false);
            e.setCollidable(false);
            e.setImmovable(true);
            e.setDescription(null);
            e.setProfile(buildProfile());
            equip(e);
        });
        if (!shared) owner.showEntity(plugin, m);
        face(m);
        return m;
    }

    private void equip(Mannequin m) {
        var eq = m.getEquipment();
        switch (mode) {
            case HOSTILE -> {
                if (hunter) {
                    eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                    eq.setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
                } else {
                    eq.setItemInMainHand(plugin.items().create(Items.Id.BEDROCK_SWORD, 1));
                }
            }
            case BOSS -> eq.setItemInMainHand(plugin.items().create(Items.Id.BEDROCK_SWORD, 1));
            case BUILDER -> eq.setItemInMainHand(new ItemStack(Material.GOLDEN_PICKAXE));
            case KNOCKBACK -> eq.setItemInMainHand(new ItemStack(Material.STICK));
            default -> {
            }
        }
    }

    private ResolvableProfile buildProfile() {
        String value = plugin.getConfig().getString("skin.texture-value", "");
        if (value != null && !value.isBlank()) {
            PlayerProfile pp = Bukkit.createProfile(UUID.randomUUID(), "Herobrine");
            String sig = plugin.getConfig().getString("skin.texture-signature", "");
            pp.setProperty(sig == null || sig.isBlank()
                    ? new ProfileProperty("textures", value)
                    : new ProfileProperty("textures", value, sig));
            return ResolvableProfile.resolvableProfile(pp);
        }
        return ResolvableProfile.resolvableProfile()
                .name(plugin.getConfig().getString("skin.name", "Herobrine"))
                .build();
    }

    private void face(Mannequin m) {
        face(m, focus.getEyeLocation().toVector());
    }

    private void face(Mannequin m, Vector target) {
        Location l = m.getLocation();
        Vector to = target.clone().subtract(l.toVector().add(new Vector(0, 1.6, 0)));
        if (to.lengthSquared() < 0.01) return;
        l.setDirection(to);
        m.setRotation(l.getYaw(), l.getPitch());
    }

    private Location head() {
        return body.getLocation().add(0, 1.6, 0);
    }

    // ------------------------------------------------------------------ tick

    /** @return false quando a aparição terminou. */
    public boolean tick() {
        age++;
        if (mode == Mode.BOSS) {
            if (!updateBossFocus()) return false;
        } else {
            if (!owner.isOnline() || owner.isDead() || owner.getGameMode() == GameMode.SPECTATOR) return false;
        }
        if (body != null && (!body.isValid() || body.getWorld() != focus.getWorld())) return false;

        return switch (mode) {
            case STARE -> tickStare(25, 9, 700, true);
            case WINDOW -> tickStare(70, 3, 500, false);
            case SKY -> tickStare(20, 8, 600, false);
            case STALK -> tickStalk();
            case CHASE -> tickChase();
            case WEEPING -> tickWeeping();
            case CIRCLE -> tickCircle();
            case FLASH -> tickFlash();
            case KNOCKBACK -> tickKnockback();
            case HOSTILE -> tickHostile();
            case MAGE -> tickMage();
            case BUILDER -> tickBuilder();
            case BOSS -> tickBoss();
        };
    }

    private boolean updateLooking() {
        boolean looked = Util.isLookingAt(owner, head(), 14);
        lookTicks = looked ? lookTicks + 1 : Math.max(0, lookTicks - 1);
        if (looked && !everSeen) {
            everSeen = true;
            plugin.presence().add(owner, plugin.getConfig().getDouble("presence.seen-bonus", 1.0));
        }
        return looked;
    }

    private boolean tickStare(int vanishAfterLook, double minDist, int maxAge, boolean dropsShield) {
        face(body);
        updateLooking();
        double dist = body.getLocation().distance(owner.getLocation());
        if (lookTicks > vanishAfterLook || dist < minDist || age > maxAge) {
            if (dropsShield && lookTicks > vanishAfterLook) maybeDropShield();
            vanish(false);
            return false;
        }
        return true;
    }

    private void maybeDropShield() {
        if (Util.rng().nextDouble() < 0.2 && body != null) {
            body.getWorld().dropItemNaturally(body.getLocation(), new ItemStack(Material.SHIELD));
        }
    }

    private boolean tickStalk() {
        if (age > 2400 || sightings >= 4) {
            vanish(false);
            return false;
        }
        if (body == null) {
            if (age >= respawnAt) {
                Location l = hiddenSpot(12, 22);
                if (l != null) body = spawnBody(l);
                else respawnAt = age + 40;
            }
            return true;
        }
        face(body);
        updateLooking();
        if (lookTicks > 22) {
            sightings++;
            maybeDropShield();
            vanish(false);
            body = null;
            lookTicks = 0;
            respawnAt = age + 80 + Util.rng().nextInt(160);
            return true;
        }
        if (age - lastMove > 100 && lookTicks == 0 && Util.viewAngle(owner, head()) > 70) {
            Location l = hiddenSpot(9, 16);
            if (l != null) {
                body.teleport(l);
                lastMove = age;
            }
        }
        return true;
    }

    private boolean tickChase() {
        face(body);
        if (age < 50) {
            updateLooking();
            return true;
        }
        if (age > 700) {
            vanish(true);
            return false;
        }
        double dist = body.getLocation().distance(owner.getLocation());
        if (dist < 2.3) {
            if (plugin.getConfig().getBoolean("jumpscares", true)) Apparitions.jumpscare(plugin, owner);
            else owner.playSound(owner.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.6f);
            vanish(false);
            return false;
        }
        if (!stepTowards(owner, 0.55, false)) unstick(owner);
        else stuck = 0;
        return true;
    }

    private boolean tickKnockback() {
        face(body);
        if (age < 30) return true;
        if (age > 600) {
            vanish(true);
            return false;
        }
        double dist = body.getLocation().distance(owner.getLocation());
        if (dist < 2.4) {
            Vector away = owner.getLocation().toVector().subtract(body.getLocation().toVector()).setY(0);
            if (away.lengthSquared() == 0) away = new Vector(1, 0, 0);
            owner.setVelocity(away.normalize().multiply(2.2).setY(0.9));
            owner.damage(2.0, body);
            body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.4f, 0.5f);
            vanish(true);
            return false;
        }
        if (!stepTowards(owner, 0.5, false)) unstick(owner);
        else stuck = 0;
        return true;
    }

    /** Weeping: só se move quando você NÃO está olhando (campo de visão amplo). */
    private boolean tickWeeping() {
        face(body);
        boolean seen = Util.isLookingAt(owner, head(), 55);
        if (seen && !everSeen) {
            everSeen = true;
            plugin.presence().add(owner, plugin.getConfig().getDouble("presence.seen-bonus", 1.0));
        }
        if (age > 1800) {
            vanish(false);
            return false;
        }
        double dist = body.getLocation().distance(owner.getLocation());
        if (dist < 2.5) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
            owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            if (plugin.getConfig().getBoolean("jumpscares", true)) Apparitions.jumpscare(plugin, owner);
            vanish(false);
            return false;
        }
        if (!seen) {
            if (!stepTowards(owner, 0.38, false) && ++stuck > 40) {
                stuck = 0;
                unstick(owner);
            }
        }
        return true;
    }

    private Location ringSpot(int i, int n) {
        double yaw = 360.0 / n * i;
        Location c = Util.pointAround(circleCenter, yaw, circleRadius);
        return Util.findFloor(circleCenter.getWorld(), c.getBlockX(), c.getBlockZ(), circleCenter.getBlockY());
    }

    private boolean tickCircle() {
        List<Mannequin> all = new ArrayList<>(extras);
        all.add(0, body);
        boolean looked = false;
        for (Mannequin m : all) {
            if (!m.isValid()) continue;
            face(m);
            if (Util.isLookingAt(owner, m.getLocation().add(0, 1.6, 0), 14)) looked = true;
        }
        lookTicks = looked ? lookTicks + 1 : Math.max(0, lookTicks - 1);
        if (age % 4 == 0) {
            circleRadius = Math.max(5.5, circleRadius - 0.08);
            for (int i = 0; i < all.size(); i++) {
                Location l = ringSpot(i, all.size());
                if (l != null) all.get(i).teleport(l);
            }
        }
        if (lookTicks > 40 || age > 360) {
            vanishAll(true);
            return false;
        }
        return true;
    }

    private boolean tickFlash() {
        face(body);
        updateLooking();
        if (lookTicks >= 3) {
            if (plugin.getConfig().getBoolean("jumpscares", true) && Util.rng().nextDouble() < 0.35) {
                Apparitions.jumpscare(plugin, owner);
            } else {
                owner.playSound(owner.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
            }
            vanish(false);
            return false;
        }
        if (age > 160) {
            vanish(false);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ combate

    private boolean tickHostile() {
        face(body);
        var cfg = plugin.getConfig();
        double dist = body.getLocation().distance(owner.getLocation());
        if (age > cfg.getInt("hostile.duration-seconds", 45) * 20 || health <= maxHealth * 0.15) {
            owner.sendMessage(Util.color("§c§o...eu volto."));
            vanish(true);
            return false;
        }
        attackCooldown = Math.max(0, attackCooldown - 1);
        if (dist < 2.4) {
            if (attackCooldown == 0) {
                attackCooldown = 20;
                double dmg = cfg.getDouble("hostile.damage", 5.0) * (hunter ? 1.6 : 1.0);
                owner.damage(dmg, body);
                knock(owner, 0.5, 0.25);
                body.swingMainHand();
                body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.6f);
            }
        } else if (!stepTowards(owner, hunter ? 0.34 : 0.3, true) && ++stuck > 20) {
            stuck = 0;
            unstick(owner);
        }
        return true;
    }

    private void knock(Player p, double strength, double up) {
        Vector kb = p.getLocation().toVector().subtract(body.getLocation().toVector()).setY(0);
        if (kb.lengthSquared() > 0) p.setVelocity(kb.normalize().multiply(strength).setY(up));
    }

    private boolean tickMage() {
        face(body);
        if (age > 900 || health <= 0) {
            owner.sendMessage(Util.color("§5§o...só uma ilusão."));
            vanishAll(true);
            return false;
        }
        body.getWorld().spawnParticle(Particle.PORTAL, body.getLocation().add(0, 1, 0), 6, 0.4, 0.8, 0.4, 0.2);
        abilityCd--;
        if (abilityCd <= 0) {
            abilityCd = 90 + Util.rng().nextInt(60);
            switch (Util.rng().nextInt(3)) {
                case 0 -> {
                    Location l = hiddenSpot(5, 9);
                    if (l != null) {
                        body.getWorld().spawnParticle(Particle.PORTAL, body.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.4);
                        body.teleport(l);
                        body.getWorld().playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.6f);
                    }
                }
                case 1 -> telegraphStrike(owner.getLocation(), 4.0);
                default -> {
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0));
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    owner.playSound(owner.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
                }
            }
        }
        illusionCd--;
        if (illusionCd <= 0 && extras.isEmpty()) {
            illusionCd = 300;
            for (int i = 0; i < 3; i++) {
                Location l = hiddenSpot(6, 11);
                if (l != null) extras.add(spawnBody(l));
            }
        }
        for (Mannequin m : new ArrayList<>(extras)) {
            if (!m.isValid()) {
                extras.remove(m);
                continue;
            }
            face(m);
        }
        if (age % 160 == 0) extras.removeIf(m -> {
            if (m.isValid()) m.remove();
            return true;
        });
        return true;
    }

    /** Aviso no chão e, 1 segundo depois, um raio que machuca quem estiver embaixo. */
    private void telegraphStrike(Location at, double damage) {
        Location pos = at.clone();
        int[] t = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            t[0] += 2;
            pos.getWorld().spawnParticle(Particle.DUST, pos.clone().add(0, 0.2, 0), 16, 1.2, 0.05, 1.2,
                    new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f));
            if (t[0] >= 20) {
                task.cancel();
                pos.getWorld().strikeLightningEffect(pos);
                for (Player p : pos.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(pos) <= 9 && p.getGameMode() == GameMode.SURVIVAL
                            || p.getLocation().distanceSquared(pos) <= 9 && p.getGameMode() == GameMode.ADVENTURE) {
                        p.damage(damage, body);
                    }
                }
            }
        }, 0L, 2L);
    }

    // ------------------------------------------------------------------ builder

    private boolean tickBuilder() {
        face(body, placements.isEmpty() ? owner.getEyeLocation().toVector()
                : placements.peek().block().getLocation().add(0.5, 0.5, 0.5).toVector());
        updateLooking();
        if (placements.isEmpty() || age > 6000) {
            vanish(false);
            return false;
        }
        if (body.getLocation().distance(owner.getLocation()) < 9) {
            vanish(false);
            return false;
        }
        if (age % 3 == 0) {
            Placement p = placements.poll();
            if (p != null && p.block().getWorld().isChunkLoaded(p.block().getX() >> 4, p.block().getZ() >> 4)) {
                p.block().setBlockData(p.data(), false);
                if (p.fake()) plugin.fakeBlocks().register(p.block(), p.data().getMaterial());
                if (p.post() != null) p.post().accept(p.block());
                body.swingMainHand();
                body.getWorld().playSound(p.block().getLocation(), Sound.BLOCK_STONE_PLACE, 0.8f, 0.8f);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ boss

    private boolean updateBossFocus() {
        Player best = null;
        double bd = Double.MAX_VALUE;
        Location from = body != null && body.isValid() ? body.getLocation() : owner.getLocation();
        for (Player p : from.getWorld().getPlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(from);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        if (best == null || bd > 80 * 80) return false;
        focus = best;
        // mantém a barra de vida nos jogadores próximos
        if (age % 40 == 0) {
            for (Player p : from.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(from) < 120 * 120) p.showBossBar(bar);
                else p.hideBossBar(bar);
            }
        }
        bar.progress((float) Math.max(0, Math.min(1, health / maxHealth)));
        return true;
    }

    private boolean tickBoss() {
        if (defeated) return false;
        var cfg = plugin.getConfig();
        face(body);
        Location bl = body.getLocation();
        double dist = bl.distance(focus.getLocation());
        attackCooldown = Math.max(0, attackCooldown - 1);
        dodgeCd = Math.max(0, dodgeCd - 1);

        int newPhase = health > maxHealth * 0.66 ? 1 : health > maxHealth * 0.33 ? 2 : 3;
        if (newPhase != phase) {
            phase = newPhase;
            for (Player p : bl.getWorld().getPlayers()) {
                p.playSound(bl, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.7f);
                p.sendMessage(Util.color(phase == 2 ? "§4§oVocê não faz ideia do que eu posso fazer."
                        : "§4§oCHEGA."));
            }
        }

        if (dist < 2.6) {
            if (attackCooldown == 0) {
                attackCooldown = phase == 3 ? 14 : 20;
                focus.damage(cfg.getDouble("boss.damage", 8.0), body);
                knock(focus, 0.6, 0.3);
                body.swingMainHand();
                bl.getWorld().playSound(bl, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.2f, 0.5f);
            }
        } else {
            double speed = phase == 3 ? 0.42 : 0.34;
            if (!stepTowards(focus, speed, true) && ++stuck > 15) {
                stuck = 0;
                unstick(focus);
            }
        }

        bl.getWorld().spawnParticle(Particle.SMOKE, bl.clone().add(0, 1, 0), 2, 0.3, 0.8, 0.3, 0.01);

        if (phase >= 2) {
            abilityCd--;
            if (abilityCd <= 0) {
                abilityCd = phase == 3 ? 70 : 110;
                telegraphStrike(focus.getLocation(), 6.0);
                if (phase == 3) telegraphStrike(focus.getLocation().add(Util.rng().nextDouble(-4, 4), 0, Util.rng().nextDouble(-4, 4)), 6.0);
            }
            illusionCd--;
            if (illusionCd <= 0) {
                illusionCd = 360;
                for (int i = 0; i < 2; i++) {
                    Location l = Util.pointAround(focus.getLocation(), Util.rng().nextDouble(360), 7);
                    Location f = Util.findFloor(l.getWorld(), l.getBlockX(), l.getBlockZ(), focus.getLocation().getBlockY());
                    if (f != null) extras.add(spawnBody(f));
                }
            }
        }
        if (phase == 3) {
            minionCd--;
            if (minionCd <= 0 && minions.size() < 6) {
                minionCd = 500;
                for (int i = 0; i < 3; i++) {
                    Location l = Util.pointAround(focus.getLocation(), Util.rng().nextDouble(360), 6);
                    Location f = Util.findFloor(l.getWorld(), l.getBlockX(), l.getBlockZ(), focus.getLocation().getBlockY());
                    if (f == null) continue;
                    Zombie z = f.getWorld().spawn(f, Zombie.class, e -> {
                        e.customName(Util.color("§8Servo"));
                        e.setShouldBurnInDay(false);
                        e.setPersistent(false);
                        e.setTarget(focus);
                    });
                    minions.add(z);
                }
            }
        }
        if (cfg.getBoolean("boss.tnt", false) && phase == 3 && age % 160 == 0) {
            Location l = focus.getLocation().add(Util.rng().nextDouble(-3, 3), 1, Util.rng().nextDouble(-3, 3));
            l.getWorld().spawn(l, org.bukkit.entity.TNTPrimed.class, t -> t.setFuseTicks(50));
        }
        tickClones(focus);
        return true;
    }

    /** Ilusões do boss correm até o alvo e explodem em fumaça causando dano leve. */
    private void tickClones(Player target) {
        for (Mannequin m : new ArrayList<>(extras)) {
            if (!m.isValid()) {
                extras.remove(m);
                continue;
            }
            face(m, target.getEyeLocation().toVector());
            Vector dir = target.getLocation().toVector().subtract(m.getLocation().toVector()).setY(0);
            if (dir.lengthSquared() < 2.5) {
                target.damage(3.0, m);
                m.getWorld().spawnParticle(Particle.LARGE_SMOKE, m.getLocation().add(0, 1, 0), 25, 0.3, 0.7, 0.3, 0.02);
                m.remove();
                extras.remove(m);
                continue;
            }
            Location next = m.getLocation().add(dir.normalize().multiply(0.28));
            Location f = Util.findFloor(next.getWorld(), next.getBlockX(), next.getBlockZ(), m.getLocation().getBlockY());
            if (f != null && Math.abs(f.getY() - m.getLocation().getY()) <= 1.01) {
                next.setY(f.getY());
                m.teleport(next);
            }
        }
    }

    private void defeat() {
        defeated = true;
        Location at = body.getLocation();
        World w = at.getWorld();
        w.strikeLightningEffect(at);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, at.clone().add(0, 1, 0), 120, 0.5, 1, 0.5, 0.5);
        Items items = plugin.items();
        w.dropItemNaturally(at, items.create(Items.Id.CURSED_DUST, 4 + Util.rng().nextInt(5)));
        w.dropItemNaturally(at, items.create(Items.Id.CURSED_DIAMOND, 1 + Util.rng().nextInt(2)));
        w.dropItemNaturally(at, items.create(Items.Id.HEROBRINES_HEART, 1));
        if (Util.rng().nextDouble() < 0.10) w.dropItemNaturally(at, items.create(Items.Id.BEDROCK_SWORD, 1));
        w.spawn(at, org.bukkit.entity.ExperienceOrb.class, o -> o.setExperience(1200));
        for (Player p : w.getPlayers()) {
            p.sendMessage(Util.color("§a§lHerobrine foi derrotado."));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if (p.getLocation().distanceSquared(at) < 100 * 100) plugin.presence().set(p, 0);
        }
    }

    // ------------------------------------------------------------------ movimento

    private void unstick(Player target) {
        Location l = Util.pointAround(target.getLocation(),
                target.getLocation().getYaw() + 180 + Util.rng().nextInt(-60, 60), 7);
        Location floor = Util.findFloor(l.getWorld(), l.getBlockX(), l.getBlockZ(), target.getLocation().getBlockY());
        if (floor != null) body.teleport(floor);
    }

    /** Anda uma fração de bloco na direção do alvo, grudado no chão. Pode quebrar blocos no caminho. */
    private boolean stepTowards(Player target, double speed, boolean canBreak) {
        Location cur = body.getLocation();
        Vector dir = target.getLocation().toVector().subtract(cur.toVector()).setY(0);
        if (dir.lengthSquared() < 0.01) return true;
        dir.normalize().multiply(speed);
        Location next = cur.clone().add(dir);
        Location floor = Util.findFloor(next.getWorld(), next.getBlockX(), next.getBlockZ(), cur.getBlockY());
        if (floor == null || Math.abs(floor.getY() - cur.getY()) > 1.01) {
            if (canBreak && plugin.getConfig().getBoolean("hostile.break-blocks", true)) breakAhead(cur, dir);
            return false;
        }
        next.setY(floor.getY());
        body.teleport(next);
        return true;
    }

    private void breakAhead(Location cur, Vector dir) {
        Location ahead = cur.clone().add(dir.clone().normalize().multiply(1.2));
        for (int dy = 0; dy < 2; dy++) {
            Block b = ahead.getBlock().getRelative(0, dy, 0);
            Material t = b.getType();
            if (!t.isAir() && !b.isLiquid() && t.getHardness() >= 0 && t.getHardness() < 50
                    && !plugin.fakeBlocks().isFake(b)) {
                b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, b.getBlockData());
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.7f);
                b.setType(Material.AIR);
            }
        }
    }

    /** Ponto fora do campo de visão do jogador, de preferência atrás dele. */
    private Location hiddenSpot(double min, double max) {
        Location p = owner.getLocation();
        for (int i = 0; i < 25; i++) {
            double yaw = p.getYaw() + 180 + Util.rng().nextDouble(-90, 90);
            Location c = Util.pointAround(p, yaw, Util.rng().nextDouble(min, max));
            Location floor = Util.findFloor(p.getWorld(), c.getBlockX(), c.getBlockZ(), p.getBlockY());
            if (floor != null && Util.viewAngle(owner, floor.clone().add(0, 1.6, 0)) > 75) return floor;
        }
        return null;
    }

    // ------------------------------------------------------------------ dano recebido

    /** Um jogador bateu em {@code hit} (corpo principal ou uma ilusão). */
    public void onHit(Entity hit, Player attacker, double damage) {
        if (extras.contains(hit)) {
            extras.remove(hit);
            if (hit.isValid()) {
                hit.getWorld().spawnParticle(Particle.LARGE_SMOKE, hit.getLocation().add(0, 1, 0), 25, 0.3, 0.7, 0.3, 0.02);
                hit.remove();
            }
            return;
        }
        switch (mode) {
            case HOSTILE, MAGE, BOSS -> {
                if (mode == Mode.BOSS) {
                    health -= damage * 0.8;
                    if (health <= 0 && !defeated) {
                        defeat();
                        return;
                    }
                    if (dodgeCd == 0 && Util.rng().nextDouble() < 0.25) {
                        dodgeCd = 40;
                        Location l = Util.pointAround(attacker.getLocation(), attacker.getLocation().getYaw() + 180 + Util.rng().nextInt(-40, 40), 4);
                        Location f = Util.findFloor(l.getWorld(), l.getBlockX(), l.getBlockZ(), attacker.getLocation().getBlockY());
                        if (f != null) {
                            body.getWorld().spawnParticle(Particle.PORTAL, body.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.4);
                            body.teleport(f);
                            body.getWorld().playSound(f, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
                        }
                    }
                } else {
                    health -= damage;
                }
                body.getWorld().spawnParticle(Particle.CRIT, body.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
            }
            default -> age = Integer.MAX_VALUE / 2; // some ao ser tocado
        }
    }

    /** Água Benta atingiu a aparição. */
    public void onHolyWater() {
        if (mode == Mode.BOSS || mode == Mode.HOSTILE || mode == Mode.MAGE) {
            health -= mode == Mode.BOSS ? 20 : 40;
            body.getWorld().spawnParticle(Particle.END_ROD, body.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
            body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
            if (mode == Mode.BOSS && health <= 0 && !defeated) defeat();
        }
        for (Mannequin m : new ArrayList<>(extras)) {
            if (m.isValid()) {
                m.getWorld().spawnParticle(Particle.END_ROD, m.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.03);
                m.remove();
            }
        }
        extras.clear();
    }

    // ------------------------------------------------------------------ fim

    public void vanish(boolean smoke) {
        if (body != null && body.isValid()) {
            if (smoke) {
                body.getWorld().spawnParticle(Particle.LARGE_SMOKE, body.getLocation().add(0, 1, 0), 25, 0.3, 0.7, 0.3, 0.02);
            }
            body.remove();
        }
    }

    private void vanishAll(boolean smoke) {
        vanish(smoke);
        for (Mannequin m : extras) {
            if (m.isValid()) {
                if (smoke) m.getWorld().spawnParticle(Particle.LARGE_SMOKE, m.getLocation().add(0, 1, 0), 15, 0.3, 0.7, 0.3, 0.02);
                m.remove();
            }
        }
        extras.clear();
    }

    public void removeNow() {
        vanishAll(false);
        for (LivingEntity z : minions) if (z.isValid()) z.remove();
        minions.clear();
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
        }
    }
}
