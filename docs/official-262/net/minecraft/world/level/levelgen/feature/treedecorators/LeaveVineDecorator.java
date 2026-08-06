/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class LeaveVineDecorator
extends TreeDecorator {
    public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("probability").xmap(LeaveVineDecorator::new, d -> Float.valueOf(d.probability));
    private final float probability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.LEAVE_VINE;
    }

    public LeaveVineDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        context.leaves().forEach(pos -> {
            BlockPos south;
            BlockPos north;
            BlockPos east;
            BlockPos west;
            if (random.nextFloat() < this.probability && context.isAir(west = pos.west())) {
                LeaveVineDecorator.addHangingVine(west, VineBlock.EAST, context);
            }
            if (random.nextFloat() < this.probability && context.isAir(east = pos.east())) {
                LeaveVineDecorator.addHangingVine(east, VineBlock.WEST, context);
            }
            if (random.nextFloat() < this.probability && context.isAir(north = pos.north())) {
                LeaveVineDecorator.addHangingVine(north, VineBlock.SOUTH, context);
            }
            if (random.nextFloat() < this.probability && context.isAir(south = pos.south())) {
                LeaveVineDecorator.addHangingVine(south, VineBlock.NORTH, context);
            }
        });
    }

    private static void addHangingVine(BlockPos pos, BooleanProperty direction, TreeDecorator.Context context) {
        context.placeVine(pos, direction);
        pos = pos.below();
        for (int maxDir = 4; context.isAir(pos) && maxDir > 0; --maxDir) {
            context.placeVine(pos, direction);
            pos = pos.below();
        }
    }
}

