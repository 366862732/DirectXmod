/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.GeometryUtils
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.cuboid;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FaceBakery {
    private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5f, 0.5f, 0.5f);

    @VisibleForTesting
    static CuboidFace.UVs defaultFaceUV(Vector3fc from, Vector3fc to, Direction facing) {
        return switch (facing) {
            default -> throw new MatchException(null, null);
            case Direction.DOWN -> new CuboidFace.UVs(from.x(), 16.0f - to.z(), to.x(), 16.0f - from.z());
            case Direction.UP -> new CuboidFace.UVs(from.x(), from.z(), to.x(), to.z());
            case Direction.NORTH -> new CuboidFace.UVs(16.0f - to.x(), 16.0f - to.y(), 16.0f - from.x(), 16.0f - from.y());
            case Direction.SOUTH -> new CuboidFace.UVs(from.x(), 16.0f - to.y(), to.x(), 16.0f - from.y());
            case Direction.WEST -> new CuboidFace.UVs(from.z(), 16.0f - to.y(), to.z(), 16.0f - from.y());
            case Direction.EAST -> new CuboidFace.UVs(16.0f - to.z(), 16.0f - to.y(), 16.0f - from.z(), 16.0f - from.y());
        };
    }

    private static Transparency computeMaterialTransparency(Material.Baked material, CuboidFace.UVs uvs) {
        if (material.forceTranslucent()) {
            return Transparency.TRANSLUCENT;
        }
        return material.sprite().contents().computeTransparency(Math.min(uvs.minU(), uvs.maxU()) / 16.0f, Math.min(uvs.minV(), uvs.maxV()) / 16.0f, Math.max(uvs.minU(), uvs.maxU()) / 16.0f, Math.max(uvs.minV(), uvs.maxV()) / 16.0f);
    }

    public static BakedQuad bakeQuad(ModelBaker modelBaker, Vector3fc from, Vector3fc to, CuboidFace face, Material.Baked material, Direction facing, ModelState modelState, @Nullable CuboidRotation elementRotation, boolean shade, int lightEmission) {
        CuboidFace.UVs uvs = face.uvs();
        if (uvs == null) {
            uvs = FaceBakery.defaultFaceUV(from, to, facing);
        }
        Transparency transparency = FaceBakery.computeMaterialTransparency(material, uvs);
        ModelBaker.Interner interner = modelBaker.interner();
        BakedQuad.MaterialInfo materialInfo = interner.materialInfo(BakedQuad.MaterialInfo.of(material, transparency, face.tintIndex(), shade, lightEmission));
        return FaceBakery.bakeQuad(interner, from, to, uvs, face.rotation(), materialInfo, facing, modelState, elementRotation);
    }

    public static BakedQuad bakeQuad(ModelBaker.Interner interner, Vector3fc from, Vector3fc to, CuboidFace.UVs uvs, Quadrant uvRotation, BakedQuad.MaterialInfo materialInfo, Direction facing, ModelState modelState, @Nullable CuboidRotation elementRotation) {
        Matrix4fc uvTransform = modelState.inverseFaceTransformation(facing);
        Vector3fc[] vertexPositions = new Vector3fc[4];
        long[] vertexPackedUvs = new long[4];
        FaceInfo faceInfo = FaceInfo.fromFacing(facing);
        for (int i = 0; i < 4; ++i) {
            FaceBakery.bakeVertex(i, faceInfo, uvs, uvRotation, uvTransform, from, to, materialInfo, modelState.transformation(), elementRotation, vertexPositions, vertexPackedUvs, interner);
        }
        Direction finalDirection = FaceBakery.calculateFacing(vertexPositions);
        if (elementRotation == null && finalDirection != null) {
            FaceBakery.recalculateWinding(vertexPositions, vertexPackedUvs, finalDirection);
        }
        return new BakedQuad(vertexPositions[0], vertexPositions[1], vertexPositions[2], vertexPositions[3], vertexPackedUvs[0], vertexPackedUvs[1], vertexPackedUvs[2], vertexPackedUvs[3], Objects.requireNonNullElse(finalDirection, Direction.UP), materialInfo);
    }

    private static void bakeVertex(int index, FaceInfo faceInfo, CuboidFace.UVs uvs, Quadrant uvRotation, Matrix4fc uvTransform, Vector3fc from, Vector3fc to, BakedQuad.MaterialInfo materialInfo, Transformation rotation, @Nullable CuboidRotation elementRotation, Vector3fc[] positionOutput, long[] uvOutput, ModelBaker.Interner interner) {
        float transformedV;
        float transformedU;
        FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(index);
        Vector3f vertex = vertexInfo.select(from, to).div(16.0f);
        if (elementRotation != null) {
            FaceBakery.rotateVertexBy(vertex, elementRotation.origin(), elementRotation.transform());
        }
        if (rotation != Transformation.IDENTITY) {
            FaceBakery.rotateVertexBy(vertex, BLOCK_MIDDLE, rotation.getMatrix());
        }
        float rawU = CuboidFace.getU(uvs, uvRotation, index);
        float rawV = CuboidFace.getV(uvs, uvRotation, index);
        if (MatrixUtil.isIdentity(uvTransform)) {
            transformedU = rawU;
            transformedV = rawV;
        } else {
            Vector3f transformedUV = uvTransform.transformPosition(new Vector3f(FaceBakery.cornerToCenter(rawU), FaceBakery.cornerToCenter(rawV), 0.0f));
            transformedU = FaceBakery.centerToCorner(transformedUV.x);
            transformedV = FaceBakery.centerToCorner(transformedUV.y);
        }
        positionOutput[index] = interner.vector((Vector3fc)vertex);
        uvOutput[index] = UVPair.pack(materialInfo.sprite().getU(transformedU), materialInfo.sprite().getV(transformedV));
    }

    private static float cornerToCenter(float value) {
        return value - 0.5f;
    }

    private static float centerToCorner(float value) {
        return value + 0.5f;
    }

    private static void rotateVertexBy(Vector3f vertex, Vector3fc origin, Matrix4fc transformation) {
        vertex.sub(origin);
        transformation.transformPosition(vertex);
        vertex.add(origin);
    }

    private static @Nullable Direction calculateFacing(Vector3fc[] positions) {
        Vector3f normal = new Vector3f();
        GeometryUtils.normal((Vector3fc)positions[0], (Vector3fc)positions[1], (Vector3fc)positions[2], (Vector3f)normal);
        return FaceBakery.findClosestDirection(normal);
    }

    private static @Nullable Direction findClosestDirection(Vector3f direction) {
        if (!direction.isFinite()) {
            return null;
        }
        Direction best = null;
        float closestProduct = 0.0f;
        for (Direction candidate : Direction.values()) {
            float product = direction.dot(candidate.getUnitVec3f());
            if (!(product >= 0.0f) || !(product > closestProduct)) continue;
            closestProduct = product;
            best = candidate;
        }
        return best;
    }

    private static void recalculateWinding(Vector3fc[] positions, long[] uvs, Direction direction) {
        float minX = 999.0f;
        float minY = 999.0f;
        float minZ = 999.0f;
        float maxX = -999.0f;
        float maxY = -999.0f;
        float maxZ = -999.0f;
        for (int i = 0; i < 4; ++i) {
            Vector3fc position = positions[i];
            float x = position.x();
            float y = position.y();
            float z = position.z();
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (!(z > maxZ)) continue;
            maxZ = z;
        }
        FaceInfo info = FaceInfo.fromFacing(direction);
        for (int vertex = 0; vertex < 4; ++vertex) {
            float newZ;
            float newY;
            FaceInfo.VertexInfo vertInfo = info.getVertexInfo(vertex);
            float newX = vertInfo.xFace().select(minX, minY, minZ, maxX, maxY, maxZ);
            int vertexToSwap = FaceBakery.findVertex(positions, vertex, newX, newY = vertInfo.yFace().select(minX, minY, minZ, maxX, maxY, maxZ), newZ = vertInfo.zFace().select(minX, minY, minZ, maxX, maxY, maxZ));
            if (vertexToSwap == -1) {
                throw new IllegalStateException("Can't find vertex to swap");
            }
            if (vertexToSwap == vertex) continue;
            FaceBakery.swap(positions, vertexToSwap, vertex);
            FaceBakery.swap(uvs, vertexToSwap, vertex);
        }
    }

    private static int findVertex(Vector3fc[] positions, int start, float x, float y, float z) {
        for (int i = start; i < 4; ++i) {
            Vector3fc position = positions[i];
            if (x != position.x() || y != position.y() || z != position.z()) continue;
            return i;
        }
        return -1;
    }

    private static void swap(Vector3fc[] array, int indexA, int indexB) {
        Vector3fc tmp = array[indexA];
        array[indexA] = array[indexB];
        array[indexB] = tmp;
    }

    private static void swap(long[] array, int indexA, int indexB) {
        long tmp = array[indexA];
        array[indexA] = array[indexB];
        array[indexB] = tmp;
    }
}

