/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.AbstractDoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class CubePointRange
extends AbstractDoubleList {
    private final int parts;

    public CubePointRange(int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("Need at least 1 part");
        }
        this.parts = parts;
    }

    public double getDouble(int index) {
        return (double)index / (double)this.parts;
    }

    public int size() {
        return this.parts + 1;
    }
}

