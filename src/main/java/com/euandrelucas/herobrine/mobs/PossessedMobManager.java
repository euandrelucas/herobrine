package com.euandrelucas.herobrine.mobs;

import com.euandrelucas.herobrine.core.HerobrinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gerencia mobs possuídos (animais e monstros), Netherbrine e a receita do item ritual de derrota.
 */
public class PossessedMobManager implements Listener {

    private final HerobrinePlugin plugin;
    private final NamespacedKey possessedKey;

    public PossessedMobManager(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.possessedKey = new NamespacedKey(plugin, "possessed");
        registerDefeatRecipe();
    }

    /**
     * 5.2: Possui um mob passivo próximo.
     */
    public void possessEntity(LivingEntity entity) {
        if (entity == null) return;
        entity.setCustomName("§c" + entity.getType().name() + " Possuído(a)");
        entity.setCustomNameVisible(true);
        entity.setGlowing(true);
        entity.getPersistentDataContainer().set(possessedKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean isPossessed(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(possessedKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfig().getBoolean("custom-mobs.possessed-monsters.enabled", true)) return;

        LivingEntity entity = event.getEntity();
        if (entity instanceof Monster) {
            double chance = plugin.getConfig().getDouble("custom-mobs.possessed-monsters.spawn-replace-chance", 0.10);
            if (Math.random() < chance) {
                possessEntity(entity);
                // Aumenta vida e dano
                if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    double current = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(current * 1.5);
                    entity.setHealth(current * 1.5);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (isPossessed(event.getEntity()) && event.getDamager() instanceof Player) {
            Entity victim = event.getEntity();
            Player player = (Player) event.getDamager();

            // Habilidades únicas de animais possuídos
            if (victim instanceof Cow) {
                player.setVelocity(player.getLocation().getDirection().multiply(-1.5).setY(0.5));
            } else if (victim instanceof Sheep) {
                victim.getWorld().spawnParticle(Particle.SMOKE_LARGE, victim.getLocation(), 20);
            } else if (victim instanceof Chicken) {
                for (int i = 0; i < 3; i++) {
                    victim.getWorld().spawnEntity(victim.getLocation(), EntityType.BAT);
                }
            }
        }
    }

    /**
     * 5.6: Receita de Crafting do Item de Banimento Definitivo.
     * Maçã dourada encantada + Estrela do Nether + Cabeça de Dragão + Blocos de Diamante.
     */
    private void registerDefeatRecipe() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lRelíquia de Banimento do Herobrine");
            meta.setLocalizedName("herobrine_banish_relic");
            item.setItemMeta(meta);
        }

        NamespacedKey recipeKey = new NamespacedKey(plugin, "herobrine_relic");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, item);
        recipe.shape("DBD", "ENE", "DBD");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('B', Material.DRAGON_HEAD);
        recipe.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
        recipe.setIngredient('N', Material.NETHER_STAR);

        try {
            Bukkit.addRecipe(recipe);
        } catch (Exception ignored) {}
    }
}
