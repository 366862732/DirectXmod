/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.FrustumIntersection
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
 */
package net.minecraft.client.renderer.culling;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

@Environment(value=EnvType.CLIENT)
public class Frustum {
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    private Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;

    public Frustum(Matrix4fc modelView, Matrix4f projection) {
        this.calculateFrustum(modelView, projection);
    }

    public Frustum(Frustum frustum) {
        this.set(frustum);
    }

    public void set(Frustum frustum) {
        this.intersection.set((Matrix4fc)frustum.matrix);
        this.matrix.set((Matrix4fc)frustum.matrix);
        this.camX = frustum.camX;
        this.camY = frustum.camY;
        this.camZ = frustum.camZ;
        this.viewVector = frustum.viewVector;
    }

    public Frustum offset(float offset) {
        this.camX += (double)(this.viewVector.x * offset);
        this.camY += (double)(this.viewVector.y * offset);
        this.camZ += (double)(this.viewVector.z * offset);
        return this;
    }

    public Frustum offsetToFullyIncludeCameraCube(int cubeSize) {
        double camX1 = Math.floor(this.camX / (double)cubeSize) * (double)cubeSize;
        double camY1 = Math.floor(this.camY / (double)cubeSize) * (double)cubeSize;
        double camZ1 = Math.floor(this.camZ / (double)cubeSize) * (double)cubeSize;
        double camX2 = Math.ceil(this.camX / (double)cubeSize) * (double)cubeSize;
        double camY2 = Math.ceil(this.camY / (double)cubeSize) * (double)cubeSize;
        double camZ2 = Math.ceil(this.camZ / (double)cubeSize) * (double)cubeSize;
        while (this.intersection.intersectAab((float)(camX1 - this.camX), (float)(camY1 - this.camY), (float)(camZ1 - this.camZ), (float)(camX2 - this.camX), (float)(camY2 - this.camY), (float)(camZ2 - this.camZ)) != -2) {
            this.camX -= (double)(this.viewVector.x() * 4.0f);
            this.camY -= (double)(this.viewVector.y() * 4.0f);
            this.camZ -= (double)(this.viewVector.z() * 4.0f);
        }
        return this;
    }

    public void prepare(double camX, double camY, double camZ) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
    }

    private void calculateFrustum(Matrix4fc modelView, Matrix4f projection) {
        projection.mul(modelView, this.matrix);
        this.intersection.set((Matrix4fc)this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0f, 0.0f, 1.0f, 0.0f));
    }

    public boolean isVisible(AABB bb) {
        int intersectionResult = this.cubeInFrustum(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        return intersectionResult == -2 || intersectionResult == -1;
    }

    public int cubeInFrustum(BoundingBox bb) {
        return this.cubeInFrustum(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);
    }

    private int cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float x1 = (float)(minX - this.camX);
        float y1 = (float)(minY - this.camY);
        float z1 = (float)(minZ - this.camZ);
        float x2 = (float)(maxX - this.camX);
        float y2 = (float)(maxY - this.camY);
        float z2 = (float)(maxZ - this.camZ);
        return this.intersection.intersectAab(x1, y1, z1, x2, y2, z2);
    }

    public boolean pointInFrustum(double x, double y, double z) {
        return this.intersection.testPoint((float)(x - this.camX), (float)(y - this.camY), (float)(z - this.camZ));
    }

    public Vector4f[] getFrustumPoints() {
        Vector4f[] frustumPoints = new Vector4f[]{new Vector4f(-1.0f, -1.0f, -1.0f, 1.0f), new Vector4f(1.0f, -1.0f, -1.0f, 1.0f), new Vector4f(1.0f, 1.0f, -1.0f, 1.0f), new Vector4f(-1.0f, 1.0f, -1.0f, 1.0f), new Vector4f(-1.0f, -1.0f, 1.0f, 1.0f), new Vector4f(1.0f, -1.0f, 1.0f, 1.0f), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector4f(-1.0f, 1.0f, 1.0f, 1.0f)};
        Matrix4f clipToWorldMatrix = this.matrix.invert(new Matrix4f());
        for (int i = 0; i < 8; ++i) {
            clipToWorldMatrix.transform(frustumPoints[i]);
            frustumPoints[i].div(frustumPoints[i].w());
        }
        return frustumPoints;
    }

    public double getCamX() {
        return this.camX;
    }

    public double getCamY() {
        return this.camY;
    }

    public double getCamZ() {
        return this.camZ;
    }
}

