package com.euandrelucas.herobrine.item;

import com.euandrelucas.herobrine.HerobrinePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Locale;

/** Itens customizados do Herobrine, identificados por uma tag no item (sem resource pack). */
public final class Items {

    public enum Id {
        CURSED_DUST("Pó Amaldiçoado", Material.GLOWSTONE_DUST, 0x8B2BE2, "Dizem que guarda um poder maligno.", "Essencial para a magia do Herobrine."),
        CURSED_DIAMOND("Diamante Amaldiçoado", Material.DIAMOND, 0xA020F0, "Solta pequenas lascas, mas não perde massa.", "Coloque-o no Altar para chamá-lo."),
        PURIFIED_DIAMOND("Diamante Purificado", Material.DIAMOND, 0xFFFFFF, "Purificado com Água Benta e encantamentos.", "Coloque-o no Altar para expulsá-lo."),
        HOLY_WATER("Água Benta", Material.SPLASH_POTION, 0xFFFFE0, "Abençoada por Notch.", "Cura mobs infectados e fere o Herobrine."),
        UNHOLY_WATER("Água Profana", Material.SPLASH_POTION, 0x4B0082, "Água Benta corrompida.", "Incendeia, causa Fome e Definhamento."),
        BEDROCK_SWORD("Espada de Bedrock", Material.NETHERITE_SWORD, 0x555555, "A arma do Herobrine.", "Dano 10, durabilidade infinita."),
        HEROBRINES_HEART("Coração do Herobrine", Material.GOLDEN_APPLE, 0xDC143C, "Concede +20 de vida máxima até você morrer.", "Cai de novo quando você morre."),
        CURSED_SWORD("Espada de Diamante Amaldiçoado", Material.DIAMOND_SWORD, 0xA020F0, "Mortes a carregam; o golpe carregado invoca um raio.", ""),
        CURSED_PICKAXE("Picareta de Diamante Amaldiçoado", Material.DIAMOND_PICKAXE, 0xA020F0, "Funde os blocos enquanto minera.", ""),
        CURSED_SHOVEL("Pá de Diamante Amaldiçoado", Material.DIAMOND_SHOVEL, 0xA020F0, "Toque suave embutido.", ""),
        CURSED_HOE("Enxada de Diamante Amaldiçoado", Material.DIAMOND_HOE, 0xA020F0, "Clique direito em plantações: fertiliza.", ""),
        CURSED_AXE("Machado de Diamante Amaldiçoado", Material.DIAMOND_AXE, 0xA020F0, "Clique direito: fúria curta e poderosa.", ""),
        CURSED_HELMET("Capacete Amaldiçoado", Material.DIAMOND_HELMET, 0xA020F0, "Visão noturna.", "Conjunto completo: regeneração e resistência ao fogo."),
        CURSED_CHESTPLATE("Peitoral Amaldiçoado", Material.DIAMOND_CHESTPLATE, 0xA020F0, "Resistência.", "Conjunto completo: regeneração e resistência ao fogo."),
        CURSED_LEGGINGS("Calças Amaldiçoadas", Material.DIAMOND_LEGGINGS, 0xA020F0, "Velocidade.", "Conjunto completo: regeneração e resistência ao fogo."),
        CURSED_BOOTS("Botas Amaldiçoadas", Material.DIAMOND_BOOTS, 0xA020F0, "Super salto.", "Conjunto completo: regeneração e resistência ao fogo.");

        final String display;
        final Material material;
        final int color;
        final String lore1;
        final String lore2;

