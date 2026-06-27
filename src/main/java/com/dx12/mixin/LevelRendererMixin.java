package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private LevelRenderState levelRenderState;

    @Inject(method = "renderLevel",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderLevel(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice terrainFog,
            Vector4f fogColor,
            boolean shouldRenderSky,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci) {

        if (!D3D12Bridge.isD3D12Active()) return;

        // Cancel OpenGL rendering — D3D12 renders the frame
        ci.cancel();

        // Delegate full frame to D3D12 bridge
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        D3D12Bridge.renderFullFrame(levelRenderState, cameraState, partialTick, modelViewMatrix);
    }
}
