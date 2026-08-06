/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.block.dispatch;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class BlockModelRotation
implements ModelState {
    private static final Map<OctahedralGroup, BlockModelRotation> BY_GROUP_ORDINAL = Util.makeEnumMap(OctahedralGroup.class, BlockModelRotation::new);
    public static final BlockModelRotation IDENTITY = BlockModelRotation.get(OctahedralGroup.IDENTITY);
    private final OctahedralGroup orientation;
    private final Transformation transformation;
    private final Map<Direction, Matrix4fc> faceMapping = new EnumMap<Direction, Matrix4fc>(Direction.class);
    private final Map<Direction, Matrix4fc> inverseFaceMapping = new EnumMap<Direction, Matrix4fc>(Direction.class);
    private final WithUvLock withUvLock = new WithUvLock(this);

    private BlockModelRotation(OctahedralGroup orientation) {
        this.orientation = orientation;
        this.transformation = orientation != OctahedralGroup.IDENTITY ? new Transformation((Matrix4fc)new Matrix4f(orientation.transformation())) : Transformation.IDENTITY;
        for (Direction face : Direction.values()) {
            Matrix4fc faceTransform = BlockMath.getFaceTransformation(this.transformation, face).getMatrix();
            this.faceMapping.put(face, faceTransform);
            this.inverseFaceMapping.put(face, (Matrix4fc)faceTransform.invertAffine(new Matrix4f()));
        }
    }

    @Override
    public Transformation transformation() {
        return this.transformation;
    }

    public static BlockModelRotation get(OctahedralGroup group) {
        return BY_GROUP_ORDINAL.get(group);
    }

    public ModelState withUvLock() {
        return this.withUvLock;
    }

    public String toString() {
        return "simple[" + this.orientation.getSerializedName() + "]";
    }

    @Environment(value=EnvType.CLIENT)
    private record WithUvLock(BlockModelRotation parent) implements ModelState
    {
        @Override
        public Transformation transformation() {
            return this.parent.transformation;
        }

        @Override
        public Matrix4fc faceTransformation(Direction face) {
            return this.parent.faceMapping.getOrDefault(face, NO_TRANSFORM);
        }

        @Override
        public Matrix4fc inverseFaceTransformation(Direction face) {
            return this.parent.inverseFaceMapping.getOrDefault(face, NO_TRANSFORM);
        }

        @Override
        public String toString() {
            return "uvLocked[" + this.parent.orientation.getSerializedName() + "]";
        }
    }
}

