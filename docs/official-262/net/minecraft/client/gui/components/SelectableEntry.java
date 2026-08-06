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

@Environment(value=EnvType.CLIENT)
public interface SelectableEntry {
    default public boolean mouseOverIcon(int relX, int relY, int size) {
        return relX >= 0 && relX < size && relY >= 0 && relY < size;
    }

    default public boolean mouseOverLeftHalf(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size;
    }

    default public boolean mouseOverRightHalf(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= 0 && relY < size;
    }

    default public boolean mouseOverTopRightQuarter(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= 0 && relY < size / 2;
    }

    default public boolean mouseOverBottomRightQuarter(int relX, int relY, int size) {
        return relX >= size / 2 && relX < size && relY >= size / 2 && relY < size;
    }

    default public boolean mouseOverTopLeftQuarter(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= 0 && relY < size / 2;
    }

    default public boolean mouseOverBottomLeftQuarter(int relX, int relY, int size) {
        return relX >= 0 && relX < size / 2 && relY >= size / 2 && relY < size;
    }
}

