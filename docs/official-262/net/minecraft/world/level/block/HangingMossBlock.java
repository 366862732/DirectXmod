/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HangingMossBlock
extends Block
implements BonemealableBlock {
    public static final MapCodec<HangingMossBlock> CODEC = HangingMossBlock.simpleCodec(HangingMossBlock::new);
    private static final VoxelShape SHAPE_BASE = Block.column(14.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_TIP = Block.column(14.0, 2.0, 16.0);
    public static final BooleanProperty TIP = BlockStateProperties.TIP;

    public MapCodec<HangingMossBlock> codec() {
        return CODEC;
    }

    public HangingMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(TIP, true));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TIP) != false ? SHAPE_TIP : SHAPE_BASE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockState above;
        if (random.nextInt(500) == 0 && ((above = level.getBlockState(pos.above())).is(BlockTags.PALE_OAK_LOGS) || above.is(Blocks.PALE_OAK_LEAVES))) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PALE_HANGING_MOSS_IDLE, SoundSource.AMBIENT, 1.0f, 1.0f, false);
        }
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return this.canStayAtPosition(level, pos);
    }

    private boolean canStayAtPosition(BlockGetter level, BlockPos pos) {
        BlockState blockState;
        BlockPos neighbourPos = pos.relative(Direction.UP);
        return MultifaceBlock.canAttachTo(level, Direction.UP, neighbourPos, blockState = level.getBlockState(neighbourPos)) || blockState.is(this);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!this.canStayAtPosition(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return (BlockState)state.setValue(TIP, !level.getBlockState(pos.below()).is(this));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canStayAtPosition(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos growPos = this.getTip(level, pos).below();
        return this.canGrowInto(level.getBlockState(growPos)) && level.isInsideBuildHeight(growPos);
    }

    private boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    public BlockPos getTip(BlockGetter level, BlockPos pos) {
        BlockState forwardState;
        BlockPos.MutableBlockPos forwardPos = pos.mutable();
        do {
            forwardPos.move(Direction.DOWN);
        } while ((forwardState = level.getBlockState(forwardPos)).is(this));
        return ((BlockPos)forwardPos.relative(Direction.UP)).immutable();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos tipPos = this.getTip(level, pos).below();
        if (!this.canGrowInto(level.getBlockState(tipPos))) {
            return;
        }
        level.setBlockAndUpdate(tipPos, (BlockState)state.setValue(TIP, true));
    }
}

