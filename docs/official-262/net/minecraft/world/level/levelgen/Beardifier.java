/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jspecify.annotations.Nullable;

public class Beardifier
implements DensityFunctions.BeardifierOrMarker {
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], kernel -> {
        for (int zi = 0; zi < 24; ++zi) {
            for (int xi = 0; xi < 24; ++xi) {
                for (int yi = 0; yi < 24; ++yi) {
                    kernel[zi * 24 * 24 + xi * 24 + yi] = (float)Beardifier.computeBeardContribution(xi - 12, yi - 12, zi - 12);
                }
            }
        }
    });
    public static final Beardifier EMPTY = new Beardifier(List.of(), List.of(), null);
    private final List<Rigid> pieces;
    private final List<JigsawJunction> junctions;
    private final @Nullable BoundingBox affectedBox;

    public static Beardifier forStructuresInChunk(StructureManager structureManager, ChunkPos chunkPos) {
        List<StructureStart> structureStarts = structureManager.startsForStructure(chunkPos, s -> s.terrainAdaptation() != TerrainAdjustment.NONE);
        if (structureStarts.isEmpty()) {
            return EMPTY;
        }
        int chunkStartBlockX = chunkPos.getMinBlockX();
        int chunkStartBlockZ = chunkPos.getMinBlockZ();
        ArrayList<Rigid> rigids = new ArrayList<Rigid>();
        ArrayList<JigsawJunction> junctions = new ArrayList<JigsawJunction>();
        BoundingBox anyPieceBoundingBox = null;
        for (StructureStart start : structureStarts) {
            TerrainAdjustment terrainAdjustment = start.getStructure().terrainAdaptation();
            for (StructurePiece piece : start.getPieces()) {
                if (!piece.isCloseToChunk(chunkPos, 12)) continue;
                if (piece instanceof PoolElementStructurePiece) {
                    PoolElementStructurePiece poolPiece = (PoolElementStructurePiece)piece;
                    StructureTemplatePool.Projection projection = poolPiece.getElement().getProjection();
                    if (projection == StructureTemplatePool.Projection.RIGID) {
                        rigids.add(new Rigid(poolPiece.getBoundingBox(), terrainAdjustment, poolPiece.getGroundLevelDelta()));
                        anyPieceBoundingBox = Beardifier.includeBoundingBox(anyPieceBoundingBox, piece.getBoundingBox());
                    }
                    for (JigsawJunction junction : poolPiece.getJunctions()) {
                        int junctionX = junction.getSourceX();
                        int junctionZ = junction.getSourceZ();
                        if (junctionX <= chunkStartBlockX - 12 || junctionZ <= chunkStartBlockZ - 12 || junctionX >= chunkStartBlockX + 15 + 12 || junctionZ >= chunkStartBlockZ + 15 + 12) continue;
                        junctions.add(junction);
                        BoundingBox junctionBox = new BoundingBox(new BlockPos(junctionX, junction.getSourceGroundY(), junctionZ));
                        anyPieceBoundingBox = Beardifier.includeBoundingBox(anyPieceBoundingBox, junctionBox);
                    }
                    continue;
                }
                rigids.add(new Rigid(piece.getBoundingBox(), terrainAdjustment, 0));
                anyPieceBoundingBox = Beardifier.includeBoundingBox(anyPieceBoundingBox, piece.getBoundingBox());
            }
        }
        if (anyPieceBoundingBox == null) {
            return EMPTY;
        }
        BoundingBox affectedBox = anyPieceBoundingBox.inflatedBy(24);
        return new Beardifier(List.copyOf(rigids), List.copyOf(junctions), affectedBox);
    }

    private static BoundingBox includeBoundingBox(@Nullable BoundingBox encompassingBox, BoundingBox newBox) {
        if (encompassingBox == null) {
            return newBox;
        }
        return BoundingBox.encapsulating(encompassingBox, newBox);
    }

    @VisibleForTesting
    public Beardifier(List<Rigid> pieces, List<JigsawJunction> junctions, @Nullable BoundingBox affectedBox) {
        this.pieces = pieces;
        this.junctions = junctions;
        this.affectedBox = affectedBox;
    }

    @Override
    public void fillArray(double[] output, DensityFunction.ContextProvider contextProvider) {
        if (this.affectedBox == null) {
            Arrays.fill(output, 0.0);
        } else {
            DensityFunctions.BeardifierOrMarker.super.fillArray(output, contextProvider);
        }
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        int blockZ;
        int blockY;
        if (this.affectedBox == null) {
            return 0.0;
        }
        int blockX = context.blockX();
        if (!this.affectedBox.isInside(blockX, blockY = context.blockY(), blockZ = context.blockZ())) {
            return 0.0;
        }
        double noiseValue = 0.0;
        for (Rigid rigid : this.pieces) {
            BoundingBox box = rigid.box();
            int groundLevelDelta = rigid.groundLevelDelta();
            int dx = Math.max(0, Math.max(box.minX() - blockX, blockX - box.maxX()));
            int dz = Math.max(0, Math.max(box.minZ() - blockZ, blockZ - box.maxZ()));
            int groundY = box.minY() + groundLevelDelta;
            int dyToGround = blockY - groundY;
            int dy = switch (rigid.terrainAdjustment()) {
                default -> throw new MatchException(null, null);
                case TerrainAdjustment.NONE -> 0;
                case TerrainAdjustment.BURY, TerrainAdjustment.BEARD_THIN -> dyToGround;
                case TerrainAdjustment.BEARD_BOX -> Math.max(0, Math.max(groundY - blockY, blockY - box.maxY()));
                case TerrainAdjustment.ENCAPSULATE -> Math.max(0, Math.max(box.minY() - blockY, blockY - box.maxY()));
            };
            noiseValue += (switch (rigid.terrainAdjustment()) {
                default -> throw new MatchException(null, null);
                case TerrainAdjustment.NONE -> 0.0;
                case TerrainAdjustment.BURY -> Beardifier.getBuryContribution(dx, (double)dy / 2.0, dz);
                case TerrainAdjustment.BEARD_THIN, TerrainAdjustment.BEARD_BOX -> Beardifier.getBeardContribution(dx, dy, dz, dyToGround) * 0.8;
                case TerrainAdjustment.ENCAPSULATE -> Beardifier.getBuryContribution((double)dx / 2.0, (double)dy / 2.0, (double)dz / 2.0) * 0.8;
            });
        }
        for (JigsawJunction junction : this.junctions) {
            int dx = blockX - junction.getSourceX();
            int dy = blockY - junction.getSourceGroundY();
            int dz = blockZ - junction.getSourceZ();
            noiseValue += Beardifier.getBeardContribution(dx, dy, dz, dy) * 0.4;
        }
        return noiseValue;
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    private static double getBuryContribution(double dx, double dy, double dz) {
        double distance = Mth.length(dx, dy, dz);
        return Mth.clampedMap(distance, 0.0, 6.0, 1.0, 0.0);
    }

    private static double getBeardContribution(int dx, int dy, int dz, int yToGround) {
        int xi = dx + 12;
        int yi = dy + 12;
        int zi = dz + 12;
        if (!(Beardifier.isInKernelRange(xi) && Beardifier.isInKernelRange(yi) && Beardifier.isInKernelRange(zi))) {
            return 0.0;
        }
        double dyWithOffset = (double)yToGround + 0.5;
        double distanceSqr = Mth.lengthSquared((double)dx, dyWithOffset, (double)dz);
        double value = -dyWithOffset * Mth.fastInvSqrt(distanceSqr / 2.0) / 2.0;
        return value * (double)BEARD_KERNEL[zi * 24 * 24 + xi * 24 + yi];
    }

    private static boolean isInKernelRange(int xi) {
        return xi >= 0 && xi < 24;
    }

    private static double computeBeardContribution(int dx, int dy, int dz) {
        return Beardifier.computeBeardContribution(dx, (double)dy + 0.5, dz);
    }

    private static double computeBeardContribution(int dx, double dy, int dz) {
        double distanceSqr = Mth.lengthSquared((double)dx, dy, (double)dz);
        double pieceWeight = Math.pow(Math.E, -distanceSqr / 16.0);
        return pieceWeight;
    }

    @VisibleForTesting
    public record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
    }
}

