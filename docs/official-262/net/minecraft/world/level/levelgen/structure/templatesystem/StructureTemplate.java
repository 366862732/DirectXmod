/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    private final List<Palette> palettes = Lists.newArrayList();
    private final List<StructureEntityInfo> entityInfoList = Lists.newArrayList();
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(Level level, BlockPos position, Vec3i size, boolean inludeEntities, List<Block> ignoreBlocks) {
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) {
            return;
        }
        BlockPos corner2 = position.offset(size).offset(-1, -1, -1);
        ArrayList fullBlockList = Lists.newArrayList();
        ArrayList blockEntitiesList = Lists.newArrayList();
        ArrayList otherBlocksList = Lists.newArrayList();
        BlockPos minCorner = new BlockPos(Math.min(position.getX(), corner2.getX()), Math.min(position.getY(), corner2.getY()), Math.min(position.getZ(), corner2.getZ()));
        BlockPos maxCorner = new BlockPos(Math.max(position.getX(), corner2.getX()), Math.max(position.getY(), corner2.getY()), Math.max(position.getZ(), corner2.getZ()));
        this.size = size;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            for (BlockPos pos : BlockPos.betweenClosed(minCorner, maxCorner)) {
                StructureBlockInfo info;
                BlockPos relativePos = pos.subtract(minCorner);
                BlockState blockState = level.getBlockState(pos);
                if (ignoreBlocks.stream().anyMatch(blockState::is)) continue;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                    blockEntity.saveWithId(output);
                    info = new StructureBlockInfo(relativePos, blockState, output.buildResult());
                } else {
                    info = new StructureBlockInfo(relativePos, blockState, null);
                }
                StructureTemplate.addToLists(info, fullBlockList, blockEntitiesList, otherBlocksList);
            }
            List<StructureBlockInfo> blockInfoList = StructureTemplate.buildInfoList(fullBlockList, blockEntitiesList, otherBlocksList);
            this.palettes.clear();
            this.palettes.add(new Palette(blockInfoList));
            if (inludeEntities) {
                this.fillEntityList(level, minCorner, maxCorner, reporter);
            } else {
                this.entityInfoList.clear();
            }
        }
    }

    private static void addToLists(StructureBlockInfo info, List<StructureBlockInfo> fullBlockList, List<StructureBlockInfo> blockEntitiesList, List<StructureBlockInfo> otherBlocksList) {
        if (info.nbt != null) {
            blockEntitiesList.add(info);
        } else if (!info.state.getBlock().hasDynamicShape() && info.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            fullBlockList.add(info);
        } else {
            otherBlocksList.add(info);
        }
    }

    private static List<StructureBlockInfo> buildInfoList(List<StructureBlockInfo> fullBlockList, List<StructureBlockInfo> blockEntitiesList, List<StructureBlockInfo> otherBlocksList) {
        Comparator<StructureBlockInfo> comparator = Comparator.comparingInt(o -> o.pos.getY()).thenComparingInt(o -> o.pos.getX()).thenComparingInt(o -> o.pos.getZ());
        fullBlockList.sort(comparator);
        otherBlocksList.sort(comparator);
        blockEntitiesList.sort(comparator);
        ArrayList blockInfoList = Lists.newArrayList();
        blockInfoList.addAll(fullBlockList);
        blockInfoList.addAll(otherBlocksList);
        blockInfoList.addAll(blockEntitiesList);
        return blockInfoList;
    }

    private void fillEntityList(Level level, BlockPos minCorner, BlockPos maxCorner, ProblemReporter reporter) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(minCorner, maxCorner), input -> !(input instanceof Player));
        this.entityInfoList.clear();
        for (Entity entity : entities) {
            BlockPos blockPos;
            Vec3 pos = new Vec3(entity.getX() - (double)minCorner.getX(), entity.getY() - (double)minCorner.getY(), entity.getZ() - (double)minCorner.getZ());
            TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(entity.problemPath()), entity.registryAccess());
            entity.save(output);
            if (entity instanceof Painting) {
                Painting painting = (Painting)entity;
                blockPos = painting.getPos().subtract(minCorner);
            } else {
                blockPos = BlockPos.containing(pos);
            }
            this.entityInfoList.add(new StructureEntityInfo(pos, blockPos, output.buildResult().copy()));
        }
    }

    public List<StructureBlockInfo> filterBlocks(BlockPos position, StructurePlaceSettings settings, Block block) {
        return this.filterBlocks(position, settings, block, true);
    }

    public List<JigsawBlockInfo> getJigsaws(BlockPos position, Rotation rotation) {
        if (this.palettes.isEmpty()) {
            return new ArrayList<JigsawBlockInfo>();
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        List<JigsawBlockInfo> jigsaws = settings.getRandomPalette(this.palettes, position).jigsaws();
        ArrayList<JigsawBlockInfo> result = new ArrayList<JigsawBlockInfo>(jigsaws.size());
        for (JigsawBlockInfo jigsaw : jigsaws) {
            StructureBlockInfo blockInfo = jigsaw.info;
            result.add(jigsaw.withInfo(new StructureBlockInfo(StructureTemplate.calculateRelativePosition(settings, blockInfo.pos()).offset(position), blockInfo.state.rotate(settings.getRotation()), blockInfo.nbt)));
        }
        return result;
    }

    public ObjectArrayList<StructureBlockInfo> filterBlocks(BlockPos position, StructurePlaceSettings settings, Block block, boolean absolute) {
        ObjectArrayList result = new ObjectArrayList();
        BoundingBox boundingBox = settings.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return result;
        }
        for (StructureBlockInfo blockInfo : settings.getRandomPalette(this.palettes, position).blocks(block)) {
            BlockPos blockPos;
            BlockPos blockPos2 = blockPos = absolute ? StructureTemplate.calculateRelativePosition(settings, blockInfo.pos).offset(position) : blockInfo.pos;
            if (boundingBox != null && !boundingBox.isInside(blockPos)) continue;
            result.add((Object)new StructureBlockInfo(blockPos, blockInfo.state.rotate(settings.getRotation()), blockInfo.nbt));
        }
        return result;
    }

    public BlockPos calculateConnectedPosition(StructurePlaceSettings settings1, BlockPos connection1, StructurePlaceSettings settings2, BlockPos connection2) {
        BlockPos markerPos1 = StructureTemplate.calculateRelativePosition(settings1, connection1);
        BlockPos markerPos2 = StructureTemplate.calculateRelativePosition(settings2, connection2);
        return markerPos1.subtract(markerPos2);
    }

    public static BlockPos calculateRelativePosition(StructurePlaceSettings settings, BlockPos pos) {
        return StructureTemplate.transform(pos, settings.getMirror(), settings.getRotation(), settings.getRotationPivot());
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public boolean placeInWorld(ServerLevelAccessor level, BlockPos position, BlockPos referencePos, StructurePlaceSettings settings, RandomSource random, @Block.UpdateFlags int updateMode) {
        if (this.palettes.isEmpty()) {
            return false;
        }
        List<StructureBlockInfo> blockInfoList = settings.getRandomPalette(this.palettes, position).blocks();
        if (blockInfoList.isEmpty() && (settings.isIgnoreEntities() || this.entityInfoList.isEmpty()) || this.size.getX() < 1 || this.size.getY() < 1 || this.size.getZ() < 1) {
            return false;
        }
        BoundingBox boundingBox = settings.getBoundingBox();
        ArrayList toFill = Lists.newArrayListWithCapacity((int)(settings.shouldApplyWaterlogging() ? blockInfoList.size() : 0));
        ArrayList lockedFluids = Lists.newArrayListWithCapacity((int)(settings.shouldApplyWaterlogging() ? blockInfoList.size() : 0));
        @Nullable ArrayList placed = Lists.newArrayListWithCapacity((int)blockInfoList.size());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        List<StructureBlockInfo> processedBlockInfoList = StructureTemplate.processBlockInfos(level, position, referencePos, settings, blockInfoList);
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            for (StructureBlockInfo blockInfo : processedBlockInfoList) {
                BlockEntity blockEntity;
                BlockPos blockPos = blockInfo.pos;
                if (boundingBox != null && !boundingBox.isInside(blockPos)) continue;
                FluidState previousFluidState = settings.shouldApplyWaterlogging() ? level.getFluidState(blockPos) : null;
                BlockState state = blockInfo.state.mirror(settings.getMirror()).rotate(settings.getRotation());
                if (blockInfo.nbt != null) {
                    level.setBlock(blockPos, Blocks.BARRIER.defaultBlockState(), 820);
                }
                if (!level.setBlock(blockPos, state, updateMode)) continue;
                minX = Math.min(minX, blockPos.getX());
                minY = Math.min(minY, blockPos.getY());
                minZ = Math.min(minZ, blockPos.getZ());
                maxX = Math.max(maxX, blockPos.getX());
                maxY = Math.max(maxY, blockPos.getY());
                maxZ = Math.max(maxZ, blockPos.getZ());
                placed.add(Pair.of((Object)blockPos, (Object)blockInfo.nbt));
                if (blockInfo.nbt != null && (blockEntity = level.getBlockEntity(blockPos)) != null) {
                    if (!SharedConstants.DEBUG_STRUCTURE_EDIT_MODE && blockEntity instanceof RandomizableContainer) {
                        blockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                    }
                    blockEntity.loadWithComponents(TagValueInput.create(reporter.forChild(blockEntity.problemPath()), (HolderLookup.Provider)level.registryAccess(), blockInfo.nbt));
                }
                if (previousFluidState == null) continue;
                if (state.getFluidState().isSource()) {
                    lockedFluids.add(blockPos);
                    continue;
                }
                if (!(state.getBlock() instanceof LiquidBlockContainer)) continue;
                ((LiquidBlockContainer)((Object)state.getBlock())).placeLiquid(level, blockPos, state, previousFluidState);
                if (previousFluidState.isSource()) continue;
                toFill.add(blockPos);
            }
            boolean filled = true;
            Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            while (filled && !toFill.isEmpty()) {
                filled = false;
                Iterator iterator = toFill.iterator();
                while (iterator.hasNext()) {
                    BlockState state;
                    Object block;
                    BlockPos pos = (BlockPos)iterator.next();
                    FluidState toPlace = level.getFluidState(pos);
                    for (int i = 0; i < directions.length && !toPlace.isSource(); ++i) {
                        BlockPos neighborPos = pos.relative(directions[i]);
                        FluidState neighbor = level.getFluidState(neighborPos);
                        if (!neighbor.isSource() || lockedFluids.contains(neighborPos)) continue;
                        toPlace = neighbor;
                    }
                    if (!toPlace.isSource() || !((block = (state = level.getBlockState(pos)).getBlock()) instanceof LiquidBlockContainer)) continue;
                    LiquidBlockContainer liquidBlockContainer = (LiquidBlockContainer)block;
                    liquidBlockContainer.placeLiquid(level, pos, state, toPlace);
                    filled = true;
                    iterator.remove();
                }
            }
            if (minX <= maxX) {
                if (!settings.getKnownShape()) {
                    BitSetDiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
                    int startX = minX;
                    int startY = minY;
                    int startZ = minZ;
                    for (Pair blockInfo : placed) {
                        BlockPos blockPos = (BlockPos)blockInfo.getFirst();
                        ((DiscreteVoxelShape)shape).fill(blockPos.getX() - startX, blockPos.getY() - startY, blockPos.getZ() - startZ);
                    }
                    StructureTemplate.updateShapeAtEdge(level, updateMode, shape, startX, startY, startZ);
                }
                for (Pair blockInfo : placed) {
                    BlockEntity blockEntity;
                    BlockPos blockPos = (BlockPos)blockInfo.getFirst();
                    if (!settings.getKnownShape()) {
                        BlockState newState;
                        BlockState state = level.getBlockState(blockPos);
                        if (state != (newState = Block.updateFromNeighbourShapes(state, level, blockPos))) {
                            level.setBlock(blockPos, newState, updateMode & 0xFFFFFFFE | 0x10);
                        }
                        level.updateNeighborsAt(blockPos, newState.getBlock());
                    }
                    if (blockInfo.getSecond() == null || (blockEntity = level.getBlockEntity(blockPos)) == null) continue;
                    blockEntity.setChanged();
                }
            }
            if (!settings.isIgnoreEntities()) {
                this.placeEntities(level, position, settings.getMirror(), settings.getRotation(), settings.getRotationPivot(), boundingBox, settings.shouldFinalizeEntities(), reporter);
            }
        }
        return true;
    }

    public static void updateShapeAtEdge(LevelAccessor level, @Block.UpdateFlags int updateMode, DiscreteVoxelShape shape, BlockPos pos) {
        StructureTemplate.updateShapeAtEdge(level, updateMode, shape, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void updateShapeAtEdge(LevelAccessor level, @Block.UpdateFlags int updateMode, DiscreteVoxelShape shape, int startX, int startY, int startZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        shape.forAllFaces((direction, x, y, z) -> {
            BlockState newNeighborState;
            pos.set(startX + x, startY + y, startZ + z);
            neighborPos.setWithOffset((Vec3i)pos, direction);
            BlockState state = level.getBlockState(pos);
            BlockState neighborState = level.getBlockState(neighborPos);
            BlockState newState = state.updateShape(level, level, pos, direction, neighborPos, neighborState, level.getRandom());
            if (state != newState) {
                level.setBlock(pos, newState, updateMode & 0xFFFFFFFE);
            }
            if (neighborState != (newNeighborState = neighborState.updateShape(level, level, neighborPos, direction.getOpposite(), pos, newState, level.getRandom()))) {
                level.setBlock(neighborPos, newNeighborState, updateMode & 0xFFFFFFFE);
            }
        });
    }

    public static List<StructureBlockInfo> processBlockInfos(ServerLevelAccessor level, BlockPos position, BlockPos referencePos, StructurePlaceSettings settings, List<StructureBlockInfo> blockInfoList) {
        ArrayList<StructureBlockInfo> originalBlockInfoList = new ArrayList<StructureBlockInfo>();
        List<StructureBlockInfo> processedBlockInfoList = new ArrayList<StructureBlockInfo>();
        boolean processOnlyInCurrentChunk = true;
        for (StructureProcessor processor : settings.getProcessors()) {
            if (!processor.evaluatesEntirePieceState()) continue;
            processOnlyInCurrentChunk = false;
            break;
        }
        BoundingBox chunkBb = settings.getBoundingBox();
        for (StructureBlockInfo blockInfo : blockInfoList) {
            BlockPos blockPos = StructureTemplate.calculateRelativePosition(settings, blockInfo.pos).offset(position);
            if (processOnlyInCurrentChunk && chunkBb != null && !chunkBb.isInside(blockPos)) continue;
            StructureBlockInfo processedBlockInfo = new StructureBlockInfo(blockPos, blockInfo.state, blockInfo.nbt != null ? blockInfo.nbt.copy() : null);
            Iterator<StructureProcessor> iterator = settings.getProcessors().iterator();
            while (processedBlockInfo != null && iterator.hasNext()) {
                processedBlockInfo = iterator.next().processBlock(level, position, referencePos, blockInfo.pos, processedBlockInfo, settings);
            }
            if (processedBlockInfo == null) continue;
            processedBlockInfoList.add(processedBlockInfo);
            originalBlockInfoList.add(blockInfo);
        }
        for (StructureProcessor processor : settings.getProcessors()) {
            processedBlockInfoList = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings);
        }
        return processedBlockInfoList;
    }

    private void placeEntities(ServerLevelAccessor level, BlockPos position, Mirror mirror, Rotation rotation, BlockPos pivot, @Nullable BoundingBox boundingBox, boolean finalizeEntities, ProblemReporter problemReporter) {
        for (StructureEntityInfo entityInfo : this.entityInfoList) {
            BlockPos blockPos = StructureTemplate.transform(entityInfo.blockPos, mirror, rotation, pivot).offset(position);
            if (boundingBox != null && !boundingBox.isInside(blockPos)) continue;
            CompoundTag tag = entityInfo.nbt.copy();
            Vec3 relativePos = StructureTemplate.transform(entityInfo.pos, mirror, rotation, pivot);
            Vec3 pos = relativePos.add(position.getX(), position.getY(), position.getZ());
            ListTag posTag = new ListTag();
            posTag.add(DoubleTag.valueOf(pos.x));
            posTag.add(DoubleTag.valueOf(pos.y));
            posTag.add(DoubleTag.valueOf(pos.z));
            tag.put("Pos", posTag);
            tag.remove("UUID");
            StructureTemplate.createEntityIgnoreException(problemReporter, level, tag).ifPresent(entity -> {
                float yRot = entity.rotate(rotation);
                entity.snapTo(pos.x, pos.y, pos.z, yRot += entity.mirror(mirror) - entity.getYRot(), entity.getXRot());
                entity.setYBodyRot(yRot);
                entity.setYHeadRot(yRot);
                if (finalizeEntities && entity instanceof Mob) {
                    Mob mob = (Mob)entity;
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.STRUCTURE, null);
                }
                level.addFreshEntityWithPassengers((Entity)entity);
            });
        }
    }

    private static Optional<Entity> createEntityIgnoreException(ProblemReporter reporter, ServerLevelAccessor level, CompoundTag tag) {
        try {
            return EntityType.create(TagValueInput.create(reporter, (HolderLookup.Provider)level.registryAccess(), tag), level.getLevel(), new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false));
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Vec3i getSize(Rotation rotation) {
        return switch (rotation) {
            case Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90 -> new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
            default -> this.size;
        };
    }

    public static BlockPos transform(BlockPos pos, Mirror mirror, Rotation rotation, BlockPos pivot) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean wasMirrored = true;
        switch (mirror) {
            case LEFT_RIGHT: {
                z = -z;
                break;
            }
            case FRONT_BACK: {
                x = -x;
                break;
            }
            default: {
                wasMirrored = false;
            }
        }
        int pivotX = pivot.getX();
        int pivotZ = pivot.getZ();
        return switch (rotation) {
            case Rotation.CLOCKWISE_180 -> new BlockPos(pivotX + pivotX - x, y, pivotZ + pivotZ - z);
            case Rotation.COUNTERCLOCKWISE_90 -> new BlockPos(pivotX - pivotZ + z, y, pivotX + pivotZ - x);
            case Rotation.CLOCKWISE_90 -> new BlockPos(pivotX + pivotZ - z, y, pivotZ - pivotX + x);
            default -> wasMirrored ? new BlockPos(x, y, z) : pos;
        };
    }

    public static Vec3 transform(Vec3 pos, Mirror mirror, Rotation rotation, BlockPos pivot) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        boolean wasMirrored = true;
        switch (mirror) {
            case LEFT_RIGHT: {
                z = 1.0 - z;
                break;
            }
            case FRONT_BACK: {
                x = 1.0 - x;
                break;
            }
            default: {
                wasMirrored = false;
            }
        }
        int pivotX = pivot.getX();
        int pivotZ = pivot.getZ();
        return switch (rotation) {
            case Rotation.CLOCKWISE_180 -> new Vec3((double)(pivotX + pivotX + 1) - x, y, (double)(pivotZ + pivotZ + 1) - z);
            case Rotation.COUNTERCLOCKWISE_90 -> new Vec3((double)(pivotX - pivotZ) + z, y, (double)(pivotX + pivotZ + 1) - x);
            case Rotation.CLOCKWISE_90 -> new Vec3((double)(pivotX + pivotZ + 1) - z, y, (double)(pivotZ - pivotX) + x);
            default -> wasMirrored ? new Vec3(x, y, z) : pos;
        };
    }

    public BlockPos getZeroPositionWithTransform(BlockPos zeroPos, Mirror mirror, Rotation rotation) {
        return StructureTemplate.getZeroPositionWithTransform(zeroPos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos zeroPos, Mirror mirror, Rotation rotation, int sizeX, int sizeZ) {
        int mirrorDeltaX = mirror == Mirror.FRONT_BACK ? --sizeX : 0;
        int mirrorDeltaZ = mirror == Mirror.LEFT_RIGHT ? --sizeZ : 0;
        BlockPos targetPos = zeroPos;
        targetPos = switch (rotation) {
            default -> throw new MatchException(null, null);
            case Rotation.NONE -> zeroPos.offset(mirrorDeltaX, 0, mirrorDeltaZ);
            case Rotation.CLOCKWISE_90 -> zeroPos.offset(sizeZ - mirrorDeltaZ, 0, mirrorDeltaX);
            case Rotation.CLOCKWISE_180 -> zeroPos.offset(sizeX - mirrorDeltaX, 0, sizeZ - mirrorDeltaZ);
            case Rotation.COUNTERCLOCKWISE_90 -> zeroPos.offset(mirrorDeltaZ, 0, sizeX - mirrorDeltaX);
        };
        return targetPos;
    }

    public BoundingBox getBoundingBox(StructurePlaceSettings settings, BlockPos position) {
        return this.getBoundingBox(position, settings.getRotation(), settings.getRotationPivot(), settings.getMirror());
    }

    public BoundingBox getBoundingBox(BlockPos position, Rotation rotation, BlockPos pivot, Mirror mirror) {
        return StructureTemplate.getBoundingBox(position, rotation, pivot, mirror, this.size);
    }

    @VisibleForTesting
    protected static BoundingBox getBoundingBox(BlockPos position, Rotation rotation, BlockPos pivot, Mirror mirror, Vec3i size) {
        Vec3i delta = size.offset(-1, -1, -1);
        BlockPos corner1 = StructureTemplate.transform(BlockPos.ZERO, mirror, rotation, pivot);
        BlockPos corner2 = StructureTemplate.transform(BlockPos.ZERO.offset(delta), mirror, rotation, pivot);
        return BoundingBox.fromCorners(corner1, corner2).move(position);
    }

    public CompoundTag save(CompoundTag tag) {
        if (this.palettes.isEmpty()) {
            tag.put(BLOCKS_TAG, new ListTag());
            tag.put(PALETTE_TAG, new ListTag());
        } else {
            ArrayList palettes = Lists.newArrayList();
            SimplePalette mainPalette = new SimplePalette();
            palettes.add(mainPalette);
            for (int p = 1; p < this.palettes.size(); ++p) {
                palettes.add(new SimplePalette());
            }
            ListTag blockList = new ListTag();
            List<StructureBlockInfo> mainPaletteBlocks = this.palettes.get(0).blocks();
            for (int i = 0; i < mainPaletteBlocks.size(); ++i) {
                StructureBlockInfo blockInfo = mainPaletteBlocks.get(i);
                CompoundTag blockTag = new CompoundTag();
                blockTag.put("pos", this.newIntegerList(blockInfo.pos.getX(), blockInfo.pos.getY(), blockInfo.pos.getZ()));
                int id = mainPalette.idFor(blockInfo.state);
                blockTag.putInt(BLOCK_TAG_STATE, id);
                if (blockInfo.nbt != null) {
                    blockTag.put("nbt", blockInfo.nbt);
                }
                blockList.add(blockTag);
                for (int p = 1; p < this.palettes.size(); ++p) {
                    SimplePalette palette = (SimplePalette)palettes.get(p);
                    palette.addMapping(this.palettes.get((int)p).blocks().get((int)i).state, id);
                }
            }
            tag.put(BLOCKS_TAG, blockList);
            if (palettes.size() == 1) {
                ListTag paletteList = new ListTag();
                for (BlockState state : mainPalette) {
                    paletteList.add(NbtUtils.writeBlockState(state));
                }
                tag.put(PALETTE_TAG, paletteList);
            } else {
                ListTag paletteListList = new ListTag();
                for (SimplePalette palette : palettes) {
                    ListTag paletteList = new ListTag();
                    for (BlockState state : palette) {
                        paletteList.add(NbtUtils.writeBlockState(state));
                    }
                    paletteListList.add(paletteList);
                }
                tag.put(PALETTE_LIST_TAG, paletteListList);
            }
        }
        ListTag entityList = new ListTag();
        for (StructureEntityInfo entityInfo : this.entityInfoList) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.put("pos", this.newDoubleList(entityInfo.pos.x, entityInfo.pos.y, entityInfo.pos.z));
            entityTag.put(ENTITY_TAG_BLOCKPOS, this.newIntegerList(entityInfo.blockPos.getX(), entityInfo.blockPos.getY(), entityInfo.blockPos.getZ()));
            if (entityInfo.nbt != null) {
                entityTag.put("nbt", entityInfo.nbt);
            }
            entityList.add(entityTag);
        }
        tag.put(ENTITIES_TAG, entityList);
        tag.put(SIZE_TAG, this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        return NbtUtils.addCurrentDataVersion(tag);
    }

    public void load(HolderGetter<Block> blockLookup, CompoundTag tag) {
        this.palettes.clear();
        this.entityInfoList.clear();
        ListTag sizeTag = tag.getListOrEmpty(SIZE_TAG);
        this.size = new Vec3i(sizeTag.getIntOr(0, 0), sizeTag.getIntOr(1, 0), sizeTag.getIntOr(2, 0));
        ListTag blockList = tag.getListOrEmpty(BLOCKS_TAG);
        Optional<ListTag> paletteListList = tag.getList(PALETTE_LIST_TAG);
        if (paletteListList.isPresent()) {
            for (int p = 0; p < paletteListList.get().size(); ++p) {
                this.loadPalette(blockLookup, paletteListList.get().getListOrEmpty(p), blockList);
            }
        } else {
            this.loadPalette(blockLookup, tag.getListOrEmpty(PALETTE_TAG), blockList);
        }
        tag.getListOrEmpty(ENTITIES_TAG).compoundStream().forEach(entityTag -> {
            ListTag posTag = entityTag.getListOrEmpty("pos");
            Vec3 pos = new Vec3(posTag.getDoubleOr(0, 0.0), posTag.getDoubleOr(1, 0.0), posTag.getDoubleOr(2, 0.0));
            ListTag blockPosTag = entityTag.getListOrEmpty(ENTITY_TAG_BLOCKPOS);
            BlockPos blockPos = new BlockPos(blockPosTag.getIntOr(0, 0), blockPosTag.getIntOr(1, 0), blockPosTag.getIntOr(2, 0));
            entityTag.getCompound("nbt").ifPresent(nbt -> this.entityInfoList.add(new StructureEntityInfo(pos, blockPos, (CompoundTag)nbt)));
        });
    }

    private void loadPalette(HolderGetter<Block> blockLookup, ListTag paletteList, ListTag blockList) {
        SimplePalette palette = new SimplePalette();
        for (int i = 0; i < paletteList.size(); ++i) {
            palette.addMapping(NbtUtils.readBlockState(blockLookup, paletteList.getCompoundOrEmpty(i)), i);
        }
        ArrayList fullBlockList = Lists.newArrayList();
        ArrayList blockEntitiesList = Lists.newArrayList();
        ArrayList otherBlocksList = Lists.newArrayList();
        blockList.compoundStream().forEach(blockTag -> {
            ListTag posTag = blockTag.getListOrEmpty("pos");
            BlockPos pos = new BlockPos(posTag.getIntOr(0, 0), posTag.getIntOr(1, 0), posTag.getIntOr(2, 0));
            BlockState state = palette.stateFor(blockTag.getIntOr(BLOCK_TAG_STATE, 0));
            CompoundTag nbt = blockTag.getCompound("nbt").orElse(null);
            StructureBlockInfo info = new StructureBlockInfo(pos, state, nbt);
            StructureTemplate.addToLists(info, fullBlockList, blockEntitiesList, otherBlocksList);
        });
        List<StructureBlockInfo> blockInfoList = StructureTemplate.buildInfoList(fullBlockList, blockEntitiesList, otherBlocksList);
        this.palettes.add(new Palette(blockInfoList));
    }

    private ListTag newIntegerList(int ... values) {
        ListTag res = new ListTag();
        for (int value : values) {
            res.add(IntTag.valueOf(value));
        }
        return res;
    }

    private ListTag newDoubleList(double ... values) {
        ListTag res = new ListTag();
        for (double value : values) {
            res.add(DoubleTag.valueOf(value));
        }
        return res;
    }

    public static JigsawBlockEntity.JointType getJointType(CompoundTag nbt, BlockState state) {
        return nbt.read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> StructureTemplate.getDefaultJointType(state));
    }

    public static JigsawBlockEntity.JointType getDefaultJointType(BlockState state) {
        return JigsawBlock.getFrontFacing(state).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
    }

    public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static final class Palette {
        private final List<StructureBlockInfo> blocks;
        private final Map<Block, List<StructureBlockInfo>> cache = Maps.newHashMap();
        private @Nullable List<JigsawBlockInfo> cachedJigsaws;

        private Palette(List<StructureBlockInfo> blocks) {
            this.blocks = blocks;
        }

        public List<JigsawBlockInfo> jigsaws() {
            if (this.cachedJigsaws == null) {
                this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(JigsawBlockInfo::of).toList();
            }
            return this.cachedJigsaws;
        }

        public List<StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public List<StructureBlockInfo> blocks(Block filter) {
            return this.cache.computeIfAbsent(filter, block -> this.blocks.stream().filter(b -> b.state.is(block)).collect(Collectors.toList()));
        }
    }

    public static class StructureEntityInfo {
        public final Vec3 pos;
        public final BlockPos blockPos;
        public final CompoundTag nbt;

        public StructureEntityInfo(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }

    public record JigsawBlockInfo(StructureBlockInfo info, JigsawBlockEntity.JointType jointType, Identifier name, ResourceKey<StructureTemplatePool> pool, Identifier target, int placementPriority, int selectionPriority) {
        public static JigsawBlockInfo of(StructureBlockInfo info) {
            CompoundTag nbt = Objects.requireNonNull(info.nbt(), () -> String.valueOf(info) + " nbt was null");
            return new JigsawBlockInfo(info, StructureTemplate.getJointType(nbt, info.state()), nbt.read("name", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID), nbt.read("pool", JigsawBlockEntity.POOL_CODEC).orElse(Pools.EMPTY), nbt.read("target", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID), nbt.getIntOr("placement_priority", 0), nbt.getIntOr("selection_priority", 0));
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>", this.info.pos, this.info.state, this.name, this.pool.identifier(), this.target, this.placementPriority, this.selectionPriority, this.info.nbt);
        }

        public JigsawBlockInfo withInfo(StructureBlockInfo info) {
            return new JigsawBlockInfo(info, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority);
        }
    }

    private static class SimplePalette
    implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper(16);
        private int lastId;

        private SimplePalette() {
        }

        public int idFor(BlockState state) {
            int id = this.ids.getId(state);
            if (id == -1) {
                id = this.lastId++;
                this.ids.addMapping(state, id);
            }
            return id;
        }

        public @Nullable BlockState stateFor(int index) {
            BlockState blockState = this.ids.byId(index);
            return blockState == null ? DEFAULT_BLOCK_STATE : blockState;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState state, int id) {
            this.ids.addMapping(state, id);
        }
    }
}

