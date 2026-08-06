/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.mojang.blaze3d.vertex;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class CompactVectorArray {
    private static final int VECTOR_SIZE = 3;
    private static final int OFFSET_X = 0;
    private static final int OFFSET_Y = 1;
    private static final int OFFSET_Z = 2;
    private final float[] contents;

    public CompactVectorArray(int count) {
        this.contents = new float[3 * count];
    }

    public int size() {
        return this.contents.length / 3;
    }

    public void set(int index, Vector3fc v) {
        this.set(index, v.x(), v.y(), v.z());
    }

    public void set(int index, float x, float y, float z) {
        this.contents[3 * index + 0] = x;
        this.contents[3 * index + 1] = y;
        this.contents[3 * index + 2] = z;
    }

    public Vector3f get(int index, Vector3f output) {
        return output.set(this.contents[3 * index + 0], this.contents[3 * index + 1], this.contents[3 * index + 2]);
    }

    public float getX(int index) {
        return this.contents[3 * index + 0];
    }

    public float getY(int index) {
        return this.contents[3 * index + 1];
    }

    public float getZ(int index) {
        return this.contents[3 * index + 2];
    }
}

