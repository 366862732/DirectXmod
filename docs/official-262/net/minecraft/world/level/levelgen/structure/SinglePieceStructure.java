/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen.structure;

import java.util.Optional;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public abstract class SinglePieceStructure
extends Structure {
    private final PieceConstructor constructor;
    private final int width;
    private final int depth;

    protected SinglePieceStructure(PieceConstructor constructor, int width, int depth, Structure.StructureSettings settings) {
        super(settings);
        this.constructor = constructor;
        this.width = width;
        this.depth = depth;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        if (SinglePieceStructure.getLowestY(context, this.width, this.depth) < context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        return SinglePieceStructure.onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> this.generatePieces((StructurePiecesBuilder)builder, context));
    }

    private void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        builder.addPiece(this.constructor.construct(context.random(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ()));
    }

    @FunctionalInterface
    protected static interface PieceConstructor {
        public StructurePiece construct(WorldgenRandom var1, int var2, int var3);
    }
}

