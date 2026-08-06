/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.core.Direction;

@Environment(value=EnvType.CLIENT)
public class CubeListBuilder {
    private static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);
    private final List<CubeDefinition> cubes = Lists.newArrayList();
    private int xTexOffs;
    private int yTexOffs;
    private boolean mirror;

    public CubeListBuilder texOffs(int xTexOffs, int yTexOffs) {
        this.xTexOffs = xTexOffs;
        this.yTexOffs = yTexOffs;
        return this;
    }

    public CubeListBuilder mirror() {
        return this.mirror(true);
    }

    public CubeListBuilder mirror(boolean mirror) {
        this.mirror = mirror;
        return this;
    }

    public CubeListBuilder addBox(String id, float x0, float y0, float z0, int w, int h, int d, CubeDeformation g, int xTexOffs, int yTexOffs) {
        this.texOffs(xTexOffs, yTexOffs);
        this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(String id, float x0, float y0, float z0, int w, int h, int d, int xTexOffs, int yTexOffs) {
        this.texOffs(xTexOffs, yTexOffs);
        this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(float x0, float y0, float z0, float w, float h, float d) {
        this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(float x0, float y0, float z0, float w, float h, float d, Set<Direction> visibleSides) {
        this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0f, 1.0f, visibleSides));
        return this;
    }

    public CubeListBuilder addBox(String id, float x0, float y0, float z0, float w, float h, float d) {
        this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(String id, float x0, float y0, float z0, float w, float h, float d, CubeDeformation g) {
        this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(float x0, float y0, float z0, float w, float h, float d, boolean mirror) {
        this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(float x0, float y0, float z0, float w, float h, float d, CubeDeformation g, float xTexScale, float yTexScale) {
        this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, xTexScale, yTexScale, ALL_VISIBLE));
        return this;
    }

    public CubeListBuilder addBox(float x0, float y0, float z0, float w, float h, float d, CubeDeformation g) {
        this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0f, 1.0f, ALL_VISIBLE));
        return this;
    }

    public List<CubeDefinition> getCubes() {
        return ImmutableList.copyOf(this.cubes);
    }

    public static CubeListBuilder create() {
        return new CubeListBuilder();
    }
}

