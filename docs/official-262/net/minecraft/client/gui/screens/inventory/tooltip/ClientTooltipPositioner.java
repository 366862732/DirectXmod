/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector2ic
 */
package net.minecraft.client.gui.screens.inventory.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2ic;

@Environment(value=EnvType.CLIENT)
public interface ClientTooltipPositioner {
    public Vector2ic positionTooltip(int var1, int var2, int var3, int var4, int var5, int var6);
}

