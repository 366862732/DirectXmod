package com.xgdt.dx11.perf.immediate.sign;

import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;

/**
 * Value key for one rasterised sign side.
 *
 * <p>{@link SignText#equals(Object)} is identity based, so it cannot be used in
 * a cache that should share a slot between two physically different signs with
 * the same text.  This key mirrors the fields that influence the rasterised
 * pixels instead: both the raw and the filtered messages (filtered text is
 * player-specific), dye colour, glow, whether filtering is active, and the
 * line metrics used by the renderer.  Draw-distance outline visibility is not
 * part of the key because the atlas always renders the outline, matching the
 * reference implementation.</p>
 */
public final class SignTextKey {
    private final Component[] messages;
    private final Component[] filteredMessages;
    private final DyeColor color;
    private final boolean glowing;
    private final boolean filtering;
    private final int lineHeight;
    private final int maxLineWidth;
    private final int hash;

    public SignTextKey(
        Component[] messages,
        Component[] filteredMessages,
        DyeColor color,
        boolean glowing,
        boolean filtering,
        int lineHeight,
        int maxLineWidth) {
        this.messages = messages;
        this.filteredMessages = filteredMessages;
        this.color = color;
        this.glowing = glowing;
        this.filtering = filtering;
        this.lineHeight = lineHeight;
        this.maxLineWidth = maxLineWidth;

        int result = Boolean.hashCode(glowing);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + Boolean.hashCode(filtering);
        result = 31 * result + lineHeight;
        result = 31 * result + maxLineWidth;
        result = 31 * result + Arrays.hashCode(messages);
        result = 31 * result + Arrays.hashCode(filteredMessages);
        this.hash = result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SignTextKey key)) {
            return false;
        }
        return this.glowing == key.glowing
            && this.filtering == key.filtering
            && this.lineHeight == key.lineHeight
            && this.maxLineWidth == key.maxLineWidth
            && this.color == key.color
            && Arrays.equals(this.messages, key.messages)
            && Arrays.equals(this.filteredMessages, key.filteredMessages);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
