/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class HangingSignEditScreen
extends AbstractSignEditScreen {
    public static final float MAGIC_BACKGROUND_SCALE = 4.5f;
    private static final Vector3fc TEXT_SCALE = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final int TEXTURE_WIDTH = 16;
    private static final int TEXTURE_HEIGHT = 16;
    private final Identifier texture;

    public HangingSignEditScreen(SignBlockEntity sign, boolean isFrontText, boolean shouldFilter) {
        super(sign, isFrontText, shouldFilter, Component.translatable("hanging_sign.edit"));
        this.texture = Identifier.withDefaultNamespace("textures/gui/hanging_signs/" + this.woodType.name() + ".png");
    }

    @Override
    protected float getSignYOffset() {
        return 125.0f;
    }

    @Override
    protected void extractSignBackground(GuiGraphicsExtractor graphics) {
        graphics.pose().translate(0.0f, -13.0f);
        graphics.pose().scale(4.5f, 4.5f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, -8, -8, 0.0f, 0.0f, 16, 16, 16, 16);
    }

    @Override
    protected Vector3fc getSignTextScale() {
        return TEXT_SCALE;
    }
}

