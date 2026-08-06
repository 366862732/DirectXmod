/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParseException
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package com.mojang.math;

import com.google.gson.JsonParseException;
import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public enum Quadrant {
    R0(0, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY),
    R90(1, OctahedralGroup.BLOCK_ROT_X_90, OctahedralGroup.BLOCK_ROT_Y_90, OctahedralGroup.BLOCK_ROT_Z_90),
    R180(2, OctahedralGroup.BLOCK_ROT_X_180, OctahedralGroup.BLOCK_ROT_Y_180, OctahedralGroup.BLOCK_ROT_Z_180),
    R270(3, OctahedralGroup.BLOCK_ROT_X_270, OctahedralGroup.BLOCK_ROT_Y_270, OctahedralGroup.BLOCK_ROT_Z_270);

    public static final Codec<Quadrant> CODEC;
    public final int shift;
    public final OctahedralGroup rotationX;
    public final OctahedralGroup rotationY;
    public final OctahedralGroup rotationZ;

    private Quadrant(int shift, OctahedralGroup rotationX, OctahedralGroup rotationY, OctahedralGroup rotationZ) {
        this.shift = shift;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    @Deprecated
    public static Quadrant parseJson(int degrees) {
        return switch (Mth.positiveModulo(degrees, 360)) {
            case 0 -> R0;
            case 90 -> R90;
            case 180 -> R180;
            case 270 -> R270;
            default -> throw new JsonParseException("Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
        };
    }

    public static OctahedralGroup fromXYAngles(Quadrant xRotation, Quadrant yRotation) {
        return yRotation.rotationY.compose(xRotation.rotationX);
    }

    public static OctahedralGroup fromXYZAngles(Quadrant xRotation, Quadrant yRotation, Quadrant zRotation) {
        return zRotation.rotationZ.compose(yRotation.rotationY.compose(xRotation.rotationX));
    }

    public int rotateVertexIndex(int index) {
        return (index + this.shift) % 4;
    }

    static {
        CODEC = Codec.INT.comapFlatMap(degrees -> switch (Mth.positiveModulo(degrees, 360)) {
            case 0 -> DataResult.success((Object)((Object)R0));
            case 90 -> DataResult.success((Object)((Object)R90));
            case 180 -> DataResult.success((Object)((Object)R180));
            case 270 -> DataResult.success((Object)((Object)R270));
            default -> DataResult.error(() -> "Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
        }, quadrant -> switch (quadrant.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> 0;
            case 1 -> 90;
            case 2 -> 180;
            case 3 -> 270;
        });
    }
}

