package com.xgdt.dx12.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版远平面只按视距算（{@code max(renderDistance*16*4, cloudRange*16)}），飞到高空后
 * 地面距离超过远平面会被直接裁掉，表现就是「开了 LOD 反而真区块消失」。
 *
 * <p>在 {@code Camera.update} 给 {@code depthFar} 赋值之后，把它至少扩到「相机到世界
 * 底部的距离 + 余量」。后续的视锥裁剪和投影矩阵都读这个字段，因此原版区块在高空仍会
 * 正常渲染；LOD 的深度映射也直接用 {@code camera.depthFar}，接缝深度会自动对齐。
 *
 * <p>只加不减：低空时原值更大，保持原版行为不变。
 */
@Mixin(Camera.class)
public class CameraFarPlaneMixin {

    @Shadow
    private float depthFar;

    @Inject(
        method = "update",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Camera;depthFar:F",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
        ),
        remap = false
    )
    private void dx12$extendDepthFarForAltitude(DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        // 相机到世界底部的高度 + 256 方块余量；地面再远也不会被远平面裁掉。
        float required = (float) (minecraft.player.getY() - level.getMinY()) + 256.0f;
        if (required > this.depthFar) {
            this.depthFar = required;
        }
    }
}
