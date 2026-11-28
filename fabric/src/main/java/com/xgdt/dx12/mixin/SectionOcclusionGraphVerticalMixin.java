package com.xgdt.dx12.mixin;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 放宽区块遮挡图的<b>垂直</b>遍历范围。
 *
 * <p>原版 {@code SectionOcclusionGraph.getRelativeFrom} 在向上/下找邻居 section 时，
 * 要求两个 section 的 Y 差不超过 {@link ViewArea#getViewDistance()}（视距，单位区块）。
 * 视距 2 时只有 2 个 section（32 格）：飞到高空后脚下地表在几十个 section 之外，
 * 整片真区块因此不会进入可见图，画面中间留下一个方洞。
 *
 * <p>这里只把 {@code getRelativeFrom} 里那次 {@code getViewDistance()} 的返回值放大，
 * 水平方向仍由 {@code isInViewDistance} 按原视距裁剪；遮挡图和视锥照常剔除不可见
 * section，所以不会把整个世界的 section 都画出来。放宽到 256 个 section（4096 格）
 * 足以覆盖玩家飞到世界高度以上的情况。
 */
@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphVerticalMixin {

    @Redirect(
        method = "getRelativeFrom",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ViewArea;getViewDistance()I"
        ),
        remap = false
    )
    private int dx12$extendVerticalViewDistance(ViewArea viewArea) {
        return Math.max(viewArea.getViewDistance(), 256);
    }
}
