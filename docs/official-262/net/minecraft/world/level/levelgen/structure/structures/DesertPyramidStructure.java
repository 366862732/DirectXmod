/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidStructure
extends SinglePieceStructure {
    public static final MapCodec<DesertPyramidStructure> CODEC = DesertPyramidStructure.simpleCodec(DesertPyramidStructure::new);

    public DesertPyramidStructure(Structure.StructureSettings settings) {
        super(DesertPyramidPiece::new, 21, 21, settings);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces) {
        SortedArraySet uniqueSandPlacements = SortedArraySet.create(Vec3i::compareTo);
        for (StructurePiece piece : pieces.pieces()) {
            if (!(piece instanceof DesertPyramidPiece)) continue;
            DesertPyramidPiece desertPyramidPiece = (DesertPyramidPiece)piece;
            uniqueSandPlacements.addAll(desertPyramidPiece.getPotentialSuspiciousSandWorldPositions());
            DesertPyramidStructure.placeSuspiciousSand(chunkBB, level, desertPyramidPiece.getRandomCollapsedRoofPos());
        }
        ObjectArrayList shuffledSandPlacements = new ObjectArrayList(uniqueSandPlacements.stream().toList());
        RandomSource positionalRandom = RandomSource.createThreadLocalInstance(level.getSeed()).forkPositional().at(pieces.calculateBoundingBox().getCenter());
        Util.shuffle(shuffledSandPlacements, positionalRandom);
        int suspiciousSandToPlace = Math.min(uniqueSandPlacements.size(), positionalRandom.nextInt(5, 8));
        for (BlockPos blockPos : shuffledSandPlacements) {
            if (suspiciousSandToPlace > 0) {
                --suspiciousSandToPlace;
                DesertPyramidStructure.placeSuspiciousSand(chunkBB, level, blockPos);
                continue;
            }
            if (!chunkBB.isInside(blockPos)) continue;
            level.setBlock(blockPos, Blocks.SAND.defaultBlockState(), 2);
        }
    }

    private static void placeSuspiciousSand(BoundingBox chunkBB, WorldGenLevel level, BlockPos blockPos) {
        if (chunkBB.isInside(blockPos)) {
            level.setBlock(blockPos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
            level.getBlockEntity(blockPos, BlockEntityTypes.BRUSHABLE_BLOCK).ifPresent(entity -> entity.setLootTable(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, blockPos.asLong()));
        }
    }

    @Override
    public StructureType<?> type() {
        return StructureType.DESERT_PYRAMID;
    }
}

