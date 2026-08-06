/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;

public class BuriedTreasureStructure
extends Structure {
    public static final MapCodec<BuriedTreasureStructure> CODEC = BuriedTreasureStructure.simpleCodec(BuriedTreasureStructure::new);

    public BuriedTreasureStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        return BuriedTreasureStructure.onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG, builder -> BuriedTreasureStructure.generatePieces(builder, context));
    }

    private static void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        BlockPos offset = new BlockPos(context.chunkPos().getBlockX(9), 90, context.chunkPos().getBlockZ(9));
        builder.addPiece(new BuriedTreasurePieces.BuriedTreasurePiece(offset));
    }

    @Override
    public StructureType<?> type() {
        return StructureType.BURIED_TREASURE;
    }
}

