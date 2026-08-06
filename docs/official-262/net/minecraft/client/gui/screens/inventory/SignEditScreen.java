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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class SignEditScreen
extends AbstractSignEditScreen {
    public static final float MAGIC_BACKGROUND_SCALE = 3.9f;
    public static final float MAGIC_TEXT_SCALE = 0.9765628f;
    private static final int TEXTURE_WIDTH = 24;
    private static final int TEXTURE_HEIGHT = 26;
    private static final int POST_HEIGHT = 14;
    private static final Vector3fc TEXT_SCALE = new Vector3f(0.9765628f, 0.9765628f, 0.9765628f);
    private final int displayedHeight;
    private final Identifier texture;

    public SignEditScreen(SignBlockEntity sign, boolean isFrontText, boolean shouldFilter) {
        super(sign, isFrontText, shouldFilter);
        this.texture = Identifier.withDefaultNamespace("textures/gui/signs/" + this.woodType.name() + ".png");
        boolean isWallSign = PlainSignBlock.getAttachmentPoint(sign.getBlockState()) == PlainSignBlock.Attachment.WALL;
        this.displayedHeight = isWallSign ? 12 : 26;
    }

    @Override
    protected float getSignYOffset() {
        return 90.0f;
    }

    @Override
    protected void extractSignBackground(GuiGraphicsExtractor graphics) {
        graphics.pose().translate(0.0f, 27.0f);
        graphics.pose().scale(3.9f, 3.9f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, -12, -13, 0.0f, 0.0f, 24, this.displayedHeight, 24, 26);
    }

    @Override
    protected Vector3fc getSignTextScale() {
        return TEXT_SCALE;
    }
}

