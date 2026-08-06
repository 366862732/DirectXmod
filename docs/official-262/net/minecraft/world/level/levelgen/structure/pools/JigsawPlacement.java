/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JigsawPlacement {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int UNSET_HEIGHT = Integer.MIN_VALUE;

    public static Optional<Structure.GenerationStub> addPieces(Structure.GenerationContext context, Holder<StructureTemplatePool> startPool, Optional<Identifier> startJigsaw, int maxDepth, BlockPos position, boolean doExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, JigsawStructure.MaxDistance maxDistanceFromCenter, PoolAliasLookup poolAliasLookup, DimensionPadding dimensionPadding, LiquidSettings liquidSettings) {
        BlockPos anchoredPosition;
        RegistryAccess registryAccess = context.registryAccess();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        WorldgenRandom random = context.random();
        HolderLookup.RegistryLookup pools = registryAccess.lookupOrThrow(Registries.TEMPLATE_POOL);
        Rotation centerRotation = Rotation.getRandom(random);
        StructureTemplatePool centerPool = startPool.unwrapKey().flatMap(arg_0 -> JigsawPlacement.lambda$addPieces$0((Registry)pools, poolAliasLookup, arg_0)).orElse(startPool.value());
        StructurePoolElement centerElement = centerPool.getRandomTemplate(random);
        if (centerElement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        }
        if (startJigsaw.isPresent()) {
            Identifier targetJigsawId = startJigsaw.get();
            Optional<BlockPos> anchor = JigsawPlacement.getRandomNamedJigsaw(centerElement, targetJigsawId, position, centerRotation, structureTemplateManager, random);
            if (anchor.isEmpty()) {
                LOGGER.error("No starting jigsaw {} found in start pool {}", (Object)targetJigsawId, (Object)startPool.unwrapKey().map(key -> key.identifier().toString()).orElse("<unregistered>"));
                return Optional.empty();
            }
            anchoredPosition = anchor.get();
        } else {
            anchoredPosition = position;
        }
        BlockPos localAnchorPosition = anchoredPosition.subtract(position);
        BlockPos adjustedPosition = position.subtract(localAnchorPosition);
        PoolElementStructurePiece centerPiece = new PoolElementStructurePiece(structureTemplateManager, centerElement, adjustedPosition, centerElement.getGroundLevelDelta(), centerRotation, centerElement.getBoundingBox(structureTemplateManager, adjustedPosition, centerRotation), liquidSettings);
        BoundingBox box = centerPiece.getBoundingBox();
        int centerX = (box.maxX() + box.minX()) / 2;
        int centerZ = (box.maxZ() + box.minZ()) / 2;
        int bottomY = projectStartToHeightmap.isEmpty() ? adjustedPosition.getY() : position.getY() + chunkGenerator.getFirstFreeHeight(centerX, centerZ, projectStartToHeightmap.get(), heightAccessor, context.randomState());
        int oldAbsoluteGroundY = box.minY() + centerPiece.getGroundLevelDelta();
        centerPiece.move(0, bottomY - oldAbsoluteGroundY, 0);
        if (JigsawPlacement.isStartTooCloseToWorldHeightLimits(heightAccessor, dimensionPadding, centerPiece.getBoundingBox())) {
            LOGGER.debug("Center piece {} with bounding box {} does not fit dimension padding {}", new Object[]{centerElement, centerPiece.getBoundingBox(), dimensionPadding});
            return Optional.empty();
        }
        int centerY = bottomY + localAnchorPosition.getY();
        return Optional.of(new Structure.GenerationStub(new BlockPos(centerX, centerY, centerZ), arg_0 -> JigsawPlacement.lambda$addPieces$2(centerPiece, maxDepth, centerX, maxDistanceFromCenter, centerY, heightAccessor, dimensionPadding, centerZ, box, context, doExpansionHack, chunkGenerator, structureTemplateManager, random, (Registry)pools, poolAliasLookup, liquidSettings, arg_0)));
    }

    private static boolean isStartTooCloseToWorldHeightLimits(LevelHeightAccessor heightAccessor, DimensionPadding dimensionPadding, BoundingBox centerPieceBb) {
        if (dimensionPadding == DimensionPadding.ZERO) {
            return false;
        }
        int minYWithPadding = heightAccessor.getMinY() + dimensionPadding.bottom();
        int maxYWithPadding = heightAccessor.getMaxY() - dimensionPadding.top();
        return centerPieceBb.minY() < minYWithPadding || centerPieceBb.maxY() > maxYWithPadding;
    }

    private static Optional<BlockPos> getRandomNamedJigsaw(StructurePoolElement element, Identifier targetJigsawId, BlockPos position, Rotation rotation, StructureTemplateManager structureTemplateManager, WorldgenRandom random) {
        List<StructureTemplate.JigsawBlockInfo> jigsaws = element.getShuffledJigsawBlocks(structureTemplateManager, position, rotation, random);
        for (StructureTemplate.JigsawBlockInfo jigsaw : jigsaws) {
            if (!targetJigsawId.equals(jigsaw.name())) continue;
            return Optional.of(jigsaw.info().pos());
        }
        return Optional.empty();
    }

    private static void addPieces(RandomState randomState, int maxDepth, boolean doExpansionHack, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, LevelHeightAccessor heightAccessor, RandomSource random, Registry<StructureTemplatePool> pools, PoolElementStructurePiece centerPiece, List<PoolElementStructurePiece> pieces, VoxelShape shape, PoolAliasLookup poolAliasLookup, LiquidSettings liquidSettings) {
        Placer placer = new Placer(pools, maxDepth, chunkGenerator, structureTemplateManager, pieces, random);
        placer.tryPlacingChildren(centerPiece, (MutableObject<VoxelShape>)new MutableObject((Object)shape), 0, doExpansionHack, heightAccessor, randomState, poolAliasLookup, liquidSettings);
        while (placer.placing.hasNext()) {
            PieceState state = (PieceState)placer.placing.next();
            placer.tryPlacingChildren(state.piece, state.free, state.depth, doExpansionHack, heightAccessor, randomState, poolAliasLookup, liquidSettings);
        }
    }

    public static boolean generateJigsaw(ServerLevel level, Holder<StructureTemplatePool> pool, Identifier target, int maxDepth, BlockPos position, boolean keepJigsaws) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureTemplateManager structureTemplateManager = level.getStructureManager();
        StructureManager structureManager = level.structureManager();
        RandomSource random = level.getRandom();
        Structure.GenerationContext generationContext = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(), structureTemplateManager, level.getSeed(), ChunkPos.containing(position), level, b -> true);
        Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(generationContext, pool, Optional.of(target), maxDepth, position, false, Optional.empty(), new JigsawStructure.MaxDistance(128), PoolAliasLookup.EMPTY, JigsawStructure.DEFAULT_DIMENSION_PADDING, JigsawStructure.DEFAULT_LIQUID_SETTINGS);
        if (stub.isPresent()) {
            StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
            for (StructurePiece piece : builder.build().pieces()) {
                if (!(piece instanceof PoolElementStructurePiece)) continue;
                PoolElementStructurePiece poolPiece = (PoolElementStructurePiece)piece;
                poolPiece.place(level, structureManager, generator, random, BoundingBox.infinite(), position, keepJigsaws);
            }
            return true;
        }
        return false;
    }

    private static /* synthetic */ void lambda$addPieces$2(PoolElementStructurePiece centerPiece, int maxDepth, int centerX, JigsawStructure.MaxDistance maxDistanceFromCenter, int centerY, LevelHeightAccessor heightAccessor, DimensionPadding dimensionPadding, int centerZ, BoundingBox box, Structure.GenerationContext context, boolean doExpansionHack, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, WorldgenRandom random, Registry pools, PoolAliasLookup poolAliasLookup, LiquidSettings liquidSettings, StructurePiecesBuilder builder) {
        ArrayList pieces = Lists.newArrayList();
        pieces.add(centerPiece);
        if (maxDepth <= 0) {
            return;
        }
        AABB aabb = new AABB(centerX - maxDistanceFromCenter.horizontal(), Math.max(centerY - maxDistanceFromCenter.vertical(), heightAccessor.getMinY() + dimensionPadding.bottom()), centerZ - maxDistanceFromCenter.horizontal(), centerX + maxDistanceFromCenter.horizontal() + 1, Math.min(centerY + maxDistanceFromCenter.vertical() + 1, heightAccessor.getMaxY() + 1 - dimensionPadding.top()), centerZ + maxDistanceFromCenter.horizontal() + 1);
        VoxelShape shape = Shapes.join(Shapes.create(aabb), Shapes.create(AABB.of(box)), BooleanOp.ONLY_FIRST);
        JigsawPlacement.addPieces(context.randomState(), maxDepth, doExpansionHack, chunkGenerator, structureTemplateManager, heightAccessor, random, pools, centerPiece, pieces, shape, poolAliasLookup, liquidSettings);
        pieces.forEach(builder::addPiece);
    }

    private static /* synthetic */ Optional lambda$addPieces$0(Registry pools, PoolAliasLookup poolAliasLookup, ResourceKey key) {
        return pools.getOptional(poolAliasLookup.lookup(key));
    }

    private static final class Placer {
        private final Registry<StructureTemplatePool> pools;
        private final int maxDepth;
        private final ChunkGenerator chunkGenerator;
        private final StructureTemplateManager structureTemplateManager;
        private final List<? super PoolElementStructurePiece> pieces;
        private final RandomSource random;
        private final SequencedPriorityIterator<PieceState> placing = new SequencedPriorityIterator();

        private Placer(Registry<StructureTemplatePool> pools, int maxDepth, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, List<? super PoolElementStructurePiece> pieces, RandomSource random) {
            this.pools = pools;
            this.maxDepth = maxDepth;
            this.chunkGenerator = chunkGenerator;
            this.structureTemplateManager = structureTemplateManager;
            this.pieces = pieces;
            this.random = random;
        }

        private void tryPlacingChildren(PoolElementStructurePiece sourcePiece, MutableObject<VoxelShape> contextFree, int depth, boolean doExpansionHack, LevelHeightAccessor heightAccessor, RandomState randomState, PoolAliasLookup poolAliasLookup, LiquidSettings liquidSettings) {
            StructurePoolElement sourceElement = sourcePiece.getElement();
            BlockPos sourceBoxPosition = sourcePiece.getPosition();
            Rotation sourceRotation = sourcePiece.getRotation();
            StructureTemplatePool.Projection sourceProjection = sourceElement.getProjection();
            boolean sourceRigid = sourceProjection == StructureTemplatePool.Projection.RIGID;
            MutableObject<@Nullable VoxelShape> sourceFree = new MutableObject<VoxelShape>();
            BoundingBox sourceBB = sourcePiece.getBoundingBox();
            int sourceBoxY = sourceBB.minY();
            block0: for (StructureTemplate.JigsawBlockInfo sourceJigsaw : sourceElement.getShuffledJigsawBlocks(this.structureTemplateManager, sourceBoxPosition, sourceRotation, this.random)) {
                StructurePoolElement targetElement;
                MutableObject<VoxelShape> childrenFree;
                StructureTemplate.StructureBlockInfo sourceJigsawInfo = sourceJigsaw.info();
                Direction sourceDirection = JigsawBlock.getFrontFacing(sourceJigsawInfo.state());
                BlockPos sourceJigsawPos = sourceJigsawInfo.pos();
                BlockPos targetJigsawPos = sourceJigsawPos.relative(sourceDirection);
                int sourceJigsawLocalY = sourceJigsawPos.getY() - sourceBoxY;
                int sourceJigsawBaseHeight = Integer.MIN_VALUE;
                ResourceKey<StructureTemplatePool> poolName = poolAliasLookup.lookup(sourceJigsaw.pool());
                Optional maybeTargetPool = this.pools.get(poolName);
                if (maybeTargetPool.isEmpty()) {
                    LOGGER.warn("Empty or non-existent pool: {}", (Object)poolName.identifier());
                    continue;
                }
                Holder targetPool = (Holder)maybeTargetPool.get();
                if (((StructureTemplatePool)targetPool.value()).size() == 0 && !targetPool.is(Pools.EMPTY)) {
                    LOGGER.warn("Empty or non-existent pool: {}", (Object)poolName.identifier());
                    continue;
                }
                Holder<StructureTemplatePool> fallback = ((StructureTemplatePool)targetPool.value()).getFallback();
                if (fallback.value().size() == 0 && !fallback.is(Pools.EMPTY)) {
                    LOGGER.warn("Empty or non-existent fallback pool: {}", (Object)fallback.unwrapKey().map(e -> e.identifier().toString()).orElse("<unregistered>"));
                    continue;
                }
                boolean attachInsideSource = sourceBB.isInside(targetJigsawPos);
                if (attachInsideSource) {
                    childrenFree = sourceFree;
                    if (sourceFree.get() == null) {
                        sourceFree.setValue((Object)Shapes.create(AABB.of(sourceBB)));
                    }
                } else {
                    childrenFree = contextFree;
                }
                ArrayList targetPieces = Lists.newArrayList();
                if (depth != this.maxDepth) {
                    targetPieces.addAll(((StructureTemplatePool)targetPool.value()).getShuffledTemplates(this.random));
                }
                targetPieces.addAll(fallback.value().getShuffledTemplates(this.random));
                int placementPriority = sourceJigsaw.placementPriority();
                Iterator iterator = targetPieces.iterator();
                while (iterator.hasNext() && (targetElement = (StructurePoolElement)iterator.next()) != EmptyPoolElement.INSTANCE) {
                    for (Rotation targetRotation : Rotation.getShuffled(this.random)) {
                        List<StructureTemplate.JigsawBlockInfo> targetJigsaws = targetElement.getShuffledJigsawBlocks(this.structureTemplateManager, BlockPos.ZERO, targetRotation, this.random);
                        BoundingBox hackBox = targetElement.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, targetRotation);
                        int expandTo = !doExpansionHack || hackBox.getYSpan() > 16 ? 0 : targetJigsaws.stream().mapToInt(targetJigsaw -> {
                            StructureTemplate.StructureBlockInfo targetJigsawInfo = targetJigsaw.info();
                            if (!hackBox.isInside(targetJigsawInfo.pos().relative(JigsawBlock.getFrontFacing(targetJigsawInfo.state())))) {
                                return 0;
                            }
                            ResourceKey<StructureTemplatePool> childPoolName = poolAliasLookup.lookup(targetJigsaw.pool());
                            Optional childPool = this.pools.get(childPoolName);
                            Optional<Holder> childFallbackPool = childPool.map(p -> ((StructureTemplatePool)p.value()).getFallback());
                            int childPoolSize = childPool.map(p -> ((StructureTemplatePool)p.value()).getMaxSize(this.structureTemplateManager)).orElse(0);
                            int childFallbackSize = childFallbackPool.map(p -> ((StructureTemplatePool)p.value()).getMaxSize(this.structureTemplateManager)).orElse(0);
                            return Math.max(childPoolSize, childFallbackSize);
                        }).max().orElse(0);
                        for (StructureTemplate.JigsawBlockInfo targetJigsaw2 : targetJigsaws) {
                            int junctionY;
                            int targetBoxY;
                            if (!JigsawBlock.canAttach(sourceJigsaw, targetJigsaw2)) continue;
                            BlockPos targetJigsawLocalPos = targetJigsaw2.info().pos();
                            BlockPos rawTargetBoxPos = targetJigsawPos.subtract(targetJigsawLocalPos);
                            BoundingBox rawTargetBB = targetElement.getBoundingBox(this.structureTemplateManager, rawTargetBoxPos, targetRotation);
                            int rawTargetY = rawTargetBB.minY();
                            StructureTemplatePool.Projection targetProjection = targetElement.getProjection();
                            boolean targetRigid = targetProjection == StructureTemplatePool.Projection.RIGID;
                            int targetJigsawLocalY = targetJigsawLocalPos.getY();
                            int deltaY = sourceJigsawLocalY - targetJigsawLocalY + JigsawBlock.getFrontFacing(sourceJigsawInfo.state()).getStepY();
                            if (sourceRigid && targetRigid) {
                                targetBoxY = sourceBoxY + deltaY;
                            } else {
                                if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                    sourceJigsawBaseHeight = this.chunkGenerator.getFirstFreeHeight(sourceJigsawPos.getX(), sourceJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
                                }
                                targetBoxY = sourceJigsawBaseHeight - targetJigsawLocalY;
                            }
                            int yOffset = targetBoxY - rawTargetY;
                            BoundingBox targetBB = rawTargetBB.moved(0, yOffset, 0);
                            BlockPos targetBoxPosition = rawTargetBoxPos.offset(0, yOffset, 0);
                            if (expandTo > 0) {
                                int newSize = Math.max(expandTo + 1, targetBB.maxY() - targetBB.minY());
                                targetBB.encapsulate(new BlockPos(targetBB.minX(), targetBB.minY() + newSize, targetBB.minZ()));
                            }
                            if (Shapes.joinIsNotEmpty((VoxelShape)childrenFree.get(), Shapes.create(AABB.of(targetBB).deflate(0.25)), BooleanOp.ONLY_SECOND)) continue;
                            childrenFree.setValue((Object)Shapes.joinUnoptimized((VoxelShape)childrenFree.get(), Shapes.create(AABB.of(targetBB)), BooleanOp.ONLY_FIRST));
                            int sourceGroundLevelDelta = sourcePiece.getGroundLevelDelta();
                            int targetGroundLevelDelta = targetRigid ? sourceGroundLevelDelta - deltaY : targetElement.getGroundLevelDelta();
                            PoolElementStructurePiece targetPiece = new PoolElementStructurePiece(this.structureTemplateManager, targetElement, targetBoxPosition, targetGroundLevelDelta, targetRotation, targetBB, liquidSettings);
                            if (sourceRigid) {
                                junctionY = sourceBoxY + sourceJigsawLocalY;
                            } else if (targetRigid) {
                                junctionY = targetBoxY + targetJigsawLocalY;
                            } else {
                                if (sourceJigsawBaseHeight == Integer.MIN_VALUE) {
                                    sourceJigsawBaseHeight = this.chunkGenerator.getFirstFreeHeight(sourceJigsawPos.getX(), sourceJigsawPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
                                }
                                junctionY = sourceJigsawBaseHeight + deltaY / 2;
                            }
                            sourcePiece.addJunction(new JigsawJunction(targetJigsawPos.getX(), junctionY - sourceJigsawLocalY + sourceGroundLevelDelta, targetJigsawPos.getZ(), deltaY, targetProjection));
                            targetPiece.addJunction(new JigsawJunction(sourceJigsawPos.getX(), junctionY - targetJigsawLocalY + targetGroundLevelDelta, sourceJigsawPos.getZ(), -deltaY, sourceProjection));
                            this.pieces.add(targetPiece);
                            if (depth + 1 > this.maxDepth) continue block0;
                            PieceState state = new PieceState(targetPiece, childrenFree, depth + 1);
                            this.placing.add(state, placementPriority);
                            continue block0;
                        }
                    }
                }
            }
        }
    }

    private record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
    }
}

