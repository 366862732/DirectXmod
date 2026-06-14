package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderLevelHead(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice terrainFog,
            Vector4f fogColor,
            boolean shouldRenderSky,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci
    ) {
        if (D3D12Bridge.isD3D12Active()) {
            ci.cancel();

            LevelRenderState levelState = getLevelRenderState((LevelRenderer)(Object)this);
            if (levelState != null) {
                D3D12Bridge.renderFullFrame(levelState, cameraState, deltaTracker.getGameTimeDeltaPartialTick(false));
            }
        }
    }

    private LevelRenderState getLevelRenderState(LevelRenderer renderer) {
        try {
            java.lang.reflect.Field field = LevelRenderer.class.getDeclaredField("levelRenderState");
            field.setAccessible(true);
            return (LevelRenderState) field.get(renderer);
        } catch (Exception e) {
            return null;
        }
    }
}