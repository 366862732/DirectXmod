/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Floats
 *  it.unimi.dsi.fastutil.ints.IntArrays
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.mojang.blaze3d.vertex;

import com.google.common.primitives.Floats;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public interface VertexSorting {
    public static final VertexSorting DISTANCE_TO_ORIGIN = VertexSorting.byDistance(0.0f, 0.0f, 0.0f);
    public static final VertexSorting ORTHOGRAPHIC_Z = VertexSorting.byDistance((Vector3f point) -> -point.z());

    public static VertexSorting byDistance(float x, float y, float z) {
        return VertexSorting.byDistance((Vector3fc)new Vector3f(x, y, z));
    }

    public static VertexSorting byDistance(Vector3fc origin) {
        return VertexSorting.byDistance(arg_0 -> ((Vector3fc)origin).distanceSquared(arg_0));
    }

    public static VertexSorting byDistance(DistanceFunction function) {
        return values -> {
            Vector3f scratch = new Vector3f();
            float[] keys = new float[values.size()];
            int[] indices = new int[values.size()];
            for (int i = 0; i < values.size(); ++i) {
                keys[i] = function.apply(values.get(i, scratch));
                indices[i] = i;
            }
            IntArrays.mergeSort((int[])indices, (o1, o2) -> Floats.compare((float)keys[o2], (float)keys[o1]));
            return indices;
        };
    }

    public int[] sort(CompactVectorArray var1);

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface DistanceFunction {
        public float apply(Vector3f var1);
    }
}

