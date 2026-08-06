/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LeverBlock
extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<LeverBlock> CODEC = LeverBlock.simpleCodec(LeverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private final Function<BlockState, VoxelShape> shapes;

    public MapCodec<LeverBlock> codec() {
        return CODEC;
    }

    protected LeverBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false)).setValue(FACE, AttachFace.WALL));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<AttachFace, Map<Direction, VoxelShape>> attachFace = Shapes.rotateAttachFace(Block.boxZ(6.0, 8.0, 10.0, 16.0));
        return this.getShapeForEachState(state -> (VoxelShape)((Map)attachFace.get(state.getValue(FACE))).get(state.getValue(FACING)), POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState stateBefore, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            BlockState stateAfter = (BlockState)stateBefore.cycle(POWERED);
            if (stateAfter.getValue(POWERED).booleanValue()) {
                LeverBlock.makeParticle(stateAfter, level, pos, 1.0f);
            }
        } else {
            this.pull(stateBefore, level, pos, null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (explosion.canTriggerBlocks()) {
            this.pull(state, level, pos, null);
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    public void pull(BlockState state, Level level, BlockPos pos, @Nullable Player player) {
        state = (BlockState)state.cycle(POWERED);
        level.setBlock(pos, state, 3);
        this.updateNeighbours(state, level, pos);
        LeverBlock.playSound(player, level, pos, state);
        level.gameEvent((Entity)player, state.getValue(POWERED) != false ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
    }

    protected static void playSound(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState stateAfter) {
        float pitch = stateAfter.getValue(POWERED) != false ? 0.6f : 0.5f;
        level.playSound(player, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, pitch);
    }

    private static void makeParticle(BlockState state, LevelAccessor level, BlockPos pos, float scale) {
        Direction opposite = ((Direction)state.getValue(FACING)).getOpposite();
        Direction oppositeConnect = LeverBlock.getConnectedDirection(state).getOpposite();
        double x = (double)pos.getX() + 0.5 + 0.1 * (double)opposite.getStepX() + 0.2 * (double)oppositeConnect.getStepX();
        double y = (double)pos.getY() + 0.5 + 0.1 * (double)opposite.getStepY() + 0.2 * (double)oppositeConnect.getStepY();
        double z = (double)pos.getZ() + 0.5 + 0.1 * (double)opposite.getStepZ() + 0.2 * (double)oppositeConnect.getStepZ();
        level.addParticle(new DustParticleOptions(0xFF0000, scale), x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED).booleanValue() && random.nextFloat() < 0.25f) {
            LeverBlock.makeParticle(state, level, pos, 0.5f);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && state.getValue(POWERED).booleanValue()) {
            this.updateNeighbours(state, level, pos);
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (state.getValue(POWERED).booleanValue() && LeverBlock.getConnectedDirection(state) == direction) {
            return 15;
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWERED) != false ? 15 : 0;
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        Direction front;
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, front, (front = LeverBlock.getConnectedDirection(state).getOpposite()).getAxis().isHorizontal() ? Direction.UP : (Direction)state.getValue(FACING));
        level.updateNeighborsAt(pos, this, orientation);
        level.updateNeighborsAt(pos.relative(front), this, orientation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, POWERED);
    }
}

