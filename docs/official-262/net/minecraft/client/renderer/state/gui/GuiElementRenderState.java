/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface GuiElementRenderState
extends ScreenArea {
    public void buildVertices(VertexConsumer var1);

    public RenderPipeline pipeline();

    public TextureSetup textureSetup();

    public @Nullable ScreenRectangle scissorArea();
}

