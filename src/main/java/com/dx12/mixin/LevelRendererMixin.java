package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import com.dx12.DX12LibClient;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
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

        System.out.println("[GL4DX12] LevelRendererMixin.onRenderLevel TRIGGERED!");

        if (D3D12Bridge.isD3D12Active() && levelRenderState != null) {
            ci.cancel();  // 暂时取消 OpenGL 渲染，测试 D3D12

            // ===== 反射检查 chunkSectionsToRender 结构 =====
            if (chunkSectionsToRender != null) {
                System.out.println("[GL4DX12] chunkSectionsToRender class: " + chunkSectionsToRender.getClass().getName());
                for (java.lang.reflect.Field field : chunkSectionsToRender.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    System.out.println("[GL4DX12]   field: " + field.getName() + " (" + field.getType().getName() + ")");
                    try {
                        Object value = field.get(chunkSectionsToRender);
                        System.out.println("[GL4DX12]     value: " + value);
                    } catch (Exception e) {
                        System.out.println("[GL4DX12]     value: <error>");
                    }
                }
            }

            // ===== 测试红色四边形（NDC坐标，coordType=2用单位矩阵） =====
            float[] quadVerts = {
                -0.8f, -0.8f, 0.0f,
                 0.8f, -0.8f, 0.0f,
                 0.8f,  0.8f, 0.0f,
                -0.8f,  0.8f, 0.0f
            };
            byte[] quadColors = new byte[16];
            for (int i = 0; i < 4; i++) {
                quadColors[i*4]     = (byte)255; // R
                quadColors[i*4 + 1] = 0;         // G
                quadColors[i*4 + 2] = 0;         // B
                quadColors[i*4 + 3] = (byte)255; // A
            }
            DX12LibClient.nativeRecordVertices(quadVerts, 4, quadColors, 2); // coordType=2 NDC
            System.out.println("[GL4DX12] BEFORE nativeDraw(4)");
            DX12LibClient.nativeDraw(4);
            System.out.println("[GL4DX12] AFTER nativeDraw(4)");
            DX12LibClient.nativePresent(); // 强制刷新

            // 调用 D3D12 渲染
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            D3D12Bridge.renderFullFrame(levelRenderState, cameraState, partialTick, modelViewMatrix);
        }
    }
}