/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public abstract class StructurePiece {
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected BoundingBox boundingBox;
    private @Nullable Direction orientation;
    private Mirror mirror;
    private Rotation rotation;
    protected int genDepth;
    private final StructurePieceType type;
    private static final Set<Block> SHAPE_CHECK_BLOCKS = ImmutableSet.builder().add((Object)Blocks.NETHER_BRICK_FENCE).add((Object)Blocks.TORCH).add((Object)Blocks.WALL_TORCH).add((Object)Blocks.OAK_FENCE).add((Object)Blocks.SPRUCE_FENCE).add((Object)Blocks.DARK_OAK_FENCE).add((Object)Blocks.PALE_OAK_FENCE).add((Object)Blocks.ACACIA_FENCE).add((Object)Blocks.BIRCH_FENCE).add((Object)Blocks.JUNGLE_FENCE).add((Object)Blocks.LADDER).add((Object)Blocks.IRON_BARS).build();

    protected StructurePiece(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
        this.type = type;
        this.genDepth = genDepth;
        this.boundingBox = boundingBox;
    }

    public StructurePiece(StructurePieceType type, CompoundTag tag) {
        this(type, tag.getIntOr("GD", 0), tag.read("BB", BoundingBox.CODEC).orElseThrow());
        int orientation = tag.getIntOr("O", 0);
        this.setOrientation(orientation == -1 ? null : Direction.from2DDataValue(orientation));
    }

    protected static BoundingBox makeBoundingBox(int x, int y, int z, Direction direction, int width, int height, int depth) {
        if (direction.getAxis() == Direction.Axis.Z) {
            return new BoundingBox(x, y, z, x + width - 1, y + height - 1, z + depth - 1);
        }
        return new BoundingBox(x, y, z, x + depth - 1, y + height - 1, z + width - 1);
    }

    protected static Direction getRandomHorizontalDirection(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    public final CompoundTag createTag(StructurePieceSerializationContext context) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.STRUCTURE_PIECE.getKey(this.getType()).toString());
        tag.store("BB", BoundingBox.CODEC, this.boundingBox);
        Direction orientation = this.getOrientation();
        tag.putInt("O", orientation == null ? -1 : orientation.get2DDataValue());
        tag.putInt("GD", this.genDepth);
        this.addAdditionalSaveData(context, tag);
        return tag;
    }

    protected abstract void addAdditionalSaveData(StructurePieceSerializationContext var1, CompoundTag var2);

    public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
    }

    public abstract void postProcess(WorldGenLevel var1, StructureManager var2, ChunkGenerator var3, RandomSource var4, BoundingBox var5, ChunkPos var6, BlockPos var7);

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public int getGenDepth() {
        return this.genDepth;
    }

    public void setGenDepth(int genDepth) {
        this.genDepth = genDepth;
    }

    public boolean isCloseToChunk(ChunkPos pos, int distance) {
        int cx = pos.getMinBlockX();
        int cz = pos.getMinBlockZ();
        return this.boundingBox.intersects(cx - distance, cz - distance, cx + 15 + distance, cz + 15 + distance);
    }

    public BlockPos getLocatorPosition() {
        return new BlockPos(this.boundingBox.getCenter());
    }

    protected BlockPos.MutableBlockPos getWorldPos(int x, int y, int z) {
        return new BlockPos.MutableBlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
    }

    protected int getWorldX(int x, int z) {
        Direction orientation = this.getOrientation();
        if (orientation == null) {
            return x;
        }
        return switch (orientation) {
            case Direction.NORTH, Direction.SOUTH -> this.boundingBox.minX() + x;
            case Direction.WEST -> this.boundingBox.maxX() - z;
            case Direction.EAST -> this.boundingBox.minX() + z;
            default -> x;
        };
    }

    protected int getWorldY(int y) {
        if (this.getOrientation() == null) {
            return y;
        }
        return y + this.boundingBox.minY();
    }

    protected int getWorldZ(int x, int z) {
        Direction orientation = this.getOrientation();
        if (orientation == null) {
            return z;
        }
        return switch (orientation) {
            case Direction.NORTH -> this.boundingBox.maxZ() - z;
            case Direction.SOUTH -> this.boundingBox.minZ() + z;
            case Direction.WEST, Direction.EAST -> this.boundingBox.minZ() + x;
            default -> z;
        };
    }

    protected void placeBlock(WorldGenLevel level, BlockState blockState, int x, int y, int z, BoundingBox chunkBB) {
        BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(pos)) {
            return;
        }
        if (!this.canBeReplaced(level, x, y, z, chunkBB)) {
            return;
        }
        if (this.mirror != Mirror.NONE) {
            blockState = blockState.mirror(this.mirror);
        }
        if (this.rotation != Rotation.NONE) {
            blockState = blockState.rotate(this.rotation);
        }
        level.setBlock(pos, blockState, 2);
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            level.scheduleTick((BlockPos)pos, fluidState.getType(), 0);
        }
        if (SHAPE_CHECK_BLOCKS.contains(blockState.getBlock())) {
            level.getChunk(pos).markPosForPostProcessing(pos);
        }
    }

    protected boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox chunkBB) {
        return true;
    }

    protected BlockState getBlock(BlockGetter level, int x, int y, int z, BoundingBox chunkBB) {
        BlockPos.MutableBlockPos blockPos = this.getWorldPos(x, y, z);
        if (!chunkBB.isInside(blockPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return level.getBlockState(blockPos);
    }

    protected boolean isInterior(LevelReader level, int x, int y, int z, BoundingBox chunkBB) {
        BlockPos.MutableBlockPos pos = this.getWorldPos(x, y + 1, z);
        if (!chunkBB.isInside(pos)) {
            return false;
        }
        return pos.getY() < level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
    }

    protected void generateAirBox(WorldGenLevel level, BoundingBox chunkBB, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                for (int z = z0; z <= z1; ++z) {
                    this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, chunkBB);
                }
            }
        }
    }

    protected void generateBox(WorldGenLevel level, BoundingBox chunkBB, int x0, int y0, int z0, int x1, int y1, int z1, BlockState edgeBlock, BlockState fillBlock, boolean skipAir) {
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                for (int z = z0; z <= z1; ++z) {
                    if (skipAir && this.getBlock(level, x, y, z, chunkBB).isAir()) continue;
                    if (y == y0 || y == y1 || x == x0 || x == x1 || z == z0 || z == z1) {
                        this.placeBlock(level, edgeBlock, x, y, z, chunkBB);
                        continue;
                    }
                    this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                }
            }
        }
    }

    protected void generateBox(WorldGenLevel level, BoundingBox chunkBB, BoundingBox boxBB, BlockState edgeBlock, BlockState fillBlock, boolean skipAir) {
        this.generateBox(level, chunkBB, boxBB.minX(), boxBB.minY(), boxBB.minZ(), boxBB.maxX(), boxBB.maxY(), boxBB.maxZ(), edgeBlock, fillBlock, skipAir);
    }

    protected void generateBox(WorldGenLevel level, BoundingBox chunkBB, int x0, int y0, int z0, int x1, int y1, int z1, boolean skipAir, RandomSource random, BlockSelector selector) {
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                for (int z = z0; z <= z1; ++z) {
                    if (skipAir && this.getBlock(level, x, y, z, chunkBB).isAir()) continue;
                    selector.next(random, x, y, z, y == y0 || y == y1 || x == x0 || x == x1 || z == z0 || z == z1);
                    this.placeBlock(level, selector.getNext(), x, y, z, chunkBB);
                }
            }
        }
    }

    protected void generateBox(WorldGenLevel level, BoundingBox chunkBB, BoundingBox boxBB, boolean skipAir, RandomSource random, BlockSelector selector) {
        this.generateBox(level, chunkBB, boxBB.minX(), boxBB.minY(), boxBB.minZ(), boxBB.maxX(), boxBB.maxY(), boxBB.maxZ(), skipAir, random, selector);
    }

    protected void generateMaybeBox(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, float probability, int x0, int y0, int z0, int x1, int y1, int z1, BlockState edgeBlock, BlockState fillBlock, boolean skipAir, boolean hasToBeInside) {
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                for (int z = z0; z <= z1; ++z) {
                    if (random.nextFloat() > probability || skipAir && this.getBlock(level, x, y, z, chunkBB).isAir() || hasToBeInside && !this.isInterior(level, x, y, z, chunkBB)) continue;
                    if (y == y0 || y == y1 || x == x0 || x == x1 || z == z0 || z == z1) {
                        this.placeBlock(level, edgeBlock, x, y, z, chunkBB);
                        continue;
                    }
                    this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                }
            }
        }
    }

    protected void maybeGenerateBlock(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, float probability, int x, int y, int z, BlockState blockState) {
        if (random.nextFloat() < probability) {
            this.placeBlock(level, blockState, x, y, z, chunkBB);
        }
    }

    protected void generateUpperHalfSphere(WorldGenLevel level, BoundingBox chunkBB, int x0, int y0, int z0, int x1, int y1, int z1, BlockState fillBlock, boolean skipAir) {
        float diagX = x1 - x0 + 1;
        float diagY = y1 - y0 + 1;
        float diagZ = z1 - z0 + 1;
        float cx = (float)x0 + diagX / 2.0f;
        float cz = (float)z0 + diagZ / 2.0f;
        for (int y = y0; y <= y1; ++y) {
            float normalizedYDistance = (float)(y - y0) / diagY;
            for (int x = x0; x <= x1; ++x) {
                float normalizedXDistance = ((float)x - cx) / (diagX * 0.5f);
                for (int z = z0; z <= z1; ++z) {
                    float dist;
                    float normalizedZDistance = ((float)z - cz) / (diagZ * 0.5f);
                    if (skipAir && this.getBlock(level, x, y, z, chunkBB).isAir() || !((dist = normalizedXDistance * normalizedXDistance + normalizedYDistance * normalizedYDistance + normalizedZDistance * normalizedZDistance) <= 1.05f)) continue;
                    this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                }
            }
        }
    }

    protected void fillColumnDown(WorldGenLevel level, BlockState blockState, int x, int startY, int z, BoundingBox chunkBB) {
        BlockPos.MutableBlockPos pos = this.getWorldPos(x, startY, z);
        if (!chunkBB.isInside(pos)) {
            return;
        }
        while (this.isReplaceableByStructures(level.getBlockState(pos)) && pos.getY() > level.getMinY() + 1) {
            level.setBlock(pos, blockState, 2);
            pos.move(Direction.DOWN);
        }
    }

    protected boolean isReplaceableByStructures(BlockState state) {
        return state.isAir() || state.liquid() || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }

    protected boolean createChest(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, int x, int y, int z, ResourceKey<LootTable> lootTable) {
        return this.createChest(level, chunkBB, random, this.getWorldPos(x, y, z), lootTable, null);
    }

    public static BlockState reorient(BlockGetter level, BlockPos blockPos, BlockState blockState) {
        Direction solidNeighbor = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos relativePos = blockPos.relative(direction);
            BlockState state = level.getBlockState(relativePos);
            if (state.is(Blocks.CHEST)) {
                return blockState;
            }
            if (!state.isSolidRender()) continue;
            if (solidNeighbor == null) {
                solidNeighbor = direction;
                continue;
            }
            solidNeighbor = null;
            break;
        }
        if (solidNeighbor != null) {
            return (BlockState)blockState.setValue(HorizontalDirectionalBlock.FACING, solidNeighbor.getOpposite());
        }
        Direction lockDir = blockState.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos relativePos = blockPos.relative(lockDir);
        if (level.getBlockState(relativePos).isSolidRender()) {
            lockDir = lockDir.getOpposite();
            relativePos = blockPos.relative(lockDir);
        }
        if (level.getBlockState(relativePos).isSolidRender()) {
            lockDir = lockDir.getClockWise();
            relativePos = blockPos.relative(lockDir);
        }
        if (level.getBlockState(relativePos).isSolidRender()) {
            lockDir = lockDir.getOpposite();
            relativePos = blockPos.relative(lockDir);
        }
        return (BlockState)blockState.setValue(HorizontalDirectionalBlock.FACING, lockDir);
    }

    protected boolean createChest(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, @Nullable BlockState blockState) {
        if (!chunkBB.isInside(pos) || level.getBlockState(pos).is(Blocks.CHEST)) {
            return false;
        }
        if (blockState == null) {
            blockState = StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState());
        }
        level.setBlock(pos, blockState, 2);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity) {
            ChestBlockEntity chestBlockEntity = (ChestBlockEntity)blockEntity;
            chestBlockEntity.setLootTable(lootTable, random.nextLong());
        }
        return true;
    }

    protected boolean createDispenser(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, int x, int y, int z, Direction facing, ResourceKey<LootTable> lootTable) {
        BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
        if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(Blocks.DISPENSER)) {
            this.placeBlock(level, (BlockState)Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), x, y, z, chunkBB);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DispenserBlockEntity) {
                DispenserBlockEntity dispenserBlockEntity = (DispenserBlockEntity)blockEntity;
                dispenserBlockEntity.setLootTable(lootTable, random.nextLong());
            }
            return true;
        }
        return false;
    }

    public void move(int dx, int dy, int dz) {
        this.boundingBox.move(dx, dy, dz);
    }

    public static BoundingBox createBoundingBox(Stream<StructurePiece> pieces) {
        return BoundingBox.encapsulatingBoxes(pieces.map(StructurePiece::getBoundingBox)::iterator).orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
    }

    public static @Nullable StructurePiece findCollisionPiece(List<StructurePiece> pieces, BoundingBox box) {
        for (StructurePiece piece : pieces) {
            if (!piece.getBoundingBox().intersects(box)) continue;
            return piece;
        }
        return null;
    }

    public @Nullable Direction getOrientation() {
        return this.orientation;
    }

    public void setOrientation(@Nullable Direction orientation) {
        this.orientation = orientation;
        if (orientation == null) {
            this.rotation = Rotation.NONE;
            this.mirror = Mirror.NONE;
        } else {
            switch (orientation) {
                case SOUTH: {
                    this.mirror = Mirror.LEFT_RIGHT;
                    this.rotation = Rotation.NONE;
                    break;
                }
                case WEST: {
                    this.mirror = Mirror.LEFT_RIGHT;
                    this.rotation = Rotation.CLOCKWISE_90;
                    break;
                }
                case EAST: {
                    this.mirror = Mirror.NONE;
                    this.rotation = Rotation.CLOCKWISE_90;
                    break;
                }
                default: {
                    this.mirror = Mirror.NONE;
                    this.rotation = Rotation.NONE;
                }
            }
        }
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public StructurePieceType getType() {
        return this.type;
    }

    public static abstract class BlockSelector {
        protected BlockState next = Blocks.AIR.defaultBlockState();

        public abstract void next(RandomSource var1, int var2, int var3, int var4, boolean var5);

        public BlockState getNext() {
            return this.next;
        }
    }
}

