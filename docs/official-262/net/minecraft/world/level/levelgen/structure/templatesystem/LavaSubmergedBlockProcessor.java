/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public class LavaSubmergedBlockProcessor
implements StructureProcessor {
    public static final MapCodec<LavaSubmergedBlockProcessor> MAP_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final LavaSubmergedBlockProcessor INSTANCE = new LavaSubmergedBlockProcessor();

    @Override
    public  @Nullable StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition, BlockPos referencePos, BlockPos templateRelativePos, StructureTemplate.StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings) {
        BlockPos pos = processedBlockInfo.pos();
        boolean wasLavaBefore = level.getBlockState(pos).is(Blocks.LAVA);
        if (wasLavaBefore && !Block.isShapeFullBlock(processedBlockInfo.state().getShape(level, pos))) {
            return new StructureTemplate.StructureBlockInfo(pos, Blocks.LAVA.defaultBlockState(), processedBlockInfo.nbt());
        }
        return processedBlockInfo;
    }

    public MapCodec<LavaSubmergedBlockProcessor> codec() {
        return MAP_CODEC;
    }
}

