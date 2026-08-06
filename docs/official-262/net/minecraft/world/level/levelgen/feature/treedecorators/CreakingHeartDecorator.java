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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class CreakingHeartDecorator
extends TreeDecorator {
    public static final MapCodec<CreakingHeartDecorator> CODEC = Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("probability").xmap(CreakingHeartDecorator::new, d -> Float.valueOf(d.probability));
    private final float probability;

    public CreakingHeartDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.CREAKING_HEART;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        ObjectArrayList<BlockPos> logs = context.logs();
        if (logs.isEmpty()) {
            return;
        }
        if (random.nextFloat() >= this.probability) {
            return;
        }
        ArrayList<BlockPos> heartPlacements = new ArrayList<BlockPos>((Collection<BlockPos>)logs);
        Util.shuffle(heartPlacements, random);
        Optional<BlockPos> targetPos = heartPlacements.stream().filter(pos -> {
            for (Direction dir : Direction.values()) {
                if (context.checkBlock(pos.relative(dir), state -> state.is(BlockTags.LOGS))) continue;
                return false;
            }
            return true;
        }).findFirst();
        if (targetPos.isEmpty()) {
            return;
        }
        context.setBlock(targetPos.get(), (BlockState)((BlockState)Blocks.CREAKING_HEART.defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)).setValue(CreakingHeartBlock.NATURAL, true));
    }
}

