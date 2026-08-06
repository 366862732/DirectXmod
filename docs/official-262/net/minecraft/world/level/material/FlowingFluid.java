/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.shorts.Short2BooleanMap
 *  it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap
 *  it.unimi.dsi.fastutil.shorts.Short2ObjectMap
 *  it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
 */
package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid
extends Fluid {
    public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
    private static final int CACHE_SIZE = 200;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<BlockStatePairKey> map = new Object2ByteLinkedOpenHashMap<BlockStatePairKey>(200){

            protected void rehash(int newN) {
            }
        };
        map.defaultReturnValue((byte)127);
        return map;
    });
    private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        builder.add(FALLING);
    }

    @Override
    public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        double flowX = 0.0;
        double flowZ = 0.0;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blockPos.setWithOffset((Vec3i)pos, direction);
            FluidState neighbourFluid = level.getFluidState(blockPos);
            if (!this.affectsFlow(neighbourFluid)) continue;
            float neighborHeight = neighbourFluid.getOwnHeight();
            float distance = 0.0f;
            if (neighborHeight == 0.0f) {
                Vec3i neighborPos;
                FluidState belowNeighborState;
                if (!level.getBlockState(blockPos).blocksMotion() && this.affectsFlow(belowNeighborState = level.getFluidState((BlockPos)(neighborPos = blockPos.below()))) && (neighborHeight = belowNeighborState.getOwnHeight()) > 0.0f) {
                    distance = fluidState.getOwnHeight() - (neighborHeight - 0.8888889f);
                }
            } else if (neighborHeight > 0.0f) {
                distance = fluidState.getOwnHeight() - neighborHeight;
            }
            if (distance == 0.0f) continue;
            flowX += (double)((float)direction.getStepX() * distance);
            flowZ += (double)((float)direction.getStepZ() * distance);
        }
        Vec3 flow = new Vec3(flowX, 0.0, flowZ);
        if (fluidState.getValue(FALLING).booleanValue()) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                blockPos.setWithOffset((Vec3i)pos, direction);
                if (!this.isSolidFace(level, blockPos, direction) && !this.isSolidFace(level, (BlockPos)blockPos.above(), direction)) continue;
                flow = flow.normalize().add(0.0, -6.0, 0.0);
                break;
            }
        }
        return flow.normalize();
    }

    private boolean affectsFlow(FluidState neighbourFluid) {
        return neighbourFluid.isEmpty() || neighbourFluid.getType().isSame(this);
    }

    protected boolean isSolidFace(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.getType().isSame(this)) {
            return false;
        }
        if (direction == Direction.UP) {
            return true;
        }
        if (state.getBlock() instanceof IceBlock) {
            return false;
        }
        return state.isFaceSturdy(level, pos, direction);
    }

    protected void spread(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
        FluidState newBelowFluid;
        Fluid newBelowFluidType;
        FluidState belowFluid;
        BlockState belowState;
        if (fluidState.isEmpty()) {
            return;
        }
        BlockPos belowPos = pos.below();
        if (this.canMaybePassThrough(level, pos, state, Direction.DOWN, belowPos, belowState = level.getBlockState(belowPos), belowFluid = belowState.getFluidState()) && belowFluid.canBeReplacedWith(level, belowPos, newBelowFluidType = (newBelowFluid = this.getNewLiquid(level, belowPos, belowState)).getType(), Direction.DOWN) && FlowingFluid.canHoldSpecificFluid(level, belowPos, belowState, newBelowFluidType)) {
            this.spreadTo(level, belowPos, belowState, Direction.DOWN, newBelowFluid);
            if (this.sourceNeighborCount(level, pos) >= 3) {
                this.spreadToSides(level, pos, fluidState, state);
            }
            return;
        }
        if (fluidState.isSource() || !this.isWaterHole(level, pos, state, belowPos, belowState)) {
            this.spreadToSides(level, pos, fluidState, state);
        }
    }

    private void spreadToSides(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState state) {
        int neighbor = fluidState.getAmount() - this.getDropOff(level);
        if (fluidState.getValue(FALLING).booleanValue()) {
            neighbor = 7;
        }
        if (neighbor <= 0) {
            return;
        }
        Map<Direction, FluidState> spreads = this.getSpread(level, pos, state);
        for (Map.Entry<Direction, FluidState> entry : spreads.entrySet()) {
            Direction spread = entry.getKey();
            FluidState newNeighborFluid = entry.getValue();
            BlockPos neighborPos = pos.relative(spread);
            this.spreadTo(level, neighborPos, level.getBlockState(neighborPos), spread, newNeighborFluid);
        }
    }

    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos abovePos;
        BlockState aboveState;
        FluidState aboveFluid;
        int highestNeighbor = 0;
        int neighbourSources = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos.MutableBlockPos relativePos = mutablePos.setWithOffset((Vec3i)pos, direction);
            BlockState blockState = level.getBlockState(relativePos);
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.getType().isSame(this) || !FlowingFluid.canPassThroughWall(direction, level, pos, state, relativePos, blockState)) continue;
            if (fluidState.isSource()) {
                ++neighbourSources;
            }
            highestNeighbor = Math.max(highestNeighbor, fluidState.getAmount());
        }
        if (neighbourSources >= 2 && this.canConvertToSource(level)) {
            BlockState belowState = level.getBlockState(mutablePos.setWithOffset((Vec3i)pos, Direction.DOWN));
            FluidState belowFluid = belowState.getFluidState();
            if (belowState.isSolid() || this.isSourceBlockOfThisType(belowFluid)) {
                return this.getSource(false);
            }
        }
        if (!(aboveFluid = (aboveState = level.getBlockState(abovePos = mutablePos.setWithOffset((Vec3i)pos, Direction.UP))).getFluidState()).isEmpty() && aboveFluid.getType().isSame(this) && FlowingFluid.canPassThroughWall(Direction.UP, level, pos, state, abovePos, aboveState)) {
            return this.getFlowing(8, true);
        }
        int amount = highestNeighbor - this.getDropOff(level);
        if (amount <= 0) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return this.getFlowing(amount, false);
    }

    private static boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos sourcePos, BlockState sourceState, BlockPos targetPos, BlockState targetState) {
        boolean result;
        BlockStatePairKey key;
        if (SharedConstants.DEBUG_DISABLE_LIQUID_SPREADING || SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD && targetPos.getZ() < 0) {
            return false;
        }
        VoxelShape targetShape = targetState.getCollisionShape(level, targetPos);
        if (targetShape == Shapes.block()) {
            return false;
        }
        VoxelShape sourceShape = sourceState.getCollisionShape(level, sourcePos);
        if (sourceShape == Shapes.block()) {
            return false;
        }
        if (sourceShape == Shapes.empty() && targetShape == Shapes.empty()) {
            return true;
        }
        Object2ByteLinkedOpenHashMap<BlockStatePairKey> cache = sourceState.getBlock().hasDynamicShape() || targetState.getBlock().hasDynamicShape() ? null : OCCLUSION_CACHE.get();
        if (cache != null) {
            key = new BlockStatePairKey(sourceState, targetState, direction);
            byte cached = cache.getAndMoveToFirst((Object)key);
            if (cached != 127) {
                return cached != 0;
            }
        } else {
            key = null;
        }
        boolean bl = result = !Shapes.mergedFaceOccludes(sourceShape, targetShape, direction);
        if (cache != null) {
            if (cache.size() == 200) {
                cache.removeLastByte();
            }
            cache.putAndMoveToFirst((Object)key, (byte)(result ? 1 : 0));
        }
        return result;
    }

    public abstract Fluid getFlowing();

    public FluidState getFlowing(int amount, boolean falling) {
        return (FluidState)((FluidState)this.getFlowing().defaultFluidState().setValue(LEVEL, amount)).setValue(FALLING, falling);
    }

    public abstract Fluid getSource();

    public FluidState getSource(boolean falling) {
        return (FluidState)this.getSource().defaultFluidState().setValue(FALLING, falling);
    }

    protected abstract boolean canConvertToSource(ServerLevel var1);

    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState target) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            LiquidBlockContainer container = (LiquidBlockContainer)((Object)block);
            container.placeLiquid(level, pos, state, target);
        } else {
            if (!state.isAir()) {
                this.beforeDestroyingBlock(level, pos, state);
            }
            level.setBlock(pos, target.createLegacyBlock(), 3);
        }
    }

    protected abstract void beforeDestroyingBlock(LevelAccessor var1, BlockPos var2, BlockState var3);

    protected int getSlopeDistance(LevelReader level, BlockPos pos, int pass, Direction from, BlockState state, SpreadContext context) {
        int lowest = 1000;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int v;
            if (direction == from) continue;
            BlockPos testPos = pos.relative(direction);
            BlockState testState = context.getBlockState(testPos);
            FluidState testFluidState = testState.getFluidState();
            if (!this.canPassThrough(level, this.getFlowing(), pos, state, direction, testPos, testState, testFluidState)) continue;
            if (context.isHole(testPos)) {
                return pass;
            }
            if (pass >= this.getSlopeFindDistance(level) || (v = this.getSlopeDistance(level, testPos, pass + 1, direction.getOpposite(), testState, context)) >= lowest) continue;
            lowest = v;
        }
        return lowest;
    }

    private boolean isWaterHole(BlockGetter level, BlockPos topPos, BlockState topState, BlockPos bottomPos, BlockState bottomState) {
        if (!FlowingFluid.canPassThroughWall(Direction.DOWN, level, topPos, topState, bottomPos, bottomState)) {
            return false;
        }
        if (bottomState.getFluidState().getType().isSame(this)) {
            return true;
        }
        return FlowingFluid.canHoldFluid(level, bottomPos, bottomState, this.getFlowing());
    }

    private boolean canPassThrough(BlockGetter level, Fluid fluid, BlockPos sourcePos, BlockState sourceState, Direction direction, BlockPos testPos, BlockState testState, FluidState testFluidState) {
        return this.canMaybePassThrough(level, sourcePos, sourceState, direction, testPos, testState, testFluidState) && FlowingFluid.canHoldSpecificFluid(level, testPos, testState, fluid);
    }

    private boolean canMaybePassThrough(BlockGetter level, BlockPos sourcePos, BlockState sourceState, Direction direction, BlockPos testPos, BlockState testState, FluidState testFluidState) {
        return !this.isSourceBlockOfThisType(testFluidState) && FlowingFluid.canHoldAnyFluid(testState) && FlowingFluid.canPassThroughWall(direction, level, sourcePos, sourceState, testPos, testState);
    }

    private boolean isSourceBlockOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }

    protected abstract int getSlopeFindDistance(LevelReader var1);

    private int sourceNeighborCount(LevelReader level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos testPos = pos.relative(direction);
            FluidState testFluidState = level.getFluidState(testPos);
            if (!this.isSourceBlockOfThisType(testFluidState)) continue;
            ++count;
        }
        return count;
    }

    protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState state) {
        int lowest = 1000;
        EnumMap result = Maps.newEnumMap(Direction.class);
        SpreadContext context = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int distance;
            FluidState newFluid;
            FluidState testFluidState;
            BlockState testState;
            BlockPos testPos;
            if (!this.canMaybePassThrough(level, pos, state, direction, testPos = pos.relative(direction), testState = level.getBlockState(testPos), testFluidState = testState.getFluidState()) || !FlowingFluid.canHoldSpecificFluid(level, testPos, testState, (newFluid = this.getNewLiquid(level, testPos, testState)).getType())) continue;
            if (context == null) {
                context = new SpreadContext(this, level, pos);
            }
            if ((distance = context.isHole(testPos) ? 0 : this.getSlopeDistance(level, testPos, 1, direction.getOpposite(), testState, context)) < lowest) {
                result.clear();
            }
            if (distance > lowest) continue;
            if (testFluidState.canBeReplacedWith(level, testPos, newFluid.getType(), direction)) {
                result.put(direction, newFluid);
            }
            lowest = distance;
        }
        return result;
    }

    private static boolean canHoldAnyFluid(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return true;
        }
        if (state.blocksMotion()) {
            return false;
        }
        return !(block instanceof DoorBlock) && !state.is(BlockTags.SIGNS) && !state.is(Blocks.LADDER) && !state.is(Blocks.SUGAR_CANE) && !state.is(Blocks.BUBBLE_COLUMN) && !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_PORTAL) && !state.is(Blocks.END_GATEWAY) && !state.is(Blocks.STRUCTURE_VOID);
    }

    private static boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid newFluid) {
        return FlowingFluid.canHoldAnyFluid(state) && FlowingFluid.canHoldSpecificFluid(level, pos, state, newFluid);
    }

    private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid newFluid) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            LiquidBlockContainer container = (LiquidBlockContainer)((Object)block);
            return container.canPlaceLiquid(null, level, pos, state, newFluid);
        }
        return true;
    }

    protected abstract int getDropOff(LevelReader var1);

    protected int getSpreadDelay(Level level, BlockPos pos, FluidState oldFluidState, FluidState newFluidState) {
        return this.getTickDelay(level);
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isSource()) {
            FluidState newFluidState = this.getNewLiquid(level, pos, level.getBlockState(pos));
            int tickDelay = this.getSpreadDelay(level, pos, fluidState, newFluidState);
            if (newFluidState.isEmpty()) {
                fluidState = newFluidState;
                blockState = Blocks.AIR.defaultBlockState();
                level.setBlock(pos, blockState, 3);
            } else if (newFluidState != fluidState) {
                fluidState = newFluidState;
                blockState = fluidState.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, fluidState.getType(), tickDelay);
            }
        }
        this.spread(level, pos, blockState, fluidState);
    }

    protected static int getLegacyLevel(FluidState fluidState) {
        if (fluidState.isSource()) {
            return 0;
        }
        return 8 - Math.min(fluidState.getAmount(), 8) + (fluidState.getValue(FALLING) != false ? 8 : 0);
    }

    private static boolean hasSameAbove(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return fluidState.getType().isSame(level.getFluidState(pos.above()).getType());
    }

    @Override
    public float getHeight(FluidState fluidState, BlockGetter level, BlockPos pos) {
        if (FlowingFluid.hasSameAbove(fluidState, level, pos)) {
            return 1.0f;
        }
        return fluidState.getOwnHeight();
    }

    @Override
    public float getOwnHeight(FluidState fluidState) {
        return (float)fluidState.getAmount() / 9.0f;
    }

    @Override
    public abstract int getAmount(FluidState var1);

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        if (state.getAmount() == 9 && FlowingFluid.hasSameAbove(state, level, pos)) {
            return Shapes.block();
        }
        return this.shapes.computeIfAbsent(state, fluidState -> Shapes.box(0.0, 0.0, 0.0, 1.0, fluidState.getHeight(level, pos), 1.0));
    }

    private record BlockStatePairKey(BlockState first, BlockState second, Direction direction) {
        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BlockStatePairKey)) return false;
            BlockStatePairKey that = (BlockStatePairKey)o;
            if (this.first != that.first) return false;
            if (this.second != that.second) return false;
            if (this.direction != that.direction) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.first);
            result = 31 * result + System.identityHashCode(this.second);
            result = 31 * result + this.direction.hashCode();
            return result;
        }
    }

    protected class SpreadContext {
        private final BlockGetter level;
        private final BlockPos origin;
        private final Short2ObjectMap<BlockState> stateCache;
        private final Short2BooleanMap holeCache;
        final /* synthetic */ FlowingFluid this$0;

        private SpreadContext(FlowingFluid this$0, BlockGetter level, BlockPos origin) {
            FlowingFluid flowingFluid = this$0;
            Objects.requireNonNull(flowingFluid);
            this.this$0 = flowingFluid;
            this.stateCache = new Short2ObjectOpenHashMap();
            this.holeCache = new Short2BooleanOpenHashMap();
            this.level = level;
            this.origin = origin;
        }

        public BlockState getBlockState(BlockPos pos) {
            return this.getBlockState(pos, this.getCacheKey(pos));
        }

        private BlockState getBlockState(BlockPos pos, short key) {
            return (BlockState)this.stateCache.computeIfAbsent(key, k -> this.level.getBlockState(pos));
        }

        public boolean isHole(BlockPos pos) {
            return this.holeCache.computeIfAbsent(this.getCacheKey(pos), key -> {
                BlockState state = this.getBlockState(pos, key);
                BlockPos below = pos.below();
                BlockState belowState = this.level.getBlockState(below);
                return this.this$0.isWaterHole(this.level, pos, state, below, belowState);
            });
        }

        private short getCacheKey(BlockPos pos) {
            int relativeX = pos.getX() - this.origin.getX();
            int relativeZ = pos.getZ() - this.origin.getZ();
            return (short)((relativeX + 128 & 0xFF) << 8 | relativeZ + 128 & 0xFF);
        }
    }
}

