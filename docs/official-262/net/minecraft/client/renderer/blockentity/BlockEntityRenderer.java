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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {
    public S createRenderState();

    default public void extractRenderState(T blockEntity, S state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    public void submit(S var1, PoseStack var2, SubmitNodeCollector var3, CameraRenderState var4);

    default public boolean shouldRenderOffScreen() {
        return false;
    }

    default public int getViewDistance() {
        return 64;
    }

    default public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        return Vec3.atCenterOf(((BlockEntity)blockEntity).getBlockPos()).closerThan(cameraPosition, this.getViewDistance());
    }
}

