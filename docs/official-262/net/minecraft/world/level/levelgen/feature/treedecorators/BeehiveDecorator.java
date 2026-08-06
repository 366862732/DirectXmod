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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class BeehiveDecorator
extends TreeDecorator {
    public static final MapCodec<BeehiveDecorator> CODEC = Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("probability").xmap(BeehiveDecorator::new, d -> Float.valueOf(d.probability));
    private static final Direction WORLDGEN_FACING = Direction.SOUTH;
    private static final Direction[] SPAWN_DIRECTIONS = (Direction[])Direction.Plane.HORIZONTAL.stream().filter(dir -> dir != WORLDGEN_FACING.getOpposite()).toArray(Direction[]::new);
    private final float probability;

    public BeehiveDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.BEEHIVE;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        ObjectArrayList<BlockPos> leaves = context.leaves();
        ObjectArrayList<BlockPos> logs = context.logs();
        if (logs.isEmpty()) {
            return;
        }
        RandomSource random = context.random();
        if (random.nextFloat() >= this.probability) {
            return;
        }
        int hiveY = !leaves.isEmpty() ? Math.max(((BlockPos)leaves.getFirst()).getY() - 1, ((BlockPos)logs.getFirst()).getY() + 1) : Math.min(((BlockPos)logs.getFirst()).getY() + 1 + random.nextInt(3), ((BlockPos)logs.getLast()).getY());
        List hivePlacements = logs.stream().filter(pos -> pos.getY() == hiveY).flatMap(pos -> Stream.of(SPAWN_DIRECTIONS).map(pos::relative)).collect(Collectors.toList());
        if (hivePlacements.isEmpty()) {
            return;
        }
        Util.shuffle(hivePlacements, random);
        Optional<BlockPos> hivePos = hivePlacements.stream().filter(pos -> context.isAir((BlockPos)pos) && context.isAir(pos.relative(WORLDGEN_FACING))).findFirst();
        if (hivePos.isEmpty()) {
            return;
        }
        context.setBlock(hivePos.get(), (BlockState)Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
        context.level().getBlockEntity(hivePos.get(), BlockEntityTypes.BEEHIVE).ifPresent(beehive -> {
            int numBees = 2 + random.nextInt(2);
            for (int count = 0; count < numBees; ++count) {
                beehive.storeBee(BeehiveBlockEntity.Occupant.create(random.nextInt(599)));
            }
        });
    }
}

