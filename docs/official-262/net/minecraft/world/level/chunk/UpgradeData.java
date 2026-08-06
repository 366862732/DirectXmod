/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.ints.IntArrays
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  it.unimi.dsi.fastutil.objects.ObjectSet
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public class UpgradeData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final UpgradeData EMPTY = new UpgradeData(EmptyBlockGetter.INSTANCE);
    private static final String TAG_INDICES = "Indices";
    private static final Direction8[] DIRECTIONS = Direction8.values();
    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec().orElse((Object)Blocks.AIR)).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec().orElse((Object)Fluids.EMPTY)).listOf();
    private final EnumSet<Direction8> sides = EnumSet.noneOf(Direction8.class);
    private final List<SavedTick<Block>> neighborBlockTicks = Lists.newArrayList();
    private final List<SavedTick<Fluid>> neighborFluidTicks = Lists.newArrayList();
    private final int[][] index;
    private static final Map<Block, BlockFixer> MAP = new IdentityHashMap<Block, BlockFixer>();
    private static final Set<BlockFixer> CHUNKY_FIXERS = Sets.newHashSet();

    private UpgradeData(LevelHeightAccessor levelHeightAccessor) {
        this.index = new int[levelHeightAccessor.getSectionsCount()][];
    }

    public UpgradeData(CompoundTag tag, LevelHeightAccessor levelHeightAccessor) {
        this(levelHeightAccessor);
        tag.getCompound(TAG_INDICES).ifPresent(indicesTag -> {
            for (int i = 0; i < this.index.length; ++i) {
                this.index[i] = indicesTag.getIntArray(String.valueOf(i)).orElse(null);
            }
        });
        int sideInt = tag.getIntOr("Sides", 0);
        for (Direction8 direction8 : Direction8.values()) {
            if ((sideInt & 1 << direction8.ordinal()) == 0) continue;
            this.sides.add(direction8);
        }
        tag.read("neighbor_block_ticks", BLOCK_TICKS_CODEC).ifPresent(this.neighborBlockTicks::addAll);
        tag.read("neighbor_fluid_ticks", FLUID_TICKS_CODEC).ifPresent(this.neighborFluidTicks::addAll);
    }

    private UpgradeData(UpgradeData source) {
        this.sides.addAll(source.sides);
        this.neighborBlockTicks.addAll(source.neighborBlockTicks);
        this.neighborFluidTicks.addAll(source.neighborFluidTicks);
        this.index = new int[source.index.length][];
        for (int i = 0; i < source.index.length; ++i) {
            int[] indices = source.index[i];
            this.index[i] = indices != null ? IntArrays.copy((int[])indices) : null;
        }
    }

    public void upgrade(LevelChunk chunk) {
        this.upgradeInside(chunk);
        for (Direction8 direction8 : DIRECTIONS) {
            UpgradeData.upgradeSides(chunk, direction8);
        }
        Level level = chunk.getLevel();
        this.neighborBlockTicks.forEach(tick -> {
            Block type = tick.type() == Blocks.AIR ? level.getBlockState(tick.pos()).getBlock() : (Block)tick.type();
            level.scheduleTick(tick.pos(), type, tick.delay(), tick.priority());
        });
        this.neighborFluidTicks.forEach(tick -> {
            Fluid type = tick.type() == Fluids.EMPTY ? level.getFluidState(tick.pos()).getType() : (Fluid)tick.type();
            level.scheduleTick(tick.pos(), type, tick.delay(), tick.priority());
        });
        CHUNKY_FIXERS.forEach(fixer -> fixer.processChunk(level));
    }

    private static void upgradeSides(LevelChunk chunk, Direction8 direction8) {
        Level level = chunk.getLevel();
        if (!chunk.getUpgradeData().sides.remove((Object)direction8)) {
            return;
        }
        Set<Direction> directions = direction8.getDirections();
        boolean min = false;
        int max = 15;
        boolean east = directions.contains(Direction.EAST);
        boolean west = directions.contains(Direction.WEST);
        boolean south = directions.contains(Direction.SOUTH);
        boolean north = directions.contains(Direction.NORTH);
        boolean singular = directions.size() == 1;
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX() + (singular && (north || south) ? 1 : (west ? 0 : 15));
        int maxX = chunkPos.getMinBlockX() + (singular && (north || south) ? 14 : (west ? 0 : 15));
        int minZ = chunkPos.getMinBlockZ() + (singular && (east || west) ? 1 : (north ? 0 : 15));
        int maxZ = chunkPos.getMinBlockZ() + (singular && (east || west) ? 14 : (north ? 0 : 15));
        Direction[] updateDirections = Direction.values();
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(minX, level.getMinY(), minZ, maxX, level.getMaxY(), maxZ)) {
            BlockState state;
            BlockState newState = state = level.getBlockState(pos);
            for (Direction direction : updateDirections) {
                neighbourPos.setWithOffset((Vec3i)pos, direction);
                newState = UpgradeData.updateState(newState, direction, level, pos, neighbourPos);
            }
            Block.updateOrDestroy(state, newState, level, pos, 18);
        }
    }

    private static BlockState updateState(BlockState state, Direction direction, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return MAP.getOrDefault(state.getBlock(), BlockFixers.DEFAULT).updateShape(state, direction, level.getBlockState(neighbourPos), level, pos, neighbourPos);
    }

    private void upgradeInside(LevelChunk chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();
        Level level = chunk.getLevel();
        for (int sectionIndex = 0; sectionIndex < this.index.length; ++sectionIndex) {
            LevelChunkSection chunkSection = chunk.getSection(sectionIndex);
            int[] upgradeIndex = this.index[sectionIndex];
            this.index[sectionIndex] = null;
            if (upgradeIndex == null || upgradeIndex.length <= 0) continue;
            Direction[] directions = Direction.values();
            PalettedContainer<BlockState> states = chunkSection.getStates();
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            int bottomYInSection = SectionPos.sectionToBlockCoord(sectionY);
            for (int coordinate : upgradeIndex) {
                BlockState state;
                int x = coordinate & 0xF;
                int y = coordinate >> 8 & 0xF;
                int z = coordinate >> 4 & 0xF;
                pos.set(chunkPos.getMinBlockX() + x, bottomYInSection + y, chunkPos.getMinBlockZ() + z);
                BlockState newState = state = states.get(coordinate);
                for (Direction direction : directions) {
                    neighbourPos.setWithOffset((Vec3i)pos, direction);
                    if (SectionPos.blockToSectionCoord(pos.getX()) != chunkPos.x() || SectionPos.blockToSectionCoord(pos.getZ()) != chunkPos.z()) continue;
                    newState = UpgradeData.updateState(newState, direction, level, pos, neighbourPos);
                }
                Block.updateOrDestroy(state, newState, level, pos, 18);
            }
        }
        for (int i = 0; i < this.index.length; ++i) {
            if (this.index[i] != null) {
                LOGGER.warn("Discarding update data for section {} for chunk ({} {})", new Object[]{level.getSectionYFromSectionIndex(i), chunkPos.x(), chunkPos.z()});
            }
            this.index[i] = null;
        }
    }

    public boolean isEmpty() {
        for (int[] ints : this.index) {
            if (ints == null) continue;
            return false;
        }
        return this.sides.isEmpty();
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        CompoundTag indicesTag = new CompoundTag();
        for (int i = 0; i < this.index.length; ++i) {
            String key = String.valueOf(i);
            if (this.index[i] == null || this.index[i].length == 0) continue;
            indicesTag.putIntArray(key, this.index[i]);
        }
        if (!indicesTag.isEmpty()) {
            tag.put(TAG_INDICES, indicesTag);
        }
        int sides = 0;
        for (Direction8 side : this.sides) {
            sides |= 1 << side.ordinal();
        }
        tag.putByte("Sides", (byte)sides);
        if (!this.neighborBlockTicks.isEmpty()) {
            tag.store("neighbor_block_ticks", BLOCK_TICKS_CODEC, this.neighborBlockTicks);
        }
        if (!this.neighborFluidTicks.isEmpty()) {
            tag.store("neighbor_fluid_ticks", FLUID_TICKS_CODEC, this.neighborFluidTicks);
        }
        return tag;
    }

    public UpgradeData copy() {
        if (this == EMPTY) {
            return EMPTY;
        }
        return new UpgradeData(this);
    }

    private static enum BlockFixers implements BlockFixer
    {
        BLACKLIST(new Block[]{Blocks.OBSERVER, Blocks.NETHER_PORTAL, Blocks.CONCRETE_POWDER.white(), Blocks.CONCRETE_POWDER.orange(), Blocks.CONCRETE_POWDER.magenta(), Blocks.CONCRETE_POWDER.lightBlue(), Blocks.CONCRETE_POWDER.yellow(), Blocks.CONCRETE_POWDER.lime(), Blocks.CONCRETE_POWDER.pink(), Blocks.CONCRETE_POWDER.gray(), Blocks.CONCRETE_POWDER.lightGray(), Blocks.CONCRETE_POWDER.cyan(), Blocks.CONCRETE_POWDER.purple(), Blocks.CONCRETE_POWDER.blue(), Blocks.CONCRETE_POWDER.brown(), Blocks.CONCRETE_POWDER.green(), Blocks.CONCRETE_POWDER.red(), Blocks.CONCRETE_POWDER.black(), Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.DRAGON_EGG, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.ACACIA_SIGN, Blocks.CHERRY_SIGN, Blocks.JUNGLE_SIGN, Blocks.DARK_OAK_SIGN, Blocks.PALE_OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.PALE_OAK_WALL_SIGN, Blocks.OAK_HANGING_SIGN, Blocks.SPRUCE_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.ACACIA_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN, Blocks.DARK_OAK_HANGING_SIGN, Blocks.PALE_OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN}){

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
                return state;
            }
        }
        ,
        DEFAULT(new Block[0]){

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
                return state.updateShape(level, level, pos, direction, neighbourPos, level.getBlockState(neighbourPos), level.getRandom());
            }
        }
        ,
        CHEST(new Block[]{Blocks.CHEST, Blocks.TRAPPED_CHEST}){

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
                if (neighbour.is(state.getBlock()) && direction.getAxis().isHorizontal() && state.getValue(ChestBlock.TYPE) == ChestType.SINGLE && neighbour.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                    Direction facing = state.getValue(ChestBlock.FACING);
                    if (direction.getAxis() != facing.getAxis() && facing == neighbour.getValue(ChestBlock.FACING)) {
                        ChestType newType = direction == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
                        level.setBlock(neighbourPos, (BlockState)neighbour.setValue(ChestBlock.TYPE, newType.getOpposite()), 18);
                        if (facing == Direction.NORTH || facing == Direction.EAST) {
                            BlockEntity one = level.getBlockEntity(pos);
                            BlockEntity two = level.getBlockEntity(neighbourPos);
                            if (one instanceof ChestBlockEntity) {
                                ChestBlockEntity firstChestBlockEntity = (ChestBlockEntity)one;
                                if (two instanceof ChestBlockEntity) {
                                    ChestBlockEntity secondChestBlockEntity = (ChestBlockEntity)two;
                                    ChestBlockEntity.swapContents(firstChestBlockEntity, secondChestBlockEntity);
                                }
                            }
                        }
                        return (BlockState)state.setValue(ChestBlock.TYPE, newType);
                    }
                }
                return state;
            }
        }
        ,
        LEAVES(true, new Block[]{Blocks.ACACIA_LEAVES, Blocks.CHERRY_LEAVES, Blocks.BIRCH_LEAVES, Blocks.PALE_OAK_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES}){
            private final ThreadLocal<List<ObjectSet<BlockPos>>> queue = ThreadLocal.withInitial(() -> Lists.newArrayListWithCapacity((int)7));

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
                BlockState newState = state.updateShape(level, level, pos, direction, neighbourPos, level.getBlockState(neighbourPos), level.getRandom());
                if (state != newState) {
                    int distance = newState.getValue(BlockStateProperties.DISTANCE);
                    List<ObjectSet<BlockPos>> queue = this.queue.get();
                    if (queue.isEmpty()) {
                        for (int i = 0; i < 7; ++i) {
                            queue.add((ObjectSet<BlockPos>)new ObjectOpenHashSet());
                        }
                    }
                    queue.get(distance).add((Object)pos.immutable());
                }
                return state;
            }

            @Override
            public void processChunk(LevelAccessor level) {
                BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
                List<ObjectSet<BlockPos>> queue = this.queue.get();
                for (int neighborDistance = 2; neighborDistance < queue.size(); ++neighborDistance) {
                    int currentDistance = neighborDistance - 1;
                    ObjectSet<BlockPos> set = queue.get(currentDistance);
                    ObjectSet<BlockPos> newSet = queue.get(neighborDistance);
                    for (BlockPos pos : set) {
                        BlockState state = level.getBlockState(pos);
                        if (state.getValue(BlockStateProperties.DISTANCE) < currentDistance) continue;
                        level.setBlock(pos, (BlockState)state.setValue(BlockStateProperties.DISTANCE, currentDistance), 18);
                        if (neighborDistance == 7) continue;
                        for (Direction direction : DIRECTIONS) {
                            neighborPos.setWithOffset((Vec3i)pos, direction);
                            BlockState neighbor = level.getBlockState(neighborPos);
                            if (!neighbor.hasProperty(BlockStateProperties.DISTANCE) || state.getValue(BlockStateProperties.DISTANCE) <= neighborDistance) continue;
                            newSet.add((Object)neighborPos.immutable());
                        }
                    }
                }
                queue.clear();
            }
        }
        ,
        STEM_BLOCK(new Block[]{Blocks.MELON_STEM, Blocks.PUMPKIN_STEM}){

            @Override
            public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
                if (state.getValue(StemBlock.AGE) == 7) {
                    Block fruit;
                    Block block = fruit = state.is(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
                    if (neighbour.is(fruit)) {
                        return (BlockState)(state.is(Blocks.PUMPKIN_STEM) ? Blocks.ATTACHED_PUMPKIN_STEM : Blocks.ATTACHED_MELON_STEM).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
                    }
                }
                return state;
            }
        };

        public static final Direction[] DIRECTIONS;

        private BlockFixers(Block ... blocks) {
            this(false, blocks);
        }

        private BlockFixers(boolean chunky, Block ... blocks) {
            for (Block block : blocks) {
                MAP.put(block, this);
            }
            if (chunky) {
                CHUNKY_FIXERS.add(this);
            }
        }

        static {
            DIRECTIONS = Direction.values();
        }
    }

    public static interface BlockFixer {
        public BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6);

        default public void processChunk(LevelAccessor level) {
        }
    }
}

