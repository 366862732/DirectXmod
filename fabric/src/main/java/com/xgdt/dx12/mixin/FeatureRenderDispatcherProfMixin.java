package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12RenderProf;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * P52：实体/方块实体提交（feature）准备路径耗时（排序 + prepare + 共享顶点缓冲上传）。
 * 官方 Vulkan 下这块同样是 Java 代码，用于对比"提交侧"是否被我们的后端拖慢。
 */
@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherProfMixin {

    @Inject(method = "prepareFrame", at = @At("HEAD"), remap = false)
    private void dx12_profPrepareFeaturesHead(SubmitNodeStorage submitNodeStorage,
                                              CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        Dx12RenderProf.begin(Dx12RenderProf.PREPARE_FEATURES);
    }

    @Inject(method = "prepareFrame", at = @At("RETURN"), remap = false)
    private void dx12_profPrepareFeaturesReturn(SubmitNodeStorage submitNodeStorage,
                                                CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        Dx12RenderProf.end(Dx12RenderProf.PREPARE_FEATURES);
    }
}
