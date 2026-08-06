/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;

public class EndGatewayFeature
extends Feature<EndGatewayConfiguration> {
    public EndGatewayFeature(Codec<EndGatewayConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<EndGatewayConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        EndGatewayConfiguration config = context.config();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -2, -1), origin.offset(1, 2, 1))) {
            boolean end;
            boolean sameX = pos.getX() == origin.getX();
            boolean sameY = pos.getY() == origin.getY();
            boolean sameZ = pos.getZ() == origin.getZ();
            boolean bl = end = Math.abs(pos.getY() - origin.getY()) == 2;
            if (sameX && sameY && sameZ) {
                BlockPos immutable = pos.immutable();
                this.setBlock(level, immutable, Blocks.END_GATEWAY.defaultBlockState());
                config.getExit().ifPresent(targetPos -> {
                    BlockEntity exitEntity = level.getBlockEntity(immutable);
                    if (exitEntity instanceof TheEndGatewayBlockEntity) {
                        TheEndGatewayBlockEntity exitGateway = (TheEndGatewayBlockEntity)exitEntity;
                        exitGateway.setExitPosition((BlockPos)targetPos, config.isExitExact());
                    }
                });
                continue;
            }
            if (sameY) {
                this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
                continue;
            }
            if (end && sameX && sameZ) {
                this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
                continue;
            }
            if (!sameX && !sameZ || end) {
                this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
                continue;
            }
            this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
        }
        return true;
    }
}

