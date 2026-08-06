/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class TextFieldHelper {
    private final Supplier<String> getMessageFn;
    private final Consumer<String> setMessageFn;
    private final Supplier<String> getClipboardFn;
    private final Consumer<String> setClipboardFn;
    private final Predicate<String> stringValidator;
    private int cursorPos;
    private int selectionPos;

    public TextFieldHelper(Supplier<String> getMessageFn, Consumer<String> setMessageFn, Supplier<String> getClipboardFn, Consumer<String> setClipboardFn, Predicate<String> stringValidator) {
        this.getMessageFn = getMessageFn;
        this.setMessageFn = setMessageFn;
        this.getClipboardFn = getClipboardFn;
        this.setClipboardFn = setClipboardFn;
        this.stringValidator = stringValidator;
        this.setCursorToEnd();
    }

    public static Supplier<String> createClipboardGetter(Minecraft minecraft) {
        return () -> TextFieldHelper.getClipboardContents(minecraft);
    }

    public static String getClipboardContents(Minecraft minecraft) {
        return ChatFormatting.stripFormatting(minecraft.keyboardHandler.getClipboard().replaceAll("\\r", ""));
    }

    public static Consumer<String> createClipboardSetter(Minecraft minecraft) {
        return text -> TextFieldHelper.setClipboardContents(minecraft, text);
    }

    public static void setClipboardContents(Minecraft minecraft, String text) {
        minecraft.keyboardHandler.setClipboard(text);
    }

    public boolean charTyped(CharacterEvent event) {
        if (event.isAllowedChatCharacter()) {
            this.insertText(this.getMessageFn.get(), event.codepointAsString());
        }
        return true;
    }

    public boolean keyPressed(KeyEvent event) {
        CursorStep cursorStep;
        if (event.isSelectAll()) {
            this.selectAll();
            return true;
        }
        if (event.isCopy()) {
            this.copy();
            return true;
        }
        if (event.isPaste()) {
            this.paste();
            return true;
        }
        if (event.isCut()) {
            this.cut();
            return true;
        }
        CursorStep cursorStep2 = cursorStep = event.hasControlDownWithQuirk() ? CursorStep.WORD : CursorStep.CHARACTER;
        if (event.key() == 259) {
            this.removeFromCursor(-1, cursorStep);
            return true;
        }
        if (event.key() == 261) {
            this.removeFromCursor(1, cursorStep);
        } else {
            if (event.isLeft()) {
                this.moveBy(-1, event.hasShiftDown(), cursorStep);
                return true;
            }
            if (event.isRight()) {
                this.moveBy(1, event.hasShiftDown(), cursorStep);
                return true;
            }
            if (event.key() == 268) {
                this.setCursorToStart(event.hasShiftDown());
                return true;
            }
            if (event.key() == 269) {
                this.setCursorToEnd(event.hasShiftDown());
                return true;
            }
        }
        return false;
    }

    private int clampToMsgLength(int value) {
        return Mth.clamp(value, 0, this.getMessageFn.get().length());
    }

    private void insertText(String message, String text) {
        if (this.selectionPos != this.cursorPos) {
            message = this.deleteSelection(message);
        }
        this.cursorPos = Mth.clamp(this.cursorPos, 0, message.length());
        String newPageText = new StringBuilder(message).insert(this.cursorPos, text).toString();
        if (this.stringValidator.test(newPageText)) {
            this.setMessageFn.accept(newPageText);
            this.selectionPos = this.cursorPos = Math.min(newPageText.length(), this.cursorPos + text.length());
        }
    }

    public void insertText(String text) {
        this.insertText(this.getMessageFn.get(), text);
    }

    private void resetSelectionIfNeeded(boolean selecting) {
        if (!selecting) {
            this.selectionPos = this.cursorPos;
        }
    }

    public void moveBy(int count, boolean selecting, CursorStep scope) {
        switch (scope.ordinal()) {
            case 0: {
                this.moveByChars(count, selecting);
                break;
            }
            case 1: {
                this.moveByWords(count, selecting);
            }
        }
    }

    public void moveByChars(int count) {
        this.moveByChars(count, false);
    }

    public void moveByChars(int count, boolean selecting) {
        this.cursorPos = Util.offsetByCodepoints(this.getMessageFn.get(), this.cursorPos, count);
        this.resetSelectionIfNeeded(selecting);
    }

    public void moveByWords(int count) {
        this.moveByWords(count, false);
    }

    public void moveByWords(int count, boolean selecting) {
        this.cursorPos = StringSplitter.getWordPosition(this.getMessageFn.get(), count, this.cursorPos, true);
        this.resetSelectionIfNeeded(selecting);
    }

    public void removeFromCursor(int count, CursorStep scope) {
        switch (scope.ordinal()) {
            case 0: {
                this.removeCharsFromCursor(count);
                break;
            }
            case 1: {
                this.removeWordsFromCursor(count);
            }
        }
    }

    public void removeWordsFromCursor(int count) {
        int wordPosition = StringSplitter.getWordPosition(this.getMessageFn.get(), count, this.cursorPos, true);
        this.removeCharsFromCursor(wordPosition - this.cursorPos);
    }

    public void removeCharsFromCursor(int count) {
        String message = this.getMessageFn.get();
        if (!message.isEmpty()) {
            String newMessage;
            if (this.selectionPos != this.cursorPos) {
                newMessage = this.deleteSelection(message);
            } else {
                int otherPos = Util.offsetByCodepoints(message, this.cursorPos, count);
                int start = Math.min(otherPos, this.cursorPos);
                int end = Math.max(otherPos, this.cursorPos);
                newMessage = new StringBuilder(message).delete(start, end).toString();
                if (count < 0) {
                    this.selectionPos = this.cursorPos = start;
                }
            }
            this.setMessageFn.accept(newMessage);
        }
    }

    public void cut() {
        String message = this.getMessageFn.get();
        this.setClipboardFn.accept(this.getSelected(message));
        this.setMessageFn.accept(this.deleteSelection(message));
    }

    public void paste() {
        this.insertText(this.getMessageFn.get(), this.getClipboardFn.get());
        this.selectionPos = this.cursorPos;
    }

    public void copy() {
        this.setClipboardFn.accept(this.getSelected(this.getMessageFn.get()));
    }

    public void selectAll() {
        this.selectionPos = 0;
        this.cursorPos = this.getMessageFn.get().length();
    }

    private String getSelected(String text) {
        int startIndex = Math.min(this.cursorPos, this.selectionPos);
        int endIndex = Math.max(this.cursorPos, this.selectionPos);
        return text.substring(startIndex, endIndex);
    }

    private String deleteSelection(String message) {
        if (this.selectionPos == this.cursorPos) {
            return message;
        }
        int startIndex = Math.min(this.cursorPos, this.selectionPos);
        int endIndex = Math.max(this.cursorPos, this.selectionPos);
        String updatedText = message.substring(0, startIndex) + message.substring(endIndex);
        this.selectionPos = this.cursorPos = startIndex;
        return updatedText;
    }

    public void setCursorToStart() {
        this.setCursorToStart(false);
    }

    public void setCursorToStart(boolean selecting) {
        this.cursorPos = 0;
        this.resetSelectionIfNeeded(selecting);
    }

    public void setCursorToEnd() {
        this.setCursorToEnd(false);
    }

    public void setCursorToEnd(boolean selecting) {
        this.cursorPos = this.getMessageFn.get().length();
        this.resetSelectionIfNeeded(selecting);
    }

    public int getCursorPos() {
        return this.cursorPos;
    }

    public void setCursorPos(int value) {
        this.setCursorPos(value, true);
    }

    public void setCursorPos(int value, boolean selecting) {
        this.cursorPos = this.clampToMsgLength(value);
        this.resetSelectionIfNeeded(selecting);
    }

    public int getSelectionPos() {
        return this.selectionPos;
    }

    public void setSelectionPos(int value) {
        this.selectionPos = this.clampToMsgLength(value);
    }

    public void setSelectionRange(int start, int end) {
        int maxSize = this.getMessageFn.get().length();
        this.cursorPos = Mth.clamp(start, 0, maxSize);
        this.selectionPos = Mth.clamp(end, 0, maxSize);
    }

    public boolean isSelecting() {
        return this.cursorPos != this.selectionPos;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum CursorStep {
        CHARACTER,
        WORD;

    }
}

