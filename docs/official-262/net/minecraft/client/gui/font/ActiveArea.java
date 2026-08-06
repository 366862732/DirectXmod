/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Style;

@Environment(value=EnvType.CLIENT)
public interface ActiveArea {
    public Style style();

    public float activeLeft();

    public float activeTop();

    public float activeRight();

    public float activeBottom();
}

