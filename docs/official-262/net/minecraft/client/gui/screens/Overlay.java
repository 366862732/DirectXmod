/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Renderable;

@Environment(value=EnvType.CLIENT)
public abstract class Overlay
implements Renderable {
    public boolean isPausing() {
        return true;
    }

    public void tick() {
    }
}

