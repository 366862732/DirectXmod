/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 */
package com.mojang.blaze3d;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public enum ProjectionType {
    PERSPECTIVE(VertexSorting.DISTANCE_TO_ORIGIN, (matrix, bias) -> matrix.scale(1.0f - bias / 4096.0f)),
    ORTHOGRAPHIC(VertexSorting.ORTHOGRAPHIC_Z, (matrix, bias) -> matrix.translate(0.0f, 0.0f, bias / 512.0f));

    private final VertexSorting vertexSorting;
    private final LayeringTransform layeringTransform;

    private ProjectionType(VertexSorting vertexSorting, LayeringTransform layeringTransform) {
        this.vertexSorting = vertexSorting;
        this.layeringTransform = layeringTransform;
    }

    public VertexSorting vertexSorting() {
        return this.vertexSorting;
    }

    public void applyLayeringTransform(Matrix4f matrix, float bias) {
        this.layeringTransform.apply(matrix, bias);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface LayeringTransform {
        public void apply(Matrix4f var1, float var2);
    }
}

