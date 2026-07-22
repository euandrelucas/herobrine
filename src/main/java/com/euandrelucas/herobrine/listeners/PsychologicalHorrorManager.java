package com.euandrelucas.herobrine.listeners;

import com.euandrelucas.herobrine.config.ConfigManager;
import com.euandrelucas.herobrine.config.MessagesManager;
import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Gerencia efeitos de terror psicológico, congelamento no swarm, jumpscares e imersão.
 */
public class PsychologicalHorrorManager implements Listener {

    private final HerobrinePlugin plugin;
    private final ConfigManager configManager;
    private final MessagesManager messagesManager;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Random random = new Random();

    public PsychologicalHorrorManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    public boolean isPlayerFrozen(Player player) {
        return player != null && frozenPlayers.contains(player.getUniqueId());
    }

    /**
     * 6.1: Dispara um Jumpscare dinâmico.
     */
    public void triggerJumpscare(Player player) {
        if (player == null || !player.isOnline()) return;

        int variation = random.nextInt(4);

        switch (variation) {
            case 0:
                spawnFaceToFaceJumpscare(player);
                break;
            case 1:
                triggerDisarmJumpscare(player);
                break;
            case 2:
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0.5f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                player.sendTitle("§0§l[FATAL ERROR]", "§cConnection Lost - Entity 303/Herobrine", 0, 50, 10);
                break;
            default:
                player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.5f);
                player.sendTitle("§c§lHEROBRINE", "§8ESTOU TE OBSERVANDO", 5, 40, 5);
                break;
        }
    }

    private void spawnFaceToFaceJumpscare(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Location spawnLoc = eyeLoc.clone().add(dir.multiply(1.2)).subtract(0, 1.2, 0);
        spawnLoc.setDirection(dir.multiply(-1));

        World world = player.getWorld();
        Zombie herobrine = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        herobrine.setCustomName("§cHerobrine");
        herobrine.setCustomNameVisible(true);
        herobrine.setBaby(false);
        herobrine.setAI(false);
        herobrine.setGlowing(true);

        ItemStack steveHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) steveHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
            steveHead.setItemMeta(meta);
        }
        if (herobrine.getEquipment() != null) {
            herobrine.getEquipment().setHelmet(steveHead);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
        player.sendTitle("§c§l", "§cPARE DE OLHAR", 0, 30, 5);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (herobrine.isValid()) {
                world.spawnParticle(Particle.FLAME, herobrine.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
                herobrine.remove();
            }
        }, 30L);
    }

    public void triggerDisarmJumpscare(Player player) {
        if (player == null || !player.isOnline()) return;

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        player.sendTitle("§c§lHEROBRINE", "§cSEU INVENTÁRIO É MEU", 5, 30, 5);

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        player.sendMessage("§c§lO susto foi tão violento que você derrubou TODO o seu inventário no chão!");
    }

    /**
     * Evento Clássico: Manada de Mobs Encarando com CONGELAMENTO TOTAL DO JOGADOR.
     */
    public void triggerStaringMobSwarm(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        frozenPlayers.add(uuid);

        // Aplica lentidão extrema (congelamento por medo)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 255));
        player.sendTitle("§c§lPARALISADO", "§8O medo congela seus movimentos...", 5, 60, 5);

        Location pLoc = player.getLocation();
        List<LivingEntity> spawnedMobs = new ArrayList<>();
        EntityType[] types = {EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN};

        int count = 6;
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = pLoc.getX() + 4 * Math.cos(angle);
            double z = pLoc.getZ() + 4 * Math.sin(angle);
            Location spawnLoc = new Location(pLoc.getWorld(), x, pLoc.getY(), z);

            EntityType selectedType = types[random.nextInt(types.length)];
            LivingEntity mob = (LivingEntity) pLoc.getWorld().spawnEntity(spawnLoc, selectedType);
            mob.setCustomName("§c...");
            mob.setCustomNameVisible(true);
            mob.setGlowing(true);
            mob.setAI(false);
            spawnedMobs.add(mob);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;
                if (ticks >= 300 || !player.isOnline()) { // 15s
                    frozenPlayers.remove(uuid);
                    if (player.isOnline()) {
                        player.removePotionEffect(PotionEffectType.SLOW);
                    }
                    for (LivingEntity mob : spawnedMobs) {
                        if (mob != null && mob.isValid()) {
                            mob.getWorld().spawnParticle(Particle.SMOKE_LARGE, mob.getLocation(), 15, 0.2, 0.5, 0.2, 0.05);
                            mob.remove();
                        }
                    }
                    cancel();
                    return;
                }

                for (LivingEntity mob : spawnedMobs) {
                    if (mob != null && mob.isValid()) {
                        Location mobLoc = mob.getLocation();
                        Vector dir = player.getEyeLocation().toVector().subtract(mobLoc.toVector()).normalize();
                        mobLoc.setDirection(dir);
                        mob.teleport(mobLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 5L);
    }

    public void triggerHauntedJukebox(Player player) {
        if (player == null || !player.isOnline()) return;

        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) return;

        for (int x = -10; x <= 10; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -10; z <= 10; z++) {
                    Block block = world.getBlockAt(pLoc.clone().add(x, y, z));
                    if (block.getType() == Material.JUKEBOX) {
                        Jukebox jukebox = (Jukebox) block.getState();
                        jukebox.setPlaying(Material.MUSIC_DISC_11);
                        jukebox.update();
                        player.playSound(block.getLocation(), Sound.MUSIC_DISC_11, 1.5f, 0.5f);
                        player.sendMessage("§cUm toca-discos próximo começa a tocar uma melodia macabra...");
                        return;
                    }
                }
            }
        }
    }

    public void sendCrypticChatMessage(Player player, String message) {
        if (player == null || !player.isOnline()) return;

        String formatted = message;
        if (plugin.getConfig().getBoolean("horror.backwards-messages", true)) {
            formatted = new StringBuilder(message).reverse().toString();
        }
        player.sendMessage("§c" + formatted);
    }

    public void sendFakeJoinMessage(Player player) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", "Herobrine");
        String msg = messagesManager.getFormattedMessage("horror.eerie-entrance", ph);
        player.sendMessage(msg);
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!plugin.getConfig().getBoolean("horror.nightmares.enabled", true)) return;

        Player player = event.getPlayer();
        double chance = plugin.getConfig().getDouble("horror.nightmares.chance", 0.15);

        if (Math.random() < chance) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 140, 0));
            player.sendMessage(messagesManager.getFormattedMessage("horror.nightmare", new HashMap<>()));
        }
    }
}
