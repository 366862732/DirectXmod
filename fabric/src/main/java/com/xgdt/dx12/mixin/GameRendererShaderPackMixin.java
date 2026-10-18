package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import com.xgdt.dx12.dx12.Dx12ShaderPackRenderer;
import com.xgdt.dx12.dx12.OfflineRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界几何渲染完成（{@code LevelRenderer.render} 返回）之后执行 HLSL 光影 pass 链。
 *
 * <p>插入点必须放在 {@code renderLevel} **内部**、{@code LevelRenderer.render} 调用之后：
 * 原版渲染完世界后会立刻把深度清零（给第一人称手部渲染用，见 {@code renderLevel}
 * 中 {@code clearDepthTexture} 调用）。若在 {@code renderLevel} 返回后才跑光影，
 * {@code scene_depth} 已经全是 0，天空 pass 会把整帧（包括地形）都当成天空重新绘制，
 * 地形"消失"。
 *
 * <p>此时机也早于手部渲染与 GUI，手和 HUD 不会被光影链二次处理。
 *
 * <p>{@code extractCamera} TAIL 处还挂了一个 ITRP 离线渲染的亚像素抖动（只在离线
 * 累积进行中生效）：必须赶在世界几何取走相机矩阵之前修改投影矩阵。放在提取相机
 * 状态之后（而不是 renderLevel 开头）是因为投影矩阵每次提取都会被整体重写
 * （{@code Camera.extractRenderState} 里 {@code projection.getMatrix(...)}），
 * 抖动不会叠加到上一帧残留的矩阵上。
 */
@Mixin(GameRenderer.class)
public class GameRendererShaderPackMixin {

    @Inject(method = "extractCamera", at = @At("TAIL"), remap = false)
    private void dx12_offlineRenderJitter(DeltaTracker deltaTracker, float worldPartialTicks,
                                          float cameraEntityPartialTicks, CallbackInfo ci) {
        OfflineRender.beginFrame();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            shift = At.Shift.AFTER
        ),
        remap = false
    )
    private void dx12_runShaderPack(CallbackInfo ci) {
        if (!Dx12Device.isInitialized()) {
            return;
        }
        Dx12ShaderPackRenderer.getInstance().renderFrame();
    }
}
