package com.euandrelucas.herobrine.world;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.function.Consumer;

/**
 * Um bloco a ser colocado por uma estrutura.
 * {@code fake} registra o bloco como falso (quebra/explode); {@code post} roda depois (ex.: encher baú).
 */
public record Placement(Block block, BlockData data, boolean fake, Consumer<Block> post) {

    public static Placement of(Block b, Material m) {
        return new Placement(b, m.createBlockData(), false, null);
    }

    public static Placement of(Block b, BlockData d) {
        return new Placement(b, d, false, null);
    }

    public static Placement fake(Block b, Material m) {
        return new Placement(b, m.createBlockData(), true, null);
    }

    public static Placement of(Block b, Material m, Consumer<Block> post) {
        return new Placement(b, m.createBlockData(), false, post);
    }
}
