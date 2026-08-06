/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font.glyphs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.TextRenderable;

@Environment(value=EnvType.CLIENT)
public interface EffectGlyph {
    public TextRenderable createEffect(float var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8);
}

