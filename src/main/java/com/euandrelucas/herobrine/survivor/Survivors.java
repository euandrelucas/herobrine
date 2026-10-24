package com.euandrelucas.herobrine.survivor;

import com.euandrelucas.herobrine.HerobrinePlugin;
import com.euandrelucas.herobrine.item.Items;
import com.euandrelucas.herobrine.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mannequin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Sobreviventes: humanos que vivem em bases e vendem o que ajuda contra (ou a favor) do Herobrine. */
public final class Survivors implements Listener {

    private static final String[] NAMES = {"Alex", "Ari", "Efe", "Kai", "Makena", "Noor", "Steve", "Sunny", "Zuri"};

    private final HerobrinePlugin plugin;
    private final NamespacedKey key;

    public Survivors(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "survivor");
    }

    public void spawn(Location at) {
        String name = NAMES[Util.rng().nextInt(NAMES.length)];
        at.getWorld().spawn(at, Mannequin.class, e -> {
            e.setPersistent(true);
            e.setInvulnerable(true);
            e.setImmovable(true);
            e.setRemoveWhenFarAway(false);
            e.setDescription(Util.color("§7Sobrevivente"));
            e.customName(Util.color("§f" + name));
            e.setProfile(io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile().name(name).build());
            e.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Mannequin m)) return;
        if (!m.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
        e.setCancelled(true);
        Merchant merchant = Bukkit.createMerchant(Util.color("§8Sobrevivente"));
        Items it = plugin.items();
        merchant.setRecipes(List.of(
                recipe(it.create(Items.Id.CURSED_DUST, 1), new ItemStack(Material.GOLD_INGOT, 4)),
                recipe(it.create(Items.Id.HOLY_WATER, 1), new ItemStack(Material.GOLD_INGOT, 3), new ItemStack(Material.EMERALD, 1)),
                recipe(new ItemStack(Material.GOLDEN_APPLE, 1), new ItemStack(Material.IRON_INGOT, 6)),
                recipe(new ItemStack(Material.BREAD, 5), new ItemStack(Material.EMERALD, 1)),
                recipe(new ItemStack(Material.TORCH, 16), new ItemStack(Material.COAL, 4))));
        e.getPlayer().openMerchant(merchant, true);
    }

    private static MerchantRecipe recipe(ItemStack result, ItemStack... cost) {
        MerchantRecipe r = new MerchantRecipe(result, 8);
        r.setIngredients(List.of(cost));
        return r;
    }
}
