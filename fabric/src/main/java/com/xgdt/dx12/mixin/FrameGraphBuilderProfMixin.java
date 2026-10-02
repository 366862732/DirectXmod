package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.xgdt.dx12.dx12.Dx12RenderProf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P52：整帧 FrameGraph 执行（所有 pass 体，含 renderGroup / feature 绘制 / 天空 / 云 / 天气）。
 * 与 P51 的 renderGroup 相减可得到"非区块"部分（实体、天空、云、天气、帧图开销）。
 */
@Mixin(FrameGraphBuilder.class)
public class FrameGraphBuilderProfMixin {

    /** 必须写明完整描述符：FrameGraphBuilder 有两个 execute 重载，仅写方法名会产生歧义。 */
    @Inject(method = "execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
            at = @At("HEAD"), remap = false)
    private void dx12_profExecFrameGraphHead(GraphicsResourceAllocator resourceAllocator,
                                             FrameGraphBuilder.Inspector inspector, CallbackInfo ci) {
        Dx12RenderProf.begin(Dx12RenderProf.EXECUTE_FRAMEGRAPH);
    }

    @Inject(method = "execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
            at = @At("RETURN"), remap = false)
    private void dx12_profExecFrameGraphReturn(GraphicsResourceAllocator resourceAllocator,
                                               FrameGraphBuilder.Inspector inspector, CallbackInfo ci) {
        Dx12RenderProf.end(Dx12RenderProf.EXECUTE_FRAMEGRAPH);
    }
}
