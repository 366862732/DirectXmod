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

import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public interface ModelState {
    public static final Matrix4fc NO_TRANSFORM = new Matrix4f();

    default public Transformation transformation() {
        return Transformation.IDENTITY;
    }

    default public Matrix4fc faceTransformation(Direction face) {
        return NO_TRANSFORM;
    }

    default public Matrix4fc inverseFaceTransformation(Direction face) {
        return NO_TRANSFORM;
    }
}

