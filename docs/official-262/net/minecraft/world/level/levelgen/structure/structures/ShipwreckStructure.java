/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;

public class ShipwreckStructure
extends Structure {
    public static final MapCodec<ShipwreckStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ShipwreckStructure.settingsCodec(i), (App)Codec.BOOL.fieldOf("is_beached").forGetter(s -> s.isBeached)).apply((Applicative)i, ShipwreckStructure::new));
    public final boolean isBeached;

    public ShipwreckStructure(Structure.StructureSettings settings, boolean isBeached) {
        super(settings);
        this.isBeached = isBeached;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        Heightmap.Types type = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
        return ShipwreckStructure.onTopOfChunkCenter(context, type, builder -> this.generatePieces((StructurePiecesBuilder)builder, context));
    }

    private void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos offset = new BlockPos(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
        ShipwreckPieces.ShipwreckPiece piece = ShipwreckPieces.addRandomPiece(context.structureTemplateManager(), offset, rotation, builder, context.random(), this.isBeached);
        if (piece.isTooBigToFitInWorldGenRegion()) {
            int height;
            BoundingBox bb = piece.getBoundingBox();
            if (this.isBeached) {
                int minY = Structure.getLowestY(context, bb.minX(), bb.getXSpan(), bb.minZ(), bb.getZSpan());
                height = piece.calculateBeachedPosition(minY, context.random());
            } else {
                height = Structure.getMeanFirstOccupiedHeight(context, bb.minX(), bb.getXSpan(), bb.minZ(), bb.getZSpan());
            }
            piece.adjustPositionHeight(height);
        }
    }

    @Override
    public StructureType<?> type() {
        return StructureType.SHIPWRECK;
    }
}

