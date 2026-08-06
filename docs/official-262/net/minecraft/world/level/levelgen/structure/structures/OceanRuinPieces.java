/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.OceanRuinStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class OceanRuinPieces {
    private static final StructureProcessor WARM_SUSPICIOUS_BLOCK_PROCESSOR = OceanRuinPieces.archyRuleProcessor(Blocks.SAND, Blocks.SUSPICIOUS_SAND, BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY);
    private static final StructureProcessor COLD_SUSPICIOUS_BLOCK_PROCESSOR = OceanRuinPieces.archyRuleProcessor(Blocks.GRAVEL, Blocks.SUSPICIOUS_GRAVEL, BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY);
    private static final Identifier[] WARM_RUINS = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/warm_1"), Identifier.withDefaultNamespace("underwater_ruin/warm_2"), Identifier.withDefaultNamespace("underwater_ruin/warm_3"), Identifier.withDefaultNamespace("underwater_ruin/warm_4"), Identifier.withDefaultNamespace("underwater_ruin/warm_5"), Identifier.withDefaultNamespace("underwater_ruin/warm_6"), Identifier.withDefaultNamespace("underwater_ruin/warm_7"), Identifier.withDefaultNamespace("underwater_ruin/warm_8")};
    private static final Identifier[] RUINS_BRICK = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/brick_1"), Identifier.withDefaultNamespace("underwater_ruin/brick_2"), Identifier.withDefaultNamespace("underwater_ruin/brick_3"), Identifier.withDefaultNamespace("underwater_ruin/brick_4"), Identifier.withDefaultNamespace("underwater_ruin/brick_5"), Identifier.withDefaultNamespace("underwater_ruin/brick_6"), Identifier.withDefaultNamespace("underwater_ruin/brick_7"), Identifier.withDefaultNamespace("underwater_ruin/brick_8")};
    private static final Identifier[] RUINS_CRACKED = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/cracked_1"), Identifier.withDefaultNamespace("underwater_ruin/cracked_2"), Identifier.withDefaultNamespace("underwater_ruin/cracked_3"), Identifier.withDefaultNamespace("underwater_ruin/cracked_4"), Identifier.withDefaultNamespace("underwater_ruin/cracked_5"), Identifier.withDefaultNamespace("underwater_ruin/cracked_6"), Identifier.withDefaultNamespace("underwater_ruin/cracked_7"), Identifier.withDefaultNamespace("underwater_ruin/cracked_8")};
    private static final Identifier[] RUINS_MOSSY = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/mossy_1"), Identifier.withDefaultNamespace("underwater_ruin/mossy_2"), Identifier.withDefaultNamespace("underwater_ruin/mossy_3"), Identifier.withDefaultNamespace("underwater_ruin/mossy_4"), Identifier.withDefaultNamespace("underwater_ruin/mossy_5"), Identifier.withDefaultNamespace("underwater_ruin/mossy_6"), Identifier.withDefaultNamespace("underwater_ruin/mossy_7"), Identifier.withDefaultNamespace("underwater_ruin/mossy_8")};
    private static final Identifier[] BIG_RUINS_BRICK = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/big_brick_1"), Identifier.withDefaultNamespace("underwater_ruin/big_brick_2"), Identifier.withDefaultNamespace("underwater_ruin/big_brick_3"), Identifier.withDefaultNamespace("underwater_ruin/big_brick_8")};
    private static final Identifier[] BIG_RUINS_MOSSY = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/big_mossy_1"), Identifier.withDefaultNamespace("underwater_ruin/big_mossy_2"), Identifier.withDefaultNamespace("underwater_ruin/big_mossy_3"), Identifier.withDefaultNamespace("underwater_ruin/big_mossy_8")};
    private static final Identifier[] BIG_RUINS_CRACKED = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/big_cracked_1"), Identifier.withDefaultNamespace("underwater_ruin/big_cracked_2"), Identifier.withDefaultNamespace("underwater_ruin/big_cracked_3"), Identifier.withDefaultNamespace("underwater_ruin/big_cracked_8")};
    private static final Identifier[] BIG_WARM_RUINS = new Identifier[]{Identifier.withDefaultNamespace("underwater_ruin/big_warm_4"), Identifier.withDefaultNamespace("underwater_ruin/big_warm_5"), Identifier.withDefaultNamespace("underwater_ruin/big_warm_6"), Identifier.withDefaultNamespace("underwater_ruin/big_warm_7")};

    private static StructureProcessor archyRuleProcessor(Block candidateBlock, Block replacementBlock, ResourceKey<LootTable> lootTable) {
        return new CappedProcessor(new RuleProcessor(List.of(new ProcessorRule(new BlockMatchTest(candidateBlock), AlwaysTrueTest.INSTANCE, PosAlwaysTrueTest.INSTANCE, replacementBlock.defaultBlockState(), new AppendLoot(lootTable)))), ConstantInt.of(5));
    }

    private static Identifier getSmallWarmRuin(RandomSource random) {
        return Util.getRandom(WARM_RUINS, random);
    }

    private static Identifier getBigWarmRuin(RandomSource random) {
        return Util.getRandom(BIG_WARM_RUINS, random);
    }

    public static void addPieces(StructureTemplateManager structureTemplateManager, BlockPos position, Rotation rotation, StructurePieceAccessor structurePieceAccessor, RandomSource random, OceanRuinStructure structure) {
        boolean isLarge = random.nextFloat() <= structure.largeProbability;
        float baseIntegrity = isLarge ? 0.9f : 0.8f;
        OceanRuinPieces.addPiece(structureTemplateManager, position, rotation, structurePieceAccessor, random, structure, isLarge, baseIntegrity);
        if (isLarge && random.nextFloat() <= structure.clusterProbability) {
            OceanRuinPieces.addClusterRuins(structureTemplateManager, random, rotation, position, structure, structurePieceAccessor);
        }
    }

    private static void addClusterRuins(StructureTemplateManager structureTemplateManager, RandomSource random, Rotation rotation, BlockPos p, OceanRuinStructure structure, StructurePieceAccessor structurePieceAccessor) {
        BlockPos parentPos = new BlockPos(p.getX(), 90, p.getZ());
        BlockPos parentCorner = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, rotation, BlockPos.ZERO).offset(parentPos);
        BoundingBox parentBB = BoundingBox.fromCorners(parentPos, parentCorner);
        BlockPos parentBottomLeft = new BlockPos(Math.min(parentPos.getX(), parentCorner.getX()), parentPos.getY(), Math.min(parentPos.getZ(), parentCorner.getZ()));
        List<BlockPos> allPositions = OceanRuinPieces.allPositions(random, parentBottomLeft);
        int ruins = Mth.nextInt(random, 4, 8);
        for (int i = 0; i < ruins; ++i) {
            Rotation nextRotation;
            BlockPos nextCorner;
            int idx;
            BlockPos pos;
            BoundingBox nextBB;
            if (allPositions.isEmpty() || (nextBB = BoundingBox.fromCorners(pos = allPositions.remove(idx = random.nextInt(allPositions.size())), nextCorner = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, nextRotation = Rotation.getRandom(random), BlockPos.ZERO).offset(pos))).intersects(parentBB)) continue;
            OceanRuinPieces.addPiece(structureTemplateManager, pos, nextRotation, structurePieceAccessor, random, structure, false, 0.8f);
        }
    }

    private static List<BlockPos> allPositions(RandomSource random, BlockPos origin) {
        ArrayList positions = Lists.newArrayList();
        positions.add(origin.offset(-16 + Mth.nextInt(random, 1, 8), 0, 16 + Mth.nextInt(random, 1, 7)));
        positions.add(origin.offset(-16 + Mth.nextInt(random, 1, 8), 0, Mth.nextInt(random, 1, 7)));
        positions.add(origin.offset(-16 + Mth.nextInt(random, 1, 8), 0, -16 + Mth.nextInt(random, 4, 8)));
        positions.add(origin.offset(Mth.nextInt(random, 1, 7), 0, 16 + Mth.nextInt(random, 1, 7)));
        positions.add(origin.offset(Mth.nextInt(random, 1, 7), 0, -16 + Mth.nextInt(random, 4, 6)));
        positions.add(origin.offset(16 + Mth.nextInt(random, 1, 7), 0, 16 + Mth.nextInt(random, 3, 8)));
        positions.add(origin.offset(16 + Mth.nextInt(random, 1, 7), 0, Mth.nextInt(random, 1, 7)));
        positions.add(origin.offset(16 + Mth.nextInt(random, 1, 7), 0, -16 + Mth.nextInt(random, 4, 8)));
        return positions;
    }

    private static void addPiece(StructureTemplateManager structureTemplateManager, BlockPos position, Rotation rotation, StructurePieceAccessor structurePieceAccessor, RandomSource random, OceanRuinStructure structure, boolean isLarge, float baseIntegrity) {
        switch (structure.biomeTemp) {
            default: {
                Identifier startPieceLocation = isLarge ? OceanRuinPieces.getBigWarmRuin(random) : OceanRuinPieces.getSmallWarmRuin(random);
                structurePieceAccessor.addPiece(new OceanRuinPiece(structureTemplateManager, startPieceLocation, position, rotation, baseIntegrity, structure.biomeTemp, isLarge));
                break;
            }
            case COLD: {
                Identifier[] bricks = isLarge ? BIG_RUINS_BRICK : RUINS_BRICK;
                Identifier[] cracked = isLarge ? BIG_RUINS_CRACKED : RUINS_CRACKED;
                Identifier[] mossy = isLarge ? BIG_RUINS_MOSSY : RUINS_MOSSY;
                int idx = random.nextInt(bricks.length);
                structurePieceAccessor.addPiece(new OceanRuinPiece(structureTemplateManager, bricks[idx], position, rotation, baseIntegrity, structure.biomeTemp, isLarge));
                structurePieceAccessor.addPiece(new OceanRuinPiece(structureTemplateManager, cracked[idx], position, rotation, 0.7f, structure.biomeTemp, isLarge));
                structurePieceAccessor.addPiece(new OceanRuinPiece(structureTemplateManager, mossy[idx], position, rotation, 0.5f, structure.biomeTemp, isLarge));
            }
        }
    }

    public static class OceanRuinPiece
    extends TemplateStructurePiece {
        private final OceanRuinStructure.Type biomeType;
        private final float integrity;
        private final boolean isLarge;

        public OceanRuinPiece(StructureTemplateManager structureTemplateManager, Identifier templateLocation, BlockPos position, Rotation rotation, float integrity, OceanRuinStructure.Type biomeType, boolean isLarge) {
            super(StructurePieceType.OCEAN_RUIN, 0, structureTemplateManager, templateLocation, templateLocation.toString(), OceanRuinPiece.makeSettings(rotation, integrity, biomeType), position);
            this.integrity = integrity;
            this.biomeType = biomeType;
            this.isLarge = isLarge;
        }

        private OceanRuinPiece(StructureTemplateManager structureTemplateManager, CompoundTag tag, Rotation rotation, float integrity, OceanRuinStructure.Type biomeType, boolean isLarge) {
            super(StructurePieceType.OCEAN_RUIN, tag, structureTemplateManager, location -> OceanRuinPiece.makeSettings(rotation, integrity, biomeType));
            this.integrity = integrity;
            this.biomeType = biomeType;
            this.isLarge = isLarge;
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation, float integrity, OceanRuinStructure.Type biomeType) {
            StructureProcessor suspiciousBlockProcessor = biomeType == OceanRuinStructure.Type.COLD ? COLD_SUSPICIOUS_BLOCK_PROCESSOR : WARM_SUSPICIOUS_BLOCK_PROCESSOR;
            return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).addProcessor(new BlockRotProcessor(integrity)).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR).addProcessor(suspiciousBlockProcessor);
        }

        public static OceanRuinPiece create(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
            Rotation rotation = tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow();
            float integrity = tag.getFloatOr("Integrity", 0.0f);
            OceanRuinStructure.Type biomeType = tag.read("BiomeType", OceanRuinStructure.Type.LEGACY_CODEC).orElseThrow();
            boolean isLarge = tag.getBooleanOr("IsLarge", false);
            return new OceanRuinPiece(structureTemplateManager, tag, rotation, integrity, biomeType, isLarge);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
            tag.putFloat("Integrity", this.integrity);
            tag.store("BiomeType", OceanRuinStructure.Type.LEGACY_CODEC, this.biomeType);
            tag.putBoolean("IsLarge", this.isLarge);
        }

        @Override
        protected void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
            Drowned drowned;
            if ("chest".equals(markerId)) {
                level.setBlock(position, (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, level.getFluidState(position).is(FluidTags.WATER)), 2);
                BlockEntity chest = level.getBlockEntity(position);
                if (chest instanceof ChestBlockEntity) {
                    ChestBlockEntity chestBlockEntity = (ChestBlockEntity)chest;
                    chestBlockEntity.setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, random.nextLong());
                }
            } else if ("drowned".equals(markerId) && (drowned = EntityTypes.DROWNED.create((Level)level.getLevel(), EntitySpawnReason.STRUCTURE)) != null) {
                drowned.setPersistenceRequired();
                drowned.snapTo(position, 0.0f, 0.0f);
                drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(position), EntitySpawnReason.STRUCTURE, null);
                level.addFreshEntityWithPassengers(drowned);
                if (position.getY() > level.getSeaLevel()) {
                    level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
                } else {
                    level.setBlock(position, Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
            this.templatePosition = new BlockPos(this.templatePosition.getX(), height, this.templatePosition.getZ());
            BlockPos corner = StructureTemplate.transform(new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1), Mirror.NONE, this.placeSettings.getRotation(), BlockPos.ZERO).offset(this.templatePosition);
            this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, level, corner), this.templatePosition.getZ());
            super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
        }

        private int getHeight(BlockPos pos, BlockGetter level, BlockPos corner) {
            int newY = pos.getY();
            int minY = 512;
            int topY = newY - 1;
            int area = 0;
            for (BlockPos p : BlockPos.betweenClosed(pos, corner)) {
                int x = p.getX();
                int z = p.getZ();
                int floorY = pos.getY() - 1;
                BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos(x, floorY, z);
                BlockState tempState = level.getBlockState(tempPos);
                FluidState tempFluid = level.getFluidState(tempPos);
                while ((tempState.isAir() || tempFluid.is(FluidTags.WATER) || tempState.is(BlockTags.ICE)) && floorY > level.getMinY() + 1) {
                    tempPos.set(x, --floorY, z);
                    tempState = level.getBlockState(tempPos);
                    tempFluid = level.getFluidState(tempPos);
                }
                minY = Math.min(minY, floorY);
                if (floorY >= topY - 2) continue;
                ++area;
            }
            int width = Math.abs(pos.getX() - corner.getX());
            if (topY - minY > 2 && area > width - 2) {
                newY = minY + 1;
            }
            return newY;
        }
    }
}

