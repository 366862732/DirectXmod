/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;

public class NetherFossilStructure
extends Structure {
    public static final MapCodec<NetherFossilStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(NetherFossilStructure.settingsCodec(i), (App)HeightProvider.CODEC.fieldOf("height").forGetter(c -> c.height)).apply((Applicative)i, NetherFossilStructure::new));
    public final HeightProvider height;

    public NetherFossilStructure(Structure.StructureSettings settings, HeightProvider height) {
        super(settings);
        this.height = height;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        WorldgenRandom random = context.random();
        int blockX = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int blockZ = context.chunkPos().getMinBlockZ() + random.nextInt(16);
        int seaLevel = context.chunkGenerator().getSeaLevel();
        WorldGenerationContext generationContext = new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor());
        int y = this.height.sample(random, generationContext);
        NoiseColumn column = context.chunkGenerator().getBaseColumn(blockX, blockZ, context.heightAccessor(), context.randomState());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockX, y, blockZ);
        while (y > seaLevel) {
            BlockState current = column.getBlock(y);
            BlockState below = column.getBlock(--y);
            if (!current.isAir() || !below.is(Blocks.SOUL_SAND) && !below.isFaceSturdy(EmptyBlockGetter.INSTANCE, pos.setY(y), Direction.UP)) continue;
            break;
        }
        if (y <= seaLevel) {
            return Optional.empty();
        }
        BlockPos position = new BlockPos(blockX, y, blockZ);
        return Optional.of(new Structure.GenerationStub(position, builder -> NetherFossilPieces.addPieces(context.structureTemplateManager(), builder, random, position)));
    }

    @Override
    public StructureType<?> type() {
        return StructureType.NETHER_FOSSIL;
    }
}

