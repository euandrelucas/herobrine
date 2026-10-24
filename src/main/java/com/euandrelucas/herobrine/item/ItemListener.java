package com.euandrelucas.herobrine.item;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.apparition.Apparition;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Habilidades dos itens do Herobrine: ferramentas, armadura, águas, coração. */
public final class ItemListener implements Listener {

    private final HerobrinePlugin plugin;
    private final Items items;
    private final NamespacedKey chargeKey;
    private final NamespacedKey heartKey;
    private final Map<Material, ItemStack> smelt = new HashMap<>();

    public ItemListener(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
        this.chargeKey = new NamespacedKey(plugin, "charge");
        this.heartKey = new NamespacedKey(plugin, "heart_bonus");
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (it.next() instanceof FurnaceRecipe fr) {
                var in = fr.getInputChoice();
                if (in instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice mc) {
                    for (Material m : mc.getChoices()) smelt.putIfAbsent(m, fr.getResult());
                }
            }
        }
        // Efeitos contínuos da armadura amaldiçoada
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::armorTick, 40L, 40L);
    }

    // ------------------------------------------------------------------ armadura

    private void armorTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            int pieces = 0;
            var inv = p.getInventory();
            if (items.is(inv.getHelmet(), Items.Id.CURSED_HELMET)) {
                pieces++;
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, false));
            }
            if (items.is(inv.getChestplate(), Items.Id.CURSED_CHESTPLATE)) {
                pieces++;
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, false, false));
            }
            if (items.is(inv.getLeggings(), Items.Id.CURSED_LEGGINGS)) {
                pieces++;
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false, false));
            }
            if (items.is(inv.getBoots(), Items.Id.CURSED_BOOTS)) {
                pieces++;
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 1, true, false, false));
            }
            if (pieces == 4) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false, false));
            }
        }
    }

    // ------------------------------------------------------------------ espada

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (!items.is(hand, Items.Id.CURSED_SWORD)) return;
        ItemMeta meta = hand.getItemMeta();
        int charge = meta.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
        charge = Math.min(5, charge + 1);
        meta.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, charge);
        hand.setItemMeta(meta);
        if (charge == 5) killer.sendActionBar(Util.color("§5A espada está carregada."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity target)) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!items.is(hand, Items.Id.CURSED_SWORD)) return;
        ItemMeta meta = hand.getItemMeta();
        int charge = meta.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
        if (charge < 5) return;
        meta.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 0);
        hand.setItemMeta(meta);
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
        e.setDamage(e.getDamage() + 6);
    }

    // ------------------------------------------------------------------ picareta / enxada / machado

    @EventHandler(ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent e) {
        if (!items.is(e.getPlayer().getInventory().getItemInMainHand(), Items.Id.CURSED_PICKAXE)) return;
        for (var drop : e.getItems()) {
            ItemStack in = drop.getItemStack();
            ItemStack out = smelt.get(in.getType());
            if (out != null) {
                ItemStack result = out.clone();
                result.setAmount(out.getAmount() * in.getAmount());
                drop.setItemStack(result);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        Items.Id id = items.idOf(hand);
        if (id == Items.Id.CURSED_HOE && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && b.getBlockData() instanceof Ageable) {
                if (b.applyBoneMeal(e.getBlockFace())) e.setCancelled(true);
            }
        } else if (id == Items.Id.CURSED_AXE
                && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && !p.hasCooldown(hand)) {
            p.setCooldown(hand, 20 * 60);
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 10, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 0));
            p.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);
            p.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation().add(0, 2, 0), 10, 0.4, 0.2, 0.4);
        }
    }

    // ------------------------------------------------------------------ águas

    @EventHandler
    public void onSplash(PotionSplashEvent e) {
        ThrownPotion potion = e.getPotion();
        Items.Id id = items.idOf(potion.getItem());
        if (id != Items.Id.HOLY_WATER && id != Items.Id.UNHOLY_WATER) return;
        boolean holy = id == Items.Id.HOLY_WATER;
        e.setCancelled(true); // evita o efeito padrão de poção de água
        var center = potion.getLocation();
        if (holy) {
            center.getWorld().spawnParticle(Particle.END_ROD, center, 30, 1, 0.5, 1, 0.05);
            for (Entity en : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                if (en instanceof LivingEntity le && plugin.infected().isInfected(le)) plugin.infected().cure(le);
                Apparition a = plugin.apparitions().find(en);
                if (a != null) a.onHolyWater();
            }
        } else {
            center.getWorld().spawnParticle(Particle.SOUL, center, 30, 1, 0.5, 1, 0.05);
            for (Entity en : center.getWorld().getNearbyEntities(center, 4, 3, 4)) {
                if (en instanceof LivingEntity le && !(en instanceof org.bukkit.entity.Mannequin)) {
                    le.setFireTicks(100);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
                }
            }
        }
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 1f);
    }

    // ------------------------------------------------------------------ coração

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (!items.is(e.getItem(), Items.Id.HEROBRINES_HEART)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max == null) return;
        if (max.getModifier(heartKey) != null) {
            p.sendActionBar(Util.color("§cVocê já carrega o coração."));
            return;
        }
        ItemStack held = p.getInventory().getItem(e.getHand());
        if (held != null) held.setAmount(held.getAmount() - 1);
        max.addModifier(new AttributeModifier(heartKey, 20.0, AttributeModifier.Operation.ADD_NUMBER));
        p.setHealth(Math.min(max.getValue(), p.getHealth() + 20));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.6f);
        p.sendMessage(Util.color("§cO coração pulsa dentro de você. +20 de vida máxima até morrer."));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max != null && max.getModifier(heartKey) != null) {
            max.removeModifier(heartKey);
            e.getDrops().add(items.create(Items.Id.HEROBRINES_HEART, 1));
        }
    }
}
