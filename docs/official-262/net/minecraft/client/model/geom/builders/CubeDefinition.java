/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.model.geom.builders;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public final class CubeDefinition {
    private final @Nullable String comment;
    private final Vector3fc origin;
    private final Vector3fc dimensions;
    private final CubeDeformation grow;
    private final boolean mirror;
    private final UVPair texCoord;
    private final UVPair texScale;
    private final Set<Direction> visibleFaces;

    public CubeDefinition(@Nullable String comment, float xTexOffs, float yTexOffs, float minX, float minY, float minZ, float width, float height, float depth, CubeDeformation grow, boolean mirror, float xTexScale, float yTexScale, Set<Direction> visibleFaces) {
        this.comment = comment;
        this.texCoord = new UVPair(xTexOffs, yTexOffs);
        this.origin = new Vector3f(minX, minY, minZ);
        this.dimensions = new Vector3f(width, height, depth);
        this.grow = grow;
        this.mirror = mirror;
        this.texScale = new UVPair(xTexScale, yTexScale);
        this.visibleFaces = visibleFaces;
    }

    public ModelPart.Cube bake(int texScaleX, int texScaleY) {
        return new ModelPart.Cube((int)this.texCoord.u(), (int)this.texCoord.v(), this.origin.x(), this.origin.y(), this.origin.z(), this.dimensions.x(), this.dimensions.y(), this.dimensions.z(), this.grow.growX, this.grow.growY, this.grow.growZ, this.mirror, (float)texScaleX * this.texScale.u(), (float)texScaleY * this.texScale.v(), this.visibleFaces);
    }
}

