package com.xgdt.dx12.mixin;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取方块图集里的全部 sprite，用来生成带 mip 的 LOD 图集副本。
 *
 * <p>{@link TextureAtlas} 没有公开遍历 sprite 的接口，但拼接完成后
 * {@code sprites} 列表里带着每张 sprite 在图集中的位置（{@code getX/getY}）和
 * 实际像素尺寸，正好足够把原图按同样的布局重新拼一遍。
 */
@Environment(EnvType.CLIENT)
@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {

    @Accessor("sprites")
    List<TextureAtlasSprite> gl4dx12$sprites();
}
