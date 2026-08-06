/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector2f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public interface VertexConsumer {
    public VertexConsumer addVertex(float var1, float var2, float var3);

    public VertexConsumer setColor(int var1, int var2, int var3, int var4);

    public VertexConsumer setColor(int var1);

    public VertexConsumer setUv(float var1, float var2);

    public VertexConsumer setUv1(int var1, int var2);

    public VertexConsumer setUv2(int var1, int var2);

    public VertexConsumer setNormal(float var1, float var2, float var3);

    public VertexConsumer setLineWidth(float var1);

    default public void addVertex(float x, float y, float z, int color, float u, float v, int overlayCoords, int lightCoords, float nx, float ny, float nz) {
        this.addVertex(x, y, z);
        this.setColor(color);
        this.setUv(u, v);
        this.setOverlay(overlayCoords);
        this.setLight(lightCoords);
        this.setNormal(nx, ny, nz);
    }

    default public VertexConsumer setColor(float r, float g, float b, float a) {
        return this.setColor((int)(r * 255.0f), (int)(g * 255.0f), (int)(b * 255.0f), (int)(a * 255.0f));
    }

    default public VertexConsumer setLight(int packedLightCoords) {
        return this.setUv2(packedLightCoords & 0xFFFF, packedLightCoords >> 16 & 0xFFFF);
    }

    default public VertexConsumer setOverlay(int packedOverlayCoords) {
        return this.setUv1(packedOverlayCoords & 0xFFFF, packedOverlayCoords >> 16 & 0xFFFF);
    }

    default public void putBlockBakedQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        Vector3fc normal = quad.direction().getUnitVec3f();
        int lightEmission = quad.materialInfo().lightEmission();
        for (int vertex = 0; vertex < 4; ++vertex) {
            Vector3fc pos = quad.position(vertex);
            long packedUv = quad.packedUV(vertex);
            int vertexColor = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
            float u = UVPair.unpackU(packedUv);
            float v = UVPair.unpackV(packedUv);
            this.addVertex(pos.x() + x, pos.y() + y, pos.z() + z, vertexColor, u, v, instance.overlayCoords(), light, normal.x(), normal.y(), normal.z());
        }
    }

    default public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Vector3fc normalVec = quad.direction().getUnitVec3f();
        Matrix4f matrix = pose.pose();
        Vector3f normal = pose.transformNormal(normalVec, new Vector3f());
        int lightEmission = quad.materialInfo().lightEmission();
        for (int vertex = 0; vertex < 4; ++vertex) {
            Vector3fc position = quad.position(vertex);
            long packedUv = quad.packedUV(vertex);
            int vertexColor = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
            Vector3f pos = matrix.transformPosition(position, new Vector3f());
            float u = UVPair.unpackU(packedUv);
            float v = UVPair.unpackV(packedUv);
            this.addVertex(pos.x(), pos.y(), pos.z(), vertexColor, u, v, instance.overlayCoords(), light, normal.x(), normal.y(), normal.z());
        }
    }

    default public VertexConsumer addVertex(Vector3fc position) {
        return this.addVertex(position.x(), position.y(), position.z());
    }

    default public VertexConsumer addVertex(PoseStack.Pose pose, Vector3fc position) {
        return this.addVertex(pose, position.x(), position.y(), position.z());
    }

    default public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        return this.addVertex((Matrix4fc)pose.pose(), x, y, z);
    }

    default public VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        Vector3f pos = pose.transformPosition(x, y, z, new Vector3f());
        return this.addVertex(pos.x(), pos.y(), pos.z());
    }

    default public VertexConsumer addVertexWith2DPose(Matrix3x2fc pose, float x, float y) {
        Vector2f pos = pose.transformPosition(x, y, new Vector2f());
        return this.addVertex(pos.x(), pos.y(), 0.0f);
    }

    default public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        Vector3f normal = pose.transformNormal(x, y, z, new Vector3f());
        return this.setNormal(normal.x(), normal.y(), normal.z());
    }

    default public VertexConsumer setNormal(PoseStack.Pose pose, Vector3fc normal) {
        return this.setNormal(pose, normal.x(), normal.y(), normal.z());
    }
}

