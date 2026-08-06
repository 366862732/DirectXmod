/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class TrunkVineDecorator
extends TreeDecorator {
    public static final MapCodec<TrunkVineDecorator> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.TRUNK_VINE;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        context.logs().forEach(pos -> {
            BlockPos south;
            BlockPos north;
            BlockPos east;
            BlockPos west;
            if (random.nextInt(3) > 0 && context.isAir(west = pos.west())) {
                context.placeVine(west, VineBlock.EAST);
            }
            if (random.nextInt(3) > 0 && context.isAir(east = pos.east())) {
                context.placeVine(east, VineBlock.WEST);
            }
            if (random.nextInt(3) > 0 && context.isAir(north = pos.north())) {
                context.placeVine(north, VineBlock.SOUTH);
            }
            if (random.nextInt(3) > 0 && context.isAir(south = pos.south())) {
                context.placeVine(south, VineBlock.NORTH);
            }
        });
    }
}

