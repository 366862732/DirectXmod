/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;

public class StrongholdStructure
extends Structure {
    public static final MapCodec<StrongholdStructure> CODEC = StrongholdStructure.simpleCodec(StrongholdStructure::new);

    public StrongholdStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        return Optional.of(new Structure.GenerationStub(context.chunkPos().getWorldPosition(), builder -> StrongholdStructure.generatePieces(builder, context)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        StrongholdPieces.StartPiece startRoom;
        int tries = 0;
        do {
            builder.clear();
            context.random().setLargeFeatureSeed(context.seed() + (long)tries++, context.chunkPos().x(), context.chunkPos().z());
            StrongholdPieces.resetPieces();
            startRoom = new StrongholdPieces.StartPiece(context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2));
            builder.addPiece(startRoom);
            startRoom.addChildren(startRoom, builder, context.random());
            List<StructurePiece> pendingChildren = startRoom.pendingChildren;
            while (!pendingChildren.isEmpty()) {
                int pos = context.random().nextInt(pendingChildren.size());
                StructurePiece structurePiece = pendingChildren.remove(pos);
                structurePiece.addChildren(startRoom, builder, context.random());
            }
            builder.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), context.chunkGenerator().getMinY(), context.random(), 10);
        } while (builder.isEmpty() || startRoom.portalRoomPiece == null);
    }

    @Override
    public StructureType<?> type() {
        return StructureType.STRONGHOLD;
    }
}

