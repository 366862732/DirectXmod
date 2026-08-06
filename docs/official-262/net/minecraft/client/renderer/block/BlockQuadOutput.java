/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface BlockQuadOutput {
    public void put(float var1, float var2, float var3, BakedQuad var4, QuadInstance var5);
}

