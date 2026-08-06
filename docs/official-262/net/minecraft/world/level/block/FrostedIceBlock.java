/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FrostedIceBlock
extends IceBlock {
    public static final MapCodec<FrostedIceBlock> CODEC = FrostedIceBlock.simpleCodec(FrostedIceBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final int NEIGHBORS_TO_AGE = 4;
    private static final int NEIGHBORS_TO_MELT = 2;

    public MapCodec<FrostedIceBlock> codec() {
        return CODEC;
    }

    public FrostedIceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, Mth.nextInt(level.getRandom(), 60, 120));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 || this.fewerNeigboursThan(level, pos, 4)) {
            int brightness;
            int n = brightness = level.dimension() == Level.END ? level.getBrightness(LightLayer.BLOCK, pos) : level.getMaxLocalRawBrightness(pos);
            if (brightness > 11 - state.getValue(AGE) - state.getLightDampening() && this.slightlyMelt(state, level, pos)) {
                BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
                for (Direction direction : Direction.values()) {
                    neighborPos.setWithOffset((Vec3i)pos, direction);
                    BlockState neighbour = level.getBlockState(neighborPos);
                    if (!neighbour.is(this) || this.slightlyMelt(neighbour, level, neighborPos)) continue;
                    level.scheduleTick((BlockPos)neighborPos, this, Mth.nextInt(random, 20, 40));
                }
                return;
            }
        }
        level.scheduleTick(pos, this, Mth.nextInt(random, 20, 40));
    }

    private boolean slightlyMelt(BlockState state, Level level, BlockPos pos) {
        int age = state.getValue(AGE);
        if (age < 3) {
            level.setBlock(pos, (BlockState)state.setValue(AGE, age + 1), 2);
            return false;
        }
        this.melt(state, level, pos);
        return true;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (block.defaultBlockState().is(this) && this.fewerNeigboursThan(level, pos, 2)) {
            this.melt(state, level, pos);
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    private boolean fewerNeigboursThan(BlockGetter level, BlockPos pos, int limit) {
        int result = 0;
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighborPos.setWithOffset((Vec3i)pos, direction);
            if (!level.getBlockState(neighborPos).is(this) || ++result < limit) continue;
            return false;
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}

