package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Matrix4fc;
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
            Object terrainFog,  // GpuBufferSlice, 用 Object 避免编译依赖
            Object fogColor,     // Vector4f
            boolean shouldRenderSky,
            Object chunkSectionsToRender,  // ChunkSectionsToRender
            CallbackInfo ci
    ) {
        if (D3D12Bridge.isD3D12Active()) {
            // 阻断原渲染
            ci.cancel();

            // 获取 LevelRenderState (通过反射或直接访问)
            // 由于 Mixin 无法直接访问私有字段，我们通过 getter 或反射
            LevelRenderState levelState = getLevelRenderState((LevelRenderer)(Object)this);

            if (levelState != null) {
                // 渲染完整 D3D12 帧
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
            e.printStackTrace();
            return null;
        }
    }
}