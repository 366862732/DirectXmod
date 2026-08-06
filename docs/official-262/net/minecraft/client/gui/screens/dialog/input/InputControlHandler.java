/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.dialog.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.InputControl;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface InputControlHandler<T extends InputControl> {
    public void addControl(T var1, Screen var2, Output var3);

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Output {
        public void accept(LayoutElement var1, Action.ValueGetter var2);
    }
}

