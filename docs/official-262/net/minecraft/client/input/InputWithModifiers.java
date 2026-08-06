/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.InputQuirks;

@Environment(value=EnvType.CLIENT)
public interface InputWithModifiers {
    public static final int NOT_DIGIT = -1;

    public @InputConstants.Value int input();

    public @Modifiers int modifiers();

    default public boolean isSelection() {
        return this.input() == 257 || this.input() == 32 || this.input() == 335;
    }

    default public boolean isConfirmation() {
        return this.input() == 257 || this.input() == 335;
    }

    default public boolean isEscape() {
        return this.input() == 256;
    }

    default public boolean isLeft() {
        return this.input() == 263;
    }

    default public boolean isRight() {
        return this.input() == 262;
    }

    default public boolean isUp() {
        return this.input() == 265;
    }

    default public boolean isDown() {
        return this.input() == 264;
    }

    default public boolean isCycleFocus() {
        return this.input() == 258;
    }

    default public int getDigit() {
        int value = this.input() - 48;
        if (value >= 0 && value <= 9) {
            return value;
        }
        return -1;
    }

    default public boolean hasAltDown() {
        return (this.modifiers() & 4) != 0;
    }

    default public boolean hasShiftDown() {
        return (this.modifiers() & 1) != 0;
    }

    default public boolean hasControlDown() {
        return (this.modifiers() & 2) != 0;
    }

    default public boolean hasControlDownWithQuirk() {
        return (this.modifiers() & InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER) != 0;
    }

    default public boolean isSelectAll() {
        return this.input() == 65 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default public boolean isCopy() {
        return this.input() == 67 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default public boolean isPaste() {
        return this.input() == 86 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default public boolean isCut() {
        return this.input() == 88 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    @Environment(value=EnvType.CLIENT)
    public static @interface Modifiers {
    }
}

