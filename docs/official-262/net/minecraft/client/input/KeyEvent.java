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
import net.minecraft.client.input.InputWithModifiers;

@Environment(value=EnvType.CLIENT)
public record KeyEvent(@InputConstants.Value int key, int scancode, @InputWithModifiers.Modifiers int modifiers) implements InputWithModifiers
{
    @Override
    public int input() {
        return this.key;
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    @Environment(value=EnvType.CLIENT)
    public static @interface Action {
    }
}

