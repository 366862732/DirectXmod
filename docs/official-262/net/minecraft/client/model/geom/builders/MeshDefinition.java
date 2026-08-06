/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public class MeshDefinition {
    private final PartDefinition root;

    public MeshDefinition() {
        this(new PartDefinition((List<CubeDefinition>)ImmutableList.of(), PartPose.ZERO));
    }

    private MeshDefinition(PartDefinition root) {
        this.root = root;
    }

    public PartDefinition getRoot() {
        return this.root;
    }

    public MeshDefinition transformed(UnaryOperator<PartPose> function) {
        return new MeshDefinition(this.root.transformed(function));
    }

    public MeshDefinition apply(MeshTransformer transformer) {
        return transformer.apply(this);
    }
}