        Id(String display, Material material, int color, String lore1, String lore2) {
            this.display = display;
            this.material = material;
            this.color = color;
            this.lore1 = lore1;
            this.lore2 = lore2;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final HerobrinePlugin plugin;
    private final NamespacedKey idKey;
    private final java.util.List<NamespacedKey> recipeKeys = new java.util.ArrayList<>();

    public Items(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item");
    }

    public NamespacedKey idKey() {
        return idKey;
    }

    public ItemStack create(Id id, int amount) {
        ItemStack item = new ItemStack(id.material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(id.display, TextColor.color(id.color)).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = id.lore2.isEmpty()
                ? List.of(gray(id.lore1))
                : List.of(gray(id.lore1), gray(id.lore2));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id.key());
        meta.setEnchantmentGlintOverride(true);

        if (meta instanceof PotionMeta pm) {
            pm.setBasePotionType(PotionType.WATER);
            pm.setColor(Color.fromRGB(id.color));
        }
        switch (id) {
            case BEDROCK_SWORD -> {
                meta.setUnbreakable(true);
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, "bedrock_damage"), 9.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            }
            case CURSED_SHOVEL -> meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
            case CURSED_SWORD, CURSED_PICKAXE, CURSED_HOE, CURSED_AXE, CURSED_HELMET, CURSED_CHESTPLATE,
                 CURSED_LEGGINGS, CURSED_BOOTS -> meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            default -> {
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Component gray(String s) {
        return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public Id idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Id.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean is(ItemStack item, Id id) {
        return idOf(item) == id;
    }

    private RecipeChoice.ExactChoice exact(Id id) {
        return new RecipeChoice.ExactChoice(create(id, 1));
    }

    public void registerRecipes() {
        // Diamante Amaldiçoado: diamante + 2 pó amaldiçoado
        ShapelessRecipe cd = new ShapelessRecipe(new NamespacedKey(plugin, "cursed_diamond"), create(Id.CURSED_DIAMOND, 1));
        cd.addIngredient(Material.DIAMOND);
        cd.addIngredient(exact(Id.CURSED_DUST));
        cd.addIngredient(exact(Id.CURSED_DUST));
        add(cd);

        // Água Benta: garrafa + ouro + pó de glowstone + lágrima de ghast
        ShapelessRecipe hw = new ShapelessRecipe(new NamespacedKey(plugin, "holy_water"), create(Id.HOLY_WATER, 1));
        hw.addIngredient(Material.GLASS_BOTTLE);
        hw.addIngredient(Material.GOLD_INGOT);
        hw.addIngredient(Material.GLOWSTONE_DUST);
        hw.addIngredient(Material.GHAST_TEAR);
        add(hw);

        // Água Profana: água benta + pó amaldiçoado + olho de aranha fermentado
        ShapelessRecipe uw = new ShapelessRecipe(new NamespacedKey(plugin, "unholy_water"), create(Id.UNHOLY_WATER, 1));
        uw.addIngredient(exact(Id.HOLY_WATER));
        uw.addIngredient(exact(Id.CURSED_DUST));
        uw.addIngredient(Material.FERMENTED_SPIDER_EYE);
        add(uw);

        // Diamante Purificado: diamante amaldiçoado + 2 águas bentas + estrela do Nether
        ShapelessRecipe pd = new ShapelessRecipe(new NamespacedKey(plugin, "purified_diamond"), create(Id.PURIFIED_DIAMOND, 1));
        pd.addIngredient(exact(Id.CURSED_DIAMOND));
        pd.addIngredient(exact(Id.HOLY_WATER));
        pd.addIngredient(exact(Id.HOLY_WATER));
        pd.addIngredient(Material.NETHER_STAR);
        add(pd);

        shaped(Id.CURSED_SWORD, new String[]{" D ", " D ", " S "});
        shaped(Id.CURSED_PICKAXE, new String[]{"DDD", " S ", " S "});
        shaped(Id.CURSED_SHOVEL, new String[]{" D ", " S ", " S "});
        shaped(Id.CURSED_HOE, new String[]{"DD ", " S ", " S "});
        shaped(Id.CURSED_AXE, new String[]{"DD ", "DS ", " S "});
        shaped(Id.CURSED_HELMET, new String[]{"DDD", "D D", "   "});
        shaped(Id.CURSED_CHESTPLATE, new String[]{"D D", "DDD", "DDD"});
        shaped(Id.CURSED_LEGGINGS, new String[]{"DDD", "D D", "D D"});
        shaped(Id.CURSED_BOOTS, new String[]{"   ", "D D", "D D"});
    }

    private void shaped(Id result, String[] shape) {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, result.key()), create(result, 1));
        r.shape(shape);
        r.setIngredient('D', exact(Id.CURSED_DIAMOND));
        if (String.join("", shape).indexOf('S') >= 0) r.setIngredient('S', Material.STICK);
        add(r);
    }

    private void add(org.bukkit.inventory.Recipe r) {
        if (r instanceof org.bukkit.Keyed k) {
            plugin.getServer().removeRecipe(k.getKey()); // evita duplicata em /reload
            recipeKeys.add(k.getKey());
        }
        plugin.getServer().addRecipe(r);
    }

    public void unregisterRecipes() {
        recipeKeys.forEach(plugin.getServer()::removeRecipe);
        recipeKeys.clear();
    }
}
