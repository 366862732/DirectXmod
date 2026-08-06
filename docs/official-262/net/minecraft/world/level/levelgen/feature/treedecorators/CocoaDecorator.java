/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class CocoaDecorator
extends TreeDecorator {
    public static final MapCodec<CocoaDecorator> CODEC = Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("probability").xmap(CocoaDecorator::new, d -> Float.valueOf(d.probability));
    private final float probability;

    public CocoaDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.COCOA;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        if (random.nextFloat() >= this.probability) {
            return;
        }
        ObjectArrayList<BlockPos> logs = context.logs();
        if (logs.isEmpty()) {
            return;
        }
        int treeY = ((BlockPos)logs.getFirst()).getY();
        logs.stream().filter(pos -> pos.getY() - treeY <= 2).forEach(pos -> {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Direction opposite;
                BlockPos cocoaPos;
                if (!(random.nextFloat() <= 0.25f) || !context.isAir(cocoaPos = pos.offset((opposite = direction.getOpposite()).getStepX(), 0, opposite.getStepZ()))) continue;
                context.setBlock(cocoaPos, (BlockState)((BlockState)Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, random.nextInt(3))).setValue(CocoaBlock.FACING, direction));
            }
        });
    }
}

