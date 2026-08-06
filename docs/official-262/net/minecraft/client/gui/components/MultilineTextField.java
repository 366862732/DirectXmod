/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.components;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class MultilineTextField {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_LIMIT = Integer.MAX_VALUE;
    private static final int LINE_SEEK_PIXEL_BIAS = 2;
    private final Font font;
    private final List<StringView> displayLines = Lists.newArrayList();
    private String value;
    private int cursor;
    private int selectCursor;
    private boolean selecting;
    private int characterLimit = Integer.MAX_VALUE;
    private int lineLimit = Integer.MAX_VALUE;
    private final int width;
    private Consumer<String> valueListener = s -> {};
    private Runnable cursorListener = () -> {};

    public MultilineTextField(Font font, int width) {
        this.font = font;
        this.width = width;
        this.setValue("");
    }

    public int characterLimit() {
        return this.characterLimit;
    }

    public void setCharacterLimit(int characterLimit) {
        if (characterLimit < 0) {
            throw new IllegalArgumentException("Character limit cannot be negative");
        }
        this.characterLimit = characterLimit;
    }

    public void setLineLimit(int lineLimit) {
        if (lineLimit < 0) {
            throw new IllegalArgumentException("Character limit cannot be negative");
        }
        this.lineLimit = lineLimit;
    }

    public boolean hasCharacterLimit() {
        return this.characterLimit != Integer.MAX_VALUE;
    }

    public boolean hasLineLimit() {
        return this.lineLimit != Integer.MAX_VALUE;
    }

    public void setValueListener(Consumer<String> valueListener) {
        this.valueListener = valueListener;
    }

    public void setCursorListener(Runnable cursorListener) {
        this.cursorListener = cursorListener;
    }

    public void setValue(String value) {
        this.setValue(value, false);
    }

    public void setValue(String value, boolean allowOverflowLineLimit) {
        String newValue = this.truncateFullText(value);
        if (!allowOverflowLineLimit && this.overflowsLineLimit(newValue)) {
            return;
        }
        this.value = newValue;
        this.selectCursor = this.cursor = this.value.length();
        this.onValueChange();
    }

    public String value() {
        return this.value;
    }

    public void insertText(String input) {
        if (input.isEmpty() && !this.hasSelection()) {
            return;
        }
        String text = this.truncateInsertionText(StringUtil.filterText(input, true));
        StringView selected = this.getSelected();
        String newValue = new StringBuilder(this.value).replace(selected.beginIndex, selected.endIndex, text).toString();
        if (this.overflowsLineLimit(newValue)) {
            return;
        }
        this.value = newValue;
        this.selectCursor = this.cursor = selected.beginIndex + text.length();
        this.onValueChange();
    }

    public void deleteText(int dir) {
        if (!this.hasSelection()) {
            this.selectCursor = Mth.clamp(this.cursor + dir, 0, this.value.length());
        }
        this.insertText("");
    }

    public int cursor() {
        return this.cursor;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    public StringView getSelected() {
        return new StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
    }

    public int getLineCount() {
        return this.displayLines.size();
    }

    public int getLineAtCursor() {
        for (int i = 0; i < this.displayLines.size(); ++i) {
            StringView view = this.displayLines.get(i);
            if (this.cursor < view.beginIndex || this.cursor > view.endIndex) continue;
            return i;
        }
        return -1;
    }

    public StringView getLineView(int lineIndex) {
        return this.displayLines.get(Mth.clamp(lineIndex, 0, this.displayLines.size() - 1));
    }

    public void seekCursor(Whence whence, int cursor) {
        switch (whence) {
            case ABSOLUTE: {
                this.cursor = cursor;
                break;
            }
            case RELATIVE: {
                this.cursor += cursor;
                break;
            }
            case END: {
                this.cursor = this.value.length() + cursor;
            }
        }
        this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
        this.cursorListener.run();
        if (!this.selecting) {
            this.selectCursor = this.cursor;
        }
    }

    public void seekCursorLine(int lineOffset) {
        if (lineOffset == 0) {
            return;
        }
        int oldCursorLeft = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
        StringView lineView = this.getCursorLineView(lineOffset);
        int newCursor = this.font.plainSubstrByWidth(this.value.substring(lineView.beginIndex, lineView.endIndex), oldCursorLeft).length();
        this.seekCursor(Whence.ABSOLUTE, lineView.beginIndex + newCursor);
    }

    public void seekCursorToPoint(double x, double y) {
        int left = Mth.floor(x);
        int top = Mth.floor(y / (double)this.font.lineHeight);
        StringView lineView = this.displayLines.get(Mth.clamp(top, 0, this.displayLines.size() - 1));
        int clickedColumn = this.font.plainSubstrByWidth(this.value.substring(lineView.beginIndex, lineView.endIndex), left).length();
        this.seekCursor(Whence.ABSOLUTE, lineView.beginIndex + clickedColumn);
    }

    public void selectWordAtCursor() {
        StringView wordView = this.getPreviousWord();
        this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
        this.setSelecting(true);
        this.seekCursor(Whence.ABSOLUTE, wordView.endIndex);
    }

    public boolean keyPressed(KeyEvent event) {
        this.selecting = event.hasShiftDown();
        if (event.isSelectAll()) {
            this.cursor = this.value.length();
            this.selectCursor = 0;
            return true;
        }
        if (event.isCopy()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            return true;
        }
        if (event.isPaste()) {
            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        if (event.isCut()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
            this.insertText("");
            return true;
        }
        switch (event.key()) {
            case 263: {
                if (event.hasControlDownWithQuirk()) {
                    StringView wordView = this.getPreviousWord();
                    this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
                } else {
                    this.seekCursor(Whence.RELATIVE, -1);
                }
                return true;
            }
            case 262: {
                if (event.hasControlDownWithQuirk()) {
                    StringView wordView = this.getNextWord();
                    this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
                } else {
                    this.seekCursor(Whence.RELATIVE, 1);
                }
                return true;
            }
            case 265: {
                if (!event.hasControlDownWithQuirk()) {
                    this.seekCursorLine(-1);
                }
                return true;
            }
            case 264: {
                if (!event.hasControlDownWithQuirk()) {
                    this.seekCursorLine(1);
                }
                return true;
            }
            case 266: {
                this.seekCursor(Whence.ABSOLUTE, 0);
                return true;
            }
            case 267: {
                this.seekCursor(Whence.END, 0);
                return true;
            }
            case 268: {
                if (event.hasControlDownWithQuirk()) {
                    this.seekCursor(Whence.ABSOLUTE, 0);
                } else {
                    this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
                }
                return true;
            }
            case 269: {
                if (event.hasControlDownWithQuirk()) {
                    this.seekCursor(Whence.END, 0);
                } else {
                    this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
                }
                return true;
            }
            case 259: {
                if (event.hasControlDownWithQuirk()) {
                    StringView wordView = this.getPreviousWord();
                    this.deleteText(wordView.beginIndex - this.cursor);
                } else {
                    this.deleteText(-1);
                }
                return true;
            }
            case 261: {
                if (event.hasControlDownWithQuirk()) {
                    StringView wordView = this.getNextWord();
                    this.deleteText(wordView.beginIndex - this.cursor);
                } else {
                    this.deleteText(1);
                }
                return true;
            }
            case 257: 
            case 335: {
                this.insertText("\n");
                return true;
            }
        }
        return false;
    }

    public Iterable<StringView> iterateLines() {
        return this.displayLines;
    }

    public boolean hasSelection() {
        return this.selectCursor != this.cursor;
    }

    @VisibleForTesting
    public String getSelectedText() {
        StringView selected = this.getSelected();
        return this.value.substring(selected.beginIndex, selected.endIndex);
    }

    private StringView getCursorLineView() {
        return this.getCursorLineView(0);
    }

    private StringView getCursorLineView(int lineOffset) {
        int lineIndex = this.getLineAtCursor();
        if (lineIndex < 0) {
            LOGGER.error("Cursor is not within text (cursor = {}, length = {})", (Object)this.cursor, (Object)this.value.length());
            return this.displayLines.getLast();
        }
        return this.displayLines.get(Mth.clamp(lineIndex + lineOffset, 0, this.displayLines.size() - 1));
    }

    @VisibleForTesting
    public StringView getPreviousWord() {
        int startPosition;
        if (this.value.isEmpty()) {
            return StringView.EMPTY;
        }
        for (startPosition = Mth.clamp(this.cursor, 0, this.value.length() - 1); startPosition > 0 && Character.isWhitespace(this.value.charAt(startPosition - 1)); --startPosition) {
        }
        while (startPosition > 0 && !Character.isWhitespace(this.value.charAt(startPosition - 1))) {
            --startPosition;
        }
        return new StringView(startPosition, this.getWordEndPosition(startPosition));
    }

    @VisibleForTesting
    public StringView getNextWord() {
        int startPosition;
        if (this.value.isEmpty()) {
            return StringView.EMPTY;
        }
        for (startPosition = Mth.clamp(this.cursor, 0, this.value.length() - 1); startPosition < this.value.length() && !Character.isWhitespace(this.value.charAt(startPosition)); ++startPosition) {
        }
        while (startPosition < this.value.length() && Character.isWhitespace(this.value.charAt(startPosition))) {
            ++startPosition;
        }
        return new StringView(startPosition, this.getWordEndPosition(startPosition));
    }

    private int getWordEndPosition(int from) {
        int end;
        for (end = from; end < this.value.length() && !Character.isWhitespace(this.value.charAt(end)); ++end) {
        }
        return end;
    }

    private void onValueChange() {
        this.reflowDisplayLines();
        this.valueListener.accept(this.value);
        this.cursorListener.run();
    }

    private void reflowDisplayLines() {
        this.displayLines.clear();
        if (this.value.isEmpty()) {
            this.displayLines.add(StringView.EMPTY);
            return;
        }
        this.font.getSplitter().splitLines(this.value, this.width, Style.EMPTY, false, (style, start, end) -> this.displayLines.add(new StringView(start, end)));
        if (this.value.charAt(this.value.length() - 1) == '\n') {
            this.displayLines.add(new StringView(this.value.length(), this.value.length()));
        }
    }

    private String truncateFullText(String input) {
        if (this.hasCharacterLimit()) {
            return StringUtil.truncateStringIfNecessary(input, this.characterLimit, false);
        }
        return input;
    }

    private String truncateInsertionText(String input) {
        String truncatedInput = input;
        if (this.hasCharacterLimit()) {
            int remainingCharacters = this.characterLimit - this.value.length();
            truncatedInput = StringUtil.truncateStringIfNecessary(input, remainingCharacters, false);
        }
        return truncatedInput;
    }

    private boolean overflowsLineLimit(String newValue) {
        return this.hasLineLimit() && this.font.getSplitter().splitLines(newValue, this.width, Style.EMPTY).size() + (StringUtil.endsWithNewLine(newValue) ? 1 : 0) > this.lineLimit;
    }

    @Environment(value=EnvType.CLIENT)
    protected record StringView(int beginIndex, int endIndex) {
        private static final StringView EMPTY = new StringView(0, 0);
    }
}

