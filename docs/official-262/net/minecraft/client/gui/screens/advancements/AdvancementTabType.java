/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.advancements;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@Environment(value=EnvType.CLIENT)
public enum AdvancementTabType {
    ABOVE(new Sprites(Identifier.withDefaultNamespace("advancements/tab_above_left_selected"), Identifier.withDefaultNamespace("advancements/tab_above_middle_selected"), Identifier.withDefaultNamespace("advancements/tab_above_right_selected")), new Sprites(Identifier.withDefaultNamespace("advancements/tab_above_left"), Identifier.withDefaultNamespace("advancements/tab_above_middle"), Identifier.withDefaultNamespace("advancements/tab_above_right")), 28, 32, 8),
    BELOW(new Sprites(Identifier.withDefaultNamespace("advancements/tab_below_left_selected"), Identifier.withDefaultNamespace("advancements/tab_below_middle_selected"), Identifier.withDefaultNamespace("advancements/tab_below_right_selected")), new Sprites(Identifier.withDefaultNamespace("advancements/tab_below_left"), Identifier.withDefaultNamespace("advancements/tab_below_middle"), Identifier.withDefaultNamespace("advancements/tab_below_right")), 28, 32, 8),
    LEFT(new Sprites(Identifier.withDefaultNamespace("advancements/tab_left_top_selected"), Identifier.withDefaultNamespace("advancements/tab_left_middle_selected"), Identifier.withDefaultNamespace("advancements/tab_left_bottom_selected")), new Sprites(Identifier.withDefaultNamespace("advancements/tab_left_top"), Identifier.withDefaultNamespace("advancements/tab_left_middle"), Identifier.withDefaultNamespace("advancements/tab_left_bottom")), 32, 28, 5),
    RIGHT(new Sprites(Identifier.withDefaultNamespace("advancements/tab_right_top_selected"), Identifier.withDefaultNamespace("advancements/tab_right_middle_selected"), Identifier.withDefaultNamespace("advancements/tab_right_bottom_selected")), new Sprites(Identifier.withDefaultNamespace("advancements/tab_right_top"), Identifier.withDefaultNamespace("advancements/tab_right_middle"), Identifier.withDefaultNamespace("advancements/tab_right_bottom")), 32, 28, 5);

    private final Sprites selectedSprites;
    private final Sprites unselectedSprites;
    private final int width;
    private final int height;
    private final int max;

    private AdvancementTabType(Sprites selectedSprites, Sprites unselectedSprites, int width, int height, int max) {
        this.selectedSprites = selectedSprites;
        this.unselectedSprites = unselectedSprites;
        this.width = width;
        this.height = height;
        this.max = max;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getMax() {
        return this.max;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int tabX, int tabY, boolean selected, int index) {
        Sprites sprites;
        Sprites sprites2 = sprites = selected ? this.selectedSprites : this.unselectedSprites;
        Identifier sprite = index == 0 ? sprites.first() : (index == this.max - 1 ? sprites.last() : sprites.middle());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tabX, tabY, this.width, this.height);
    }

    public void extractIcon(GuiGraphicsExtractor graphics, int xo, int yo, int index, ItemStack icon) {
        int x = xo + this.getX(index);
        int y = yo + this.getY(index);
        switch (this.ordinal()) {
            case 0: {
                x += 6;
                y += 9;
                break;
            }
            case 1: {
                x += 6;
                y += 6;
                break;
            }
            case 2: {
                x += 10;
                y += 5;
                break;
            }
            case 3: {
                x += 6;
                y += 5;
            }
        }
        graphics.fakeItem(icon, x, y);
    }

    public int getX(int index) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> (this.width + 4) * index;
            case 1 -> (this.width + 4) * index;
            case 2 -> -this.width + 4;
            case 3 -> 248;
        };
    }

    public int getY(int index) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> -this.height + 4;
            case 1 -> 136;
            case 2 -> this.height * index;
            case 3 -> this.height * index;
        };
    }

    public boolean isMouseOver(int xo, int yo, int index, double mx, double my) {
        int x = xo + this.getX(index);
        int y = yo + this.getY(index);
        return mx > (double)x && mx < (double)(x + this.width) && my > (double)y && my < (double)(y + this.height);
    }

    @Environment(value=EnvType.CLIENT)
    private record Sprites(Identifier first, Identifier middle, Identifier last) {
    }
}

