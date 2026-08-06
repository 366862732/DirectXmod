/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonInfo;

@Environment(value=EnvType.CLIENT)
public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) implements InputWithModifiers
{
    @Override
    public int input() {
        return this.button();
    }

    public @MouseButtonInfo.MouseButton int button() {
        return this.buttonInfo().button();
    }

    @Override
    public @InputWithModifiers.Modifiers int modifiers() {
        return this.buttonInfo().modifiers();
    }
}

