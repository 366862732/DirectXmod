/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public final class ModelPart {
    public static final float DEFAULT_SCALE = 1.0f;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0f;
    public float yScale = 1.0f;
    public float zScale = 1.0f;
    public boolean visible = true;
    public boolean skipDraw;
    private final List<Cube> cubes;
    private final Map<String, ModelPart> children;
    private PartPose initialPose = PartPose.ZERO;

    public ModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
        this.cubes = cubes;
        this.children = children;
    }

    public PartPose storePose() {
        return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
    }

    public PartPose getInitialPose() {
        return this.initialPose;
    }

    public void setInitialPose(PartPose initialPose) {
        this.initialPose = initialPose;
    }

    public void resetPose() {
        this.loadPose(this.initialPose);
    }

    public void loadPose(PartPose pose) {
        this.x = pose.x();
        this.y = pose.y();
        this.z = pose.z();
        this.xRot = pose.xRot();
        this.yRot = pose.yRot();
        this.zRot = pose.zRot();
        this.xScale = pose.xScale();
        this.yScale = pose.yScale();
        this.zScale = pose.zScale();
    }

    public boolean hasChild(String name) {
        return this.children.containsKey(name);
    }

    public ModelPart getChild(String name) {
        ModelPart result = this.children.get(name);
        if (result == null) {
            throw new NoSuchElementException("Can't find part " + name);
        }
        return result;
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float xRot, float yRot, float zRot) {
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords) {
        this.render(poseStack, buffer, lightCoords, overlayCoords, -1);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        if (!this.visible) {
            return;
        }
        if (this.cubes.isEmpty() && this.children.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        this.translateAndRotate(poseStack);
        if (!this.skipDraw) {
            this.compile(poseStack.last(), buffer, lightCoords, overlayCoords, color);
        }
        for (ModelPart child : this.children.values()) {
            child.render(poseStack, buffer, lightCoords, overlayCoords, color);
        }
        poseStack.popPose();
    }

    public void rotateBy(Quaternionf rotation) {
        Matrix3f oldRotation = new Matrix3f().rotationZYX(this.zRot, this.yRot, this.xRot);
        Matrix3f newRotation = oldRotation.rotate((Quaternionfc)rotation);
        Vector3f newAngles = newRotation.getEulerAnglesZYX(new Vector3f());
        this.setRotation(newAngles.x, newAngles.y, newAngles.z);
    }

    public void getExtentsForGui(PoseStack poseStack, Consumer<Vector3fc> output) {
        this.visit(poseStack, (pose, partPath, cubeIndex, cube) -> {
            for (Polygon polygon : cube.polygons) {
                for (Vertex vertex : polygon.vertices()) {
                    float x = vertex.worldX();
                    float y = vertex.worldY();
                    float z = vertex.worldZ();
                    Vector3f pos = pose.pose().transformPosition(x, y, z, new Vector3f());
                    output.accept((Vector3fc)pos);
                }
            }
        });
    }

    public void visit(PoseStack poseStack, Visitor visitor) {
        this.visit(poseStack, visitor, "");
    }

    private void visit(PoseStack poseStack, Visitor visitor, String path) {
        if (this.cubes.isEmpty() && this.children.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        this.translateAndRotate(poseStack);
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < this.cubes.size(); ++i) {
            visitor.visit(pose, path, i, this.cubes.get(i));
        }
        String childPath = path + "/";
        this.children.forEach((name, child) -> child.visit(poseStack, visitor, childPath + name));
        poseStack.popPose();
    }

    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x / 16.0f, this.y / 16.0f, this.z / 16.0f);
        if (this.xRot != 0.0f || this.yRot != 0.0f || this.zRot != 0.0f) {
            poseStack.mulPose((Quaternionfc)new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
        }
        if (this.xScale != 1.0f || this.yScale != 1.0f || this.zScale != 1.0f) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    private void compile(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
        for (Cube cube : this.cubes) {
            cube.compile(pose, builder, lightCoords, overlayCoords, color);
        }
    }

    public Cube getRandomCube(RandomSource random) {
        return Util.getRandom(this.cubes, random);
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }

    public void offsetPos(Vector3f offset) {
        this.x += offset.x();
        this.y += offset.y();
        this.z += offset.z();
    }

    public void offsetRotation(Vector3f offset) {
        this.xRot += offset.x();
        this.yRot += offset.y();
        this.zRot += offset.z();
    }

    public void offsetScale(Vector3f offset) {
        this.xScale += offset.x();
        this.yScale += offset.y();
        this.zScale += offset.z();
    }

    public List<ModelPart> getAllParts() {
        ArrayList<ModelPart> allParts = new ArrayList<ModelPart>();
        allParts.add(this);
        this.addAllChildren((name, part) -> allParts.add((ModelPart)part));
        return List.copyOf(allParts);
    }

    public Function<String, @Nullable ModelPart> createPartLookup() {
        HashMap<String, ModelPart> parts = new HashMap<String, ModelPart>();
        parts.put("root", this);
        this.addAllChildren(parts::putIfAbsent);
        return parts::get;
    }

    private void addAllChildren(BiConsumer<String, ModelPart> output) {
        for (Map.Entry<String, ModelPart> entry : this.children.entrySet()) {
            output.accept(entry.getKey(), entry.getValue());
        }
        for (ModelPart part : this.children.values()) {
            part.addAllChildren(output);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Visitor {
        public void visit(PoseStack.Pose var1, String var2, int var3, Cube var4);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Cube {
        public final Polygon[] polygons;
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;

        public Cube(int xTexOffs, int yTexOffs, float minX, float minY, float minZ, float width, float height, float depth, float growX, float growY, float growZ, boolean mirror, float xTexSize, float yTexSize, Set<Direction> visibleFaces) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = minX + width;
            this.maxY = minY + height;
            this.maxZ = minZ + depth;
            this.polygons = new Polygon[visibleFaces.size()];
            float maxX = minX + width;
            float maxY = minY + height;
            float maxZ = minZ + depth;
            minX -= growX;
            minY -= growY;
            minZ -= growZ;
            maxX += growX;
            maxY += growY;
            maxZ += growZ;
            if (mirror) {
                float tmp = maxX;
                maxX = minX;
                minX = tmp;
            }
            Vertex t0 = new Vertex(minX, minY, minZ, 0.0f, 0.0f);
            Vertex t1 = new Vertex(maxX, minY, minZ, 0.0f, 8.0f);
            Vertex t2 = new Vertex(maxX, maxY, minZ, 8.0f, 8.0f);
            Vertex t3 = new Vertex(minX, maxY, minZ, 8.0f, 0.0f);
            Vertex l0 = new Vertex(minX, minY, maxZ, 0.0f, 0.0f);
            Vertex l1 = new Vertex(maxX, minY, maxZ, 0.0f, 8.0f);
            Vertex l2 = new Vertex(maxX, maxY, maxZ, 8.0f, 8.0f);
            Vertex l3 = new Vertex(minX, maxY, maxZ, 8.0f, 0.0f);
            float u0 = xTexOffs;
            float u1 = (float)xTexOffs + depth;
            float u2 = (float)xTexOffs + depth + width;
            float u22 = (float)xTexOffs + depth + width + width;
            float u3 = (float)xTexOffs + depth + width + depth;
            float u4 = (float)xTexOffs + depth + width + depth + width;
            float v0 = yTexOffs;
            float v1 = (float)yTexOffs + depth;
            float v2 = (float)yTexOffs + depth + height;
            int pos = 0;
            if (visibleFaces.contains(Direction.DOWN)) {
                this.polygons[pos++] = new Polygon(new Vertex[]{l1, l0, t0, t1}, u1, v0, u2, v1, xTexSize, yTexSize, mirror, Direction.DOWN);
            }
            if (visibleFaces.contains(Direction.UP)) {
                this.polygons[pos++] = new Polygon(new Vertex[]{t2, t3, l3, l2}, u2, v1, u22, v0, xTexSize, yTexSize, mirror, Direction.UP);
            }
            if (visibleFaces.contains(Direction.WEST)) {
                this.polygons[pos++] = new Polygon(new Vertex[]{t0, l0, l3, t3}, u0, v1, u1, v2, xTexSize, yTexSize, mirror, Direction.WEST);
            }
            if (visibleFaces.contains(Direction.NORTH)) {
                this.polygons[pos++] = new Polygon(new Vertex[]{t1, t0, t3, t2}, u1, v1, u2, v2, xTexSize, yTexSize, mirror, Direction.NORTH);
            }
            if (visibleFaces.contains(Direction.EAST)) {
                this.polygons[pos++] = new Polygon(new Vertex[]{l1, t1, t2, l2}, u2, v1, u3, v2, xTexSize, yTexSize, mirror, Direction.EAST);
            }
            if (visibleFaces.contains(Direction.SOUTH)) {
                this.polygons[pos] = new Polygon(new Vertex[]{l0, l1, l2, l3}, u3, v1, u4, v2, xTexSize, yTexSize, mirror, Direction.SOUTH);
            }
        }

        public void compile(PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color) {
            Matrix4f matrix = pose.pose();
            Vector3f scratchVector = new Vector3f();
            for (Polygon polygon : this.polygons) {
                Vector3f normal = pose.transformNormal(polygon.normal, scratchVector);
                float nx = normal.x();
                float ny = normal.y();
                float nz = normal.z();
                for (Vertex vertex : polygon.vertices) {
                    float x = vertex.worldX();
                    float y = vertex.worldY();
                    float z = vertex.worldZ();
                    Vector3f pos = matrix.transformPosition(x, y, z, scratchVector);
                    builder.addVertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlayCoords, lightCoords, nx, ny, nz);
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Polygon(Vertex[] vertices, Vector3fc normal) {
        public Polygon(Vertex[] vertices, float u0, float v0, float u1, float v1, float xTexSize, float yTexSize, boolean mirror, Direction facing) {
            this(vertices, (mirror ? Polygon.mirrorFacing(facing) : facing).getUnitVec3f());
            float us = 0.0f / xTexSize;
            float vs = 0.0f / yTexSize;
            vertices[0] = vertices[0].remap(u1 / xTexSize - us, v0 / yTexSize + vs);
            vertices[1] = vertices[1].remap(u0 / xTexSize + us, v0 / yTexSize + vs);
            vertices[2] = vertices[2].remap(u0 / xTexSize + us, v1 / yTexSize - vs);
            vertices[3] = vertices[3].remap(u1 / xTexSize - us, v1 / yTexSize - vs);
            if (mirror) {
                int length = vertices.length;
                for (int i = 0; i < length / 2; ++i) {
                    Vertex tmp = vertices[i];
                    vertices[i] = vertices[length - 1 - i];
                    vertices[length - 1 - i] = tmp;
                }
            }
        }

        private static Direction mirrorFacing(Direction facing) {
            return facing.getAxis() == Direction.Axis.X ? facing.getOpposite() : facing;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Vertex(float x, float y, float z, float u, float v) {
        public static final float SCALE_FACTOR = 16.0f;

        public Vertex remap(float u, float v) {
            return new Vertex(this.x, this.y, this.z, u, v);
        }

        public float worldX() {
            return this.x / 16.0f;
        }

        public float worldY() {
            return this.y / 16.0f;
        }

        public float worldZ() {
            return this.z / 16.0f;
        }
    }
}

