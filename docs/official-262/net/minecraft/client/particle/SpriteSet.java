/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public interface SpriteSet {
    public TextureAtlasSprite get(int var1, int var2);

    public TextureAtlasSprite get(RandomSource var1);

    public TextureAtlasSprite first();
}

