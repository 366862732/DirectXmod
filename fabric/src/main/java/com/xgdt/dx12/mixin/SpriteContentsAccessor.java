package com.xgdt.dx12.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 {@link SpriteContents} 的原始 PNG 像素。
 *
 * <p>原版方块图集在当前配置下只有 mip0（日志 {@code mips=1}），LOD 远处采样不到
 * 粗 mip，贴图会缩成噪点并整片闪烁。这里把每张 sprite 的原始图片借出来，CPU 生成
 * 一条带 mip 的 LOD 专用图集；布局与原图集完全一致，所以 mesher 里现成的 UV 矩形
 * 不需要改。
 */
@Environment(EnvType.CLIENT)
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("originalImage")
    NativeImage gl4dx12$originalImage();
}
