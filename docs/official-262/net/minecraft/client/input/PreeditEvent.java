/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package net.minecraft.client.input;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.nio.IntBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public record PreeditEvent(String fullText, int caretPosition, List<String> blocks, int focusedBlock) {
    public PreeditEvent {
        Preconditions.checkElementIndex((int)focusedBlock, (int)blocks.size());
    }

    public static @Nullable PreeditEvent createFromCallback(int preeditSize, long preeditPtr, int blockCount, long blockSizesPtr, int focusedBlock, int caret) {
        if (preeditSize == 0) {
            return null;
        }
        int[] codepoints = PreeditEvent.readIntBuffer(preeditSize, preeditPtr);
        int[] blockSizes = PreeditEvent.readIntBuffer(blockCount, blockSizesPtr);
        StringBuilder fullText = new StringBuilder();
        ImmutableList.Builder blocks = ImmutableList.builder();
        int offset = 0;
        int convertedCaret = 0;
        for (int blockSize : blockSizes) {
            StringBuilder blockBuilder = new StringBuilder();
            for (int i = 0; i < blockSize; ++i) {
                int codepoint = codepoints[offset];
                if (offset == caret) {
                    convertedCaret = fullText.length() + blockBuilder.length();
                }
                blockBuilder.appendCodePoint(codepoint);
                ++offset;
            }
            String block = blockBuilder.toString();
            blocks.add((Object)block);
            fullText.append(block);
        }
        if (offset == caret) {
            convertedCaret = fullText.length();
        }
        return new PreeditEvent(fullText.toString(), convertedCaret, (List<String>)blocks.build(), focusedBlock);
    }

    private static int[] readIntBuffer(int size, long ptr) {
        IntBuffer buffer = MemoryUtil.memIntBuffer((long)ptr, (int)size);
        int[] result = new int[size];
        buffer.get(result);
        return result;
    }

    public MutableComponent toFormattedText(Style focusedStyle) {
        int blockCount = this.blocks.size();
        if (blockCount == 1) {
            return Component.literal(this.blocks.getFirst()).withStyle(focusedStyle);
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < blockCount; ++i) {
            MutableComponent part = Component.literal(this.blocks.get(i));
            if (i == this.focusedBlock) {
                part.withStyle(focusedStyle);
            }
            result.append(part);
        }
        return result;
    }
}

