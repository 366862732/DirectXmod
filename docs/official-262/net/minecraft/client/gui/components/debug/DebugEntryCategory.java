/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public record DebugEntryCategory(Component label, float sortKey) {
    public static final DebugEntryCategory SCREEN_TEXT = new DebugEntryCategory(Component.translatable("debug.options.category.text"), 1.0f);
    public static final DebugEntryCategory RENDERER = new DebugEntryCategory(Component.translatable("debug.options.category.renderer"), 2.0f);
}

