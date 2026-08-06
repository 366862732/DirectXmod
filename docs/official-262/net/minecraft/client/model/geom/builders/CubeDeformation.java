/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom.builders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class CubeDeformation {
    public static final CubeDeformation NONE = new CubeDeformation(0.0f);
    final float growX;
    final float growY;
    final float growZ;

    public CubeDeformation(float growX, float growY, float growZ) {
        this.growX = growX;
        this.growY = growY;
        this.growZ = growZ;
    }

    public CubeDeformation(float grow) {
        this(grow, grow, grow);
    }

    public CubeDeformation extend(float factor) {
        return new CubeDeformation(this.growX + factor, this.growY + factor, this.growZ + factor);
    }

    public CubeDeformation extend(float factorX, float factorY, float factorZ) {
        return new CubeDeformation(this.growX + factorX, this.growY + factorY, this.growZ + factorZ);
    }
}

