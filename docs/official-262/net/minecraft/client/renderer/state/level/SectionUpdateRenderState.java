/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.state.level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;

@Environment(value=EnvType.CLIENT)
public record SectionUpdateRenderState(long sectionNode, boolean playerChanged, RenderSectionRegion region) {
}

