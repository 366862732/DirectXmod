/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class CommonButtons {
    public static SpriteIconButton language(int width, Button.OnPress onPress, boolean iconOnly) {
        SpriteIconButton button = SpriteIconButton.builder(Component.translatable("options.language"), onPress, iconOnly).width(width).sprite(Identifier.withDefaultNamespace("icon/language"), 15, 15).narration(supplier -> Component.translatable("options.language.narration")).build();
        button.setTooltip(Tooltip.create(Component.translatable("options.language.tooltip")));
        return button;
    }

    public static SpriteIconButton accessibility(int width, Button.OnPress onPress, boolean iconOnly) {
        MutableComponent text = iconOnly ? Component.translatable("options.accessibility") : Component.translatable("accessibility.onboarding.accessibility.button");
        SpriteIconButton button = SpriteIconButton.builder(text, onPress, iconOnly).width(width).sprite(Identifier.withDefaultNamespace("icon/accessibility"), 15, 15).narration(supplier -> Component.translatable("accessibility.onboarding.accessibility.button.narration")).build();
        button.setTooltip(Tooltip.create(Component.translatable("options.accessibility.tooltip")));
        return button;
    }

    public static FriendsButton friends(int width, Button.OnPress onPress, boolean friendsAvailable) {
        return new FriendsButton(width, onPress, friendsAvailable);
    }
}

