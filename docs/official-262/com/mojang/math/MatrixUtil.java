/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Math
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package com.mojang.math;

import com.mojang.math.GivensParameters;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class MatrixUtil {
    private static final float G = 3.0f + 2.0f * Math.sqrt((float)2.0f);
    private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle(0.7853982f);

    private MatrixUtil() {
    }

    public static Matrix4f mulComponentWise(Matrix4f m, float factor) {
        return m.set(m.m00() * factor, m.m01() * factor, m.m02() * factor, m.m03() * factor, m.m10() * factor, m.m11() * factor, m.m12() * factor, m.m13() * factor, m.m20() * factor, m.m21() * factor, m.m22() * factor, m.m23() * factor, m.m30() * factor, m.m31() * factor, m.m32() * factor, m.m33() * factor);
    }

    private static GivensParameters approxGivensQuat(float a11, float a12, float a22) {
        float sh = a12;
        float ch = 2.0f * (a11 - a22);
        if (G * sh * sh < ch * ch) {
            return GivensParameters.fromUnnormalized(sh, ch);
        }
        return PI_4;
    }

    private static GivensParameters qrGivensQuat(float a1, float a2) {
        float p = (float)java.lang.Math.hypot(a1, a2);
        float sh = p > 1.0E-6f ? a2 : 0.0f;
        float ch = Math.abs((float)a1) + Math.max((float)p, (float)1.0E-6f);
        if (a1 < 0.0f) {
            float f = sh;
            sh = ch;
            ch = f;
        }
        return GivensParameters.fromUnnormalized(sh, ch);
    }

    private static void similarityTransform(Matrix3f a, Matrix3f q) {
        a.mul((Matrix3fc)q);
        q.transpose();
        q.mul((Matrix3fc)a);
        a.set((Matrix3fc)q);
    }

    private static void stepJacobi(Matrix3f m, Matrix3f tmpMat, Quaternionf tmpQ, Quaternionf output) {
        Quaternionf qt;
        GivensParameters p;
        if (m.m01 * m.m01 + m.m10 * m.m10 > 1.0E-6f) {
            p = MatrixUtil.approxGivensQuat(m.m00, 0.5f * (m.m01 + m.m10), m.m11);
            qt = p.aroundZ(tmpQ);
            output.mul((Quaternionfc)qt);
            p.aroundZ(tmpMat);
            MatrixUtil.similarityTransform(m, tmpMat);
        }
        if (m.m02 * m.m02 + m.m20 * m.m20 > 1.0E-6f) {
            p = MatrixUtil.approxGivensQuat(m.m00, 0.5f * (m.m02 + m.m20), m.m22).inverse();
            qt = p.aroundY(tmpQ);
            output.mul((Quaternionfc)qt);
            p.aroundY(tmpMat);
            MatrixUtil.similarityTransform(m, tmpMat);
        }
        if (m.m12 * m.m12 + m.m21 * m.m21 > 1.0E-6f) {
            p = MatrixUtil.approxGivensQuat(m.m11, 0.5f * (m.m12 + m.m21), m.m22);
            qt = p.aroundX(tmpQ);
            output.mul((Quaternionfc)qt);
            p.aroundX(tmpMat);
            MatrixUtil.similarityTransform(m, tmpMat);
        }
    }

    public static void eigenvalueJacobi(Matrix3f inOut, int steps, Quaternionf result) {
        result.identity();
        Matrix3f scratchMat = new Matrix3f();
        Quaternionf scratchQ = new Quaternionf();
        for (int i = 0; i < steps; ++i) {
            MatrixUtil.stepJacobi(inOut, scratchMat, scratchQ, result);
        }
        result.normalize();
    }

    public static void svdDecompose(Matrix4fc input, Vector3f t, Quaternionf u, Vector3f s, Quaternionf v) {
        float scaleFactor = 1.0f / input.m33();
        MatrixUtil.svdDecompose(new Matrix3f(input).scale(scaleFactor), u, s, v);
        input.getTranslation(t).mul(scaleFactor);
    }

    private static void svdDecompose(Matrix3f input, Quaternionf u, Vector3f s, Quaternionf v) {
        Matrix3f scratch = new Matrix3f((Matrix3fc)input);
        scratch.transpose();
        scratch.mul((Matrix3fc)input);
        MatrixUtil.eigenvalueJacobi(scratch, 5, v);
        float columnScaleSquare0 = scratch.m00;
        float columnScaleSquare1 = scratch.m11;
        boolean zeroColumn0 = (double)columnScaleSquare0 < 1.0E-6;
        boolean zeroColumn1 = (double)columnScaleSquare1 < 1.0E-6;
        Matrix3f u012s = input.rotate((Quaternionfc)v);
        u.identity();
        Quaternionf tmpQ = new Quaternionf();
        GivensParameters p = zeroColumn0 ? MatrixUtil.qrGivensQuat(u012s.m11, -u012s.m10) : MatrixUtil.qrGivensQuat(u012s.m00, u012s.m01);
        Quaternionf qt0 = p.aroundZ(tmpQ);
        Matrix3f u12s = p.aroundZ(scratch);
        u.mul((Quaternionfc)qt0);
        u12s.transpose().mul((Matrix3fc)u012s);
        scratch = u012s;
        p = zeroColumn0 ? MatrixUtil.qrGivensQuat(u12s.m22, -u12s.m20) : MatrixUtil.qrGivensQuat(u12s.m00, u12s.m02);
        p = p.inverse();
        Quaternionf qt1 = p.aroundY(tmpQ);
        Matrix3f u2s = p.aroundY(scratch);
        u.mul((Quaternionfc)qt1);
        u2s.transpose().mul((Matrix3fc)u12s);
        scratch = u12s;
        p = zeroColumn1 ? MatrixUtil.qrGivensQuat(u2s.m22, -u2s.m21) : MatrixUtil.qrGivensQuat(u2s.m11, u2s.m12);
        Quaternionf qt2 = p.aroundX(tmpQ);
        Matrix3f sm = p.aroundX(scratch);
        u.mul((Quaternionfc)qt2);
        sm.transpose().mul((Matrix3fc)u2s);
        s.set(sm.m00, sm.m11, sm.m22);
        v.conjugate();
    }

    public static boolean checkPropertyRaw(Matrix4fc matrix, int property) {
        return (matrix.properties() & property) != 0;
    }

    public static boolean checkProperty(Matrix4fc matrix, int property) {
        if (MatrixUtil.checkPropertyRaw(matrix, property)) {
            return true;
        }
        if (matrix instanceof Matrix4f) {
            Matrix4f mutableMatrix = (Matrix4f)matrix;
            int currentProperties = mutableMatrix.properties();
            mutableMatrix.determineProperties();
            mutableMatrix.assume(mutableMatrix.properties() | currentProperties);
            return MatrixUtil.checkPropertyRaw(matrix, property);
        }
        return false;
    }

    public static boolean isIdentity(Matrix4fc matrix) {
        return MatrixUtil.checkProperty(matrix, 4);
    }

    public static boolean isPureTranslation(Matrix4fc matrix) {
        return MatrixUtil.checkProperty(matrix, 8);
    }
}

