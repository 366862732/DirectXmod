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

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.List;

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

            // 提取 ChunkSectionsToRender 中的 Draw 数据
            if (chunkSectionsToRender != null) {
                extractAndRenderChunks(chunkSectionsToRender);
            }

            // 调用 D3D12 渲染
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            D3D12Bridge.renderFullFrame(levelRenderState, cameraState, partialTick, modelViewMatrix);
        }
    }

    private void extractAndRenderChunks(ChunkSectionsToRender sections) {
        if (sections == null) return;

        try {
            // 获取 drawGroupsPerLayer 字段
            Field drawGroupsField = sections.getClass().getDeclaredField("drawGroupsPerLayer");
            drawGroupsField.setAccessible(true);
            EnumMap<?, ?> drawGroups = (EnumMap<?, ?>) drawGroupsField.get(sections);

            // 遍历每个层 (SOLID, CUTOUT, TRANSLUCENT)
            for (Object layer : drawGroups.keySet()) {
                Object drawList = drawGroups.get(layer);
                if (drawList == null) continue;

                // 获取 Draw 列表
                if (drawList instanceof List) {
                    List<?> draws = (List<?>) drawList;
                    System.out.println("[GL4DX12] Layer " + layer + " has " + draws.size() + " draws");

                    for (Object draw : draws) {
                        // 提取每个 Draw 的顶点数据
                        processDraw(draw);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processDraw(Object draw) {
        try {
            // 获取 vertexBuffer
            Field vbField = draw.getClass().getDeclaredField("vertexBuffer");
            vbField.setAccessible(true);
            Object vertexBuffer = vbField.get(draw);

            // 获取 indexCount 和 baseVertex
            Field indexCountField = draw.getClass().getDeclaredField("indexCount");
            indexCountField.setAccessible(true);
            int indexCount = (int) indexCountField.get(draw);

            Field baseVertexField = draw.getClass().getDeclaredField("baseVertex");
            baseVertexField.setAccessible(true);
            int baseVertex = (int) baseVertexField.get(draw);

            System.out.println("[GL4DX12] Draw: indexCount=" + indexCount + ", baseVertex=" + baseVertex);

            // TODO: 从 vertexBuffer 中提取实际顶点数据
            // 这需要根据 Minecraft 的顶点格式解析
            // 暂时跳过，先打印日志确认流程

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}