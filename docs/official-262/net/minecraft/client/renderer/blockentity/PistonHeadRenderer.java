/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PistonHeadRenderer
implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
    @Override
    public PistonHeadRenderState createRenderState() {
        return new PistonHeadRenderState();
    }

    @Override
    public void extractRenderState(PistonMovingBlockEntity blockEntity, PistonHeadRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.xOffset = blockEntity.getXOff(partialTicks);
        state.yOffset = blockEntity.getYOff(partialTicks);
        state.zOffset = blockEntity.getZOff(partialTicks);
        state.block = null;
        state.base = null;
        BlockState blockState = blockEntity.getMovedState();
        Level level = blockEntity.getLevel();
        if (level instanceof ClientLevel) {
            ClientLevel level2 = (ClientLevel)level;
            if (!blockState.isAir()) {
                BlockPos pos = blockEntity.getBlockPos().relative(blockEntity.getMovementDirection().getOpposite());
                Holder<Biome> biome = level2.getBiome(pos);
                if (blockState.is(Blocks.PISTON_HEAD) && blockEntity.getProgress(partialTicks) <= 4.0f) {
                    blockState = (BlockState)blockState.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTicks) <= 0.5f);
                    state.block = PistonHeadRenderer.createMovingBlock(pos, blockState, biome, level2);
                } else if (blockEntity.isSourcePiston() && !blockEntity.isExtending()) {
                    PistonType value = blockState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
                    BlockState pistonHeadState = (BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.TYPE, value)).setValue(PistonHeadBlock.FACING, (Direction)blockState.getValue(PistonBaseBlock.FACING));
                    pistonHeadState = (BlockState)pistonHeadState.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTicks) >= 0.5f);
                    state.block = PistonHeadRenderer.createMovingBlock(pos, pistonHeadState, biome, level2);
                    BlockPos basePos = pos.relative(blockEntity.getMovementDirection());
                    blockState = (BlockState)blockState.setValue(PistonBaseBlock.EXTENDED, true);
                    state.base = PistonHeadRenderer.createMovingBlock(basePos, blockState, biome, level2);
                } else {
                    state.block = PistonHeadRenderer.createMovingBlock(pos, blockState, biome, level2);
                }
            }
        }
    }

    @Override
    public void submit(PistonHeadRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.block == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(state.xOffset, state.yOffset, state.zOffset);
        submitNodeCollector.submitMovingBlock(poseStack, state.block, 0);
        poseStack.popPose();
        if (state.base != null) {
            submitNodeCollector.submitMovingBlock(poseStack, state.base, 0);
        }
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState blockState, Holder<Biome> biome, ClientLevel level) {
        MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
        movingBlockRenderState.randomSeedPos = pos;
        movingBlockRenderState.blockPos = pos;
        movingBlockRenderState.blockState = blockState;
        movingBlockRenderState.biome = biome;
        movingBlockRenderState.cardinalLighting = level.cardinalLighting();
        movingBlockRenderState.lightEngine = level.getLightEngine();
        return movingBlockRenderState;
    }

    @Override
    public int getViewDistance() {
        return 68;
    }
}

