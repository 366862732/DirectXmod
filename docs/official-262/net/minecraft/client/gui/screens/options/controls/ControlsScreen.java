/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.options.controls;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class ControlsScreen
extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("controls.title");

    private static OptionInstance<?>[] options(Options options) {
        return new OptionInstance[]{options.toggleCrouch(), options.toggleSprint(), options.toggleAttack(), options.toggleUse(), options.autoJump(), options.sprintWindow(), options.operatorItemsTab()};
    }

    public ControlsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        this.list.addSmall(Button.builder(Component.translatable("options.mouse_settings"), button -> this.minecraft.gui.setScreen(new MouseSettingsScreen(this, this.options))).build(), Button.builder(Component.translatable("controls.keybinds"), button -> this.minecraft.gui.setScreen(new KeyBindsScreen(this, this.options))).build());
        this.list.addSmall(ControlsScreen.options(this.options));
    }
}

