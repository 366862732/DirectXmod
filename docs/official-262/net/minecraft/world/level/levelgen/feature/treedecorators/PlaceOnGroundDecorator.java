/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class PlaceOnGroundDecorator
extends TreeDecorator {
    public static final MapCodec<PlaceOnGroundDecorator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("tries", (Object)128).forGetter(p -> p.tries), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("radius", (Object)2).forGetter(p -> p.radius), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("height", (Object)1).forGetter(p -> p.height), (App)BlockStateProvider.CODEC.fieldOf("block_state_provider").forGetter(p -> p.blockStateProvider)).apply((Applicative)i, PlaceOnGroundDecorator::new));
    private final int tries;
    private final int radius;
    private final int height;
    private final BlockStateProvider blockStateProvider;

    public PlaceOnGroundDecorator(int tries, int radius, int height, BlockStateProvider blockStateProvider) {
        this.tries = tries;
        this.radius = radius;
        this.height = height;
        this.blockStateProvider = blockStateProvider;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PLACE_ON_GROUND;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        List<BlockPos> blockPositions = TreeFeature.getLowestTrunkOrRootOfTree(context);
        if (blockPositions.isEmpty()) {
            return;
        }
        BlockPos origin = blockPositions.getFirst();
        int minY = origin.getY();
        int minX = origin.getX();
        int maxX = origin.getX();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();
        for (BlockPos position : blockPositions) {
            if (position.getY() != minY) continue;
            minX = Math.min(minX, position.getX());
            maxX = Math.max(maxX, position.getX());
            minZ = Math.min(minZ, position.getZ());
            maxZ = Math.max(maxZ, position.getZ());
        }
        RandomSource random = context.random();
        BoundingBox bb = new BoundingBox(minX, minY, minZ, maxX, minY, maxZ).inflatedBy(this.radius, this.height, this.radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < this.tries; ++i) {
            pos.set(random.nextIntBetweenInclusive(bb.minX(), bb.maxX()), random.nextIntBetweenInclusive(bb.minY(), bb.maxY()), random.nextIntBetweenInclusive(bb.minZ(), bb.maxZ()));
            this.attemptToPlaceBlockAbove(context, pos);
        }
    }

    private void attemptToPlaceBlockAbove(TreeDecorator.Context context, BlockPos pos) {
        BlockPos abovePos = pos.above();
        if (context.level().isStateAtPosition(abovePos, state -> state.isAir() || state.is(Blocks.VINE)) && context.checkBlock(pos, BlockBehaviour.BlockStateBase::isSolidRender) && context.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY() <= abovePos.getY()) {
            context.setBlock(abovePos, this.blockStateProvider.getState(context.level(), context.random(), abovePos));
        }
    }
}

