package com.euandrelucas.herobrine.world;

import com.euandrelucas.herobrine.HerobrinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocos "falsos" do Herobrine: bedrock que quebra de verdade e diamante que explode.
 * As posições são salvas em disco para sobreviver a reinícios.
 */
public final class FakeBlocks implements Listener {

    private final HerobrinePlugin plugin;
    private final File file;
    private final Map<String, Material> tracked = new HashMap<>();

    public FakeBlocks(HerobrinePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "fake_blocks.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String s : yml.getStringList("blocks")) {
            String[] p = s.split(";");
            if (p.length != 5) continue;
            Material m = Material.matchMaterial(p[4]);
            if (m != null) tracked.put(p[0] + ";" + p[1] + ";" + p[2] + ";" + p[3], m);
        }
    }

    private static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    public void register(Block b, Material type) {
        tracked.put(key(b), type);
        save();
    }

    public boolean isFake(Block b) {
        return tracked.containsKey(key(b));
    }

    private void save() {
        List<String> out = new ArrayList<>();
        tracked.forEach((k, v) -> out.add(k + ";" + v.name()));
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("blocks", out);
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui salvar fake_blocks.yml: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material type = tracked.remove(key(b));
        if (type == null) return;
        save();
        e.setCancelled(true);
        b.setType(Material.AIR);
        Location at = b.getLocation().add(0.5, 0.5, 0.5);
        if (type == Material.DIAMOND_BLOCK) {
            // diamante falso: explode (sem destruir blocos, só assusta e machuca)
            b.getWorld().createExplosion(at, 2.5f, false, false);
        } else {
            b.getWorld().playSound(at, Sound.BLOCK_STONE_BREAK, 1f, 0.6f);
        }
    }
}
