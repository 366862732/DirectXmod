/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Math
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 */
package com.mojang.math;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public record GivensParameters(float sinHalf, float cosHalf) {
    public static GivensParameters fromUnnormalized(float sinHalf, float cosHalf) {
        float w = Math.invsqrt((float)(sinHalf * sinHalf + cosHalf * cosHalf));
        return new GivensParameters(w * sinHalf, w * cosHalf);
    }

    public static GivensParameters fromPositiveAngle(float angle) {
        float sin = Math.sin((float)(angle / 2.0f));
        float cos = Math.cosFromSin((float)sin, (float)(angle / 2.0f));
        return new GivensParameters(sin, cos);
    }

    public GivensParameters inverse() {
        return new GivensParameters(-this.sinHalf, this.cosHalf);
    }

    public Quaternionf aroundX(Quaternionf input) {
        return input.set(this.sinHalf, 0.0f, 0.0f, this.cosHalf);
    }

    public Quaternionf aroundY(Quaternionf input) {
        return input.set(0.0f, this.sinHalf, 0.0f, this.cosHalf);
    }

    public Quaternionf aroundZ(Quaternionf input) {
        return input.set(0.0f, 0.0f, this.sinHalf, this.cosHalf);
    }

    public float cos() {
        return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
    }

    public float sin() {
        return 2.0f * this.sinHalf * this.cosHalf;
    }

    public Matrix3f aroundX(Matrix3f input) {
        input.m01 = 0.0f;
        input.m02 = 0.0f;
        input.m10 = 0.0f;
        input.m20 = 0.0f;
        float c = this.cos();
        float s = this.sin();
        input.m11 = c;
        input.m22 = c;
        input.m12 = s;
        input.m21 = -s;
        input.m00 = 1.0f;
        return input;
    }

    public Matrix3f aroundY(Matrix3f input) {
        input.m01 = 0.0f;
        input.m10 = 0.0f;
        input.m12 = 0.0f;
        input.m21 = 0.0f;
        float c = this.cos();
        float s = this.sin();
        input.m00 = c;
        input.m22 = c;
        input.m02 = -s;
        input.m20 = s;
        input.m11 = 1.0f;
        return input;
    }

    public Matrix3f aroundZ(Matrix3f input) {
        input.m02 = 0.0f;
        input.m12 = 0.0f;
        input.m20 = 0.0f;
        input.m21 = 0.0f;
        float c = this.cos();
        float s = this.sin();
        input.m00 = c;
        input.m11 = c;
        input.m01 = s;
        input.m10 = -s;
        input.m22 = 1.0f;
        return input;
    }
}

