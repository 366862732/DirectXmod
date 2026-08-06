/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PitcherCropBlock
extends DoublePlantBlock
implements BonemealableBlock {
    public static final MapCodec<PitcherCropBlock> CODEC = PitcherCropBlock.simpleCodec(PitcherCropBlock::new);
    public static final int MAX_AGE = 4;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;
    private static final int DOUBLE_PLANT_AGE_INTERSECTION = 3;
    private static final int BONEMEAL_INCREASE = 1;
    private static final VoxelShape SHAPE_BULB = Block.column(6.0, -1.0, 3.0);
    private static final VoxelShape SHAPE_CROP = Block.column(10.0, -1.0, 5.0);
    private final Function<BlockState, VoxelShape> shapes = this.makeShapes();

    public MapCodec<PitcherCropBlock> codec() {
        return CODEC;
    }

    public PitcherCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        int[] plantHeights = new int[]{0, 9, 11, 22, 26};
        return this.getShapeForEachState(state -> {
            int height = (state.getValue(AGE) == 0 ? 4 : 6) + plantHeights[state.getValue(AGE)];
            int width = state.getValue(AGE) == 0 ? 6 : 10;
            return switch (state.getValue(HALF)) {
                default -> throw new MatchException(null, null);
                case DoubleBlockHalf.LOWER -> Block.column(width, -1.0, Math.min(16, -1 + height));
                case DoubleBlockHalf.UPPER -> Block.column(width, 0.0, Math.max(0, -1 + height - 16));
            };
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return state.getValue(AGE) == 0 ? SHAPE_BULB : SHAPE_CROP;
        }
        return Shapes.empty();
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (PitcherCropBlock.isDouble(state.getValue(AGE))) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (PitcherCropBlock.isLower(state) && !PitcherCropBlock.sufficientLight(level, pos)) {
            return false;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_CROPS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (entity instanceof Ravager && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                serverLevel.destroyBlock(pos, true, entity);
            }
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !this.isMaxAge(state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldProgressGrowth;
        float growthSpeed = CropBlock.getGrowthSpeed(this, level, pos);
        boolean bl = shouldProgressGrowth = random.nextInt((int)(25.0f / growthSpeed) + 1) == 0;
        if (shouldProgressGrowth) {
            this.grow(level, state, pos, 1);
        }
    }

    private void grow(ServerLevel level, BlockState lowerState, BlockPos lowerPos, int increase) {
        int updatedAge = Math.min(lowerState.getValue(AGE) + increase, 4);
        if (!this.canGrow(level, lowerPos, lowerState, updatedAge)) {
            return;
        }
        BlockState newLowerState = (BlockState)lowerState.setValue(AGE, updatedAge);
        level.setBlock(lowerPos, newLowerState, 2);
        if (PitcherCropBlock.isDouble(updatedAge)) {
            level.setBlock(lowerPos.above(), (BlockState)newLowerState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    private static boolean canGrowInto(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.PITCHER_CROP);
    }

    private static boolean sufficientLight(LevelReader level, BlockPos pos) {
        return CropBlock.hasSufficientLight(level, pos);
    }

    private static boolean isLower(BlockState state) {
        return state.is(Blocks.PITCHER_CROP) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean isDouble(int age) {
        return age >= 3;
    }

    private boolean canGrow(LevelReader level, BlockPos lowerPos, BlockState lowerState, int newAge) {
        return !this.isMaxAge(lowerState) && PitcherCropBlock.sufficientLight(level, lowerPos) && level.isInsideBuildHeight(lowerPos.above()) && (!PitcherCropBlock.isDouble(newAge) || PitcherCropBlock.canGrowInto(level, lowerPos.above()));
    }

    private boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) >= 4;
    }

    private @Nullable PosAndState getLowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (PitcherCropBlock.isLower(state)) {
            return new PosAndState(pos, state);
        }
        BlockPos lowerPos = pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (PitcherCropBlock.isLower(lowerState)) {
            return new PosAndState(lowerPos, lowerState);
        }
        return null;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
        if (lowerHalf == null) {
            return false;
        }
        return this.canGrow(level, lowerHalf.pos, lowerHalf.state, lowerHalf.state.getValue(AGE) + 1);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        PosAndState lowerHalf = this.getLowerHalf(level, pos, state);
        if (lowerHalf == null) {
            return;
        }
        this.grow(level, lowerHalf.state, lowerHalf.pos, 1);
    }

    private record PosAndState(BlockPos pos, BlockState state) {
    }
}

