/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Math
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.resources.model.cuboid;

import com.mojang.math.MatrixUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public record CuboidRotation(Vector3fc origin, RotationValue value, boolean rescale, Matrix4fc transform) {
    public CuboidRotation(Vector3fc origin, RotationValue value, boolean rescale) {
        this(origin, value, rescale, (Matrix4fc)CuboidRotation.computeTransform(value, rescale));
    }

    private static Matrix4f computeTransform(RotationValue value, boolean rescale) {
        Matrix4f result = value.transformation();
        if (rescale && !MatrixUtil.isIdentity((Matrix4fc)result)) {
            Vector3fc scale = CuboidRotation.computeRescale((Matrix4fc)result);
            result.scale(scale);
        }
        return result;
    }

    private static Vector3fc computeRescale(Matrix4fc rotation) {
        Vector3f scratch = new Vector3f();
        float scaleX = CuboidRotation.scaleFactorForAxis(rotation, Direction.Axis.X, scratch);
        float scaleY = CuboidRotation.scaleFactorForAxis(rotation, Direction.Axis.Y, scratch);
        float scaleZ = CuboidRotation.scaleFactorForAxis(rotation, Direction.Axis.Z, scratch);
        return scratch.set(scaleX, scaleY, scaleZ);
    }

    private static float scaleFactorForAxis(Matrix4fc rotation, Direction.Axis axis, Vector3f scratch) {
        Vector3f axisUnit = scratch.set(axis.getPositive().getUnitVec3f());
        Vector3f transformedAxisUnit = rotation.transformDirection(axisUnit);
        float absX = Math.abs((float)transformedAxisUnit.x);
        float absY = Math.abs((float)transformedAxisUnit.y);
        float absZ = Math.abs((float)transformedAxisUnit.z);
        float maxComponent = Math.max((float)Math.max((float)absX, (float)absY), (float)absZ);
        return 1.0f / maxComponent;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface RotationValue {
        public Matrix4f transformation();
    }

    @Environment(value=EnvType.CLIENT)
    public record EulerXYZRotation(float x, float y, float z) implements RotationValue
    {
        @Override
        public Matrix4f transformation() {
            return new Matrix4f().rotationZYX(this.z * ((float)java.lang.Math.PI / 180), this.y * ((float)java.lang.Math.PI / 180), this.x * ((float)java.lang.Math.PI / 180));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record SingleAxisRotation(Direction.Axis axis, float angle) implements RotationValue
    {
        @Override
        public Matrix4f transformation() {
            Matrix4f result = new Matrix4f();
            if (this.angle == 0.0f) {
                return result;
            }
            Vector3fc rotateAround = this.axis.getPositive().getUnitVec3f();
            result.rotation(this.angle * ((float)java.lang.Math.PI / 180), rotateAround);
            return result;
        }
    }
}

