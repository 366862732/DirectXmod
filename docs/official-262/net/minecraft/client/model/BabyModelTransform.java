/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;

@Environment(value=EnvType.CLIENT)
public record BabyModelTransform(boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset, float babyHeadScale, float babyBodyScale, float bodyYOffset, Set<String> headParts) implements MeshTransformer
{
    public BabyModelTransform(Set<String> headParts) {
        this(false, 5.0f, 2.0f, headParts);
    }

    public BabyModelTransform(boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset, Set<String> headParts) {
        this(scaleHead, babyYHeadOffset, babyZHeadOffset, 2.0f, 2.0f, 24.0f, headParts);
    }

    @Override
    public MeshDefinition apply(MeshDefinition mesh) {
        float headScale = this.scaleHead ? 1.5f / this.babyHeadScale : 1.0f;
        float bodyScale = 1.0f / this.babyBodyScale;
        UnaryOperator headTransform = p -> p.translated(0.0f, this.babyYHeadOffset, this.babyZHeadOffset).scaled(headScale);
        UnaryOperator bodyTransform = p -> p.translated(0.0f, this.bodyYOffset, 0.0f).scaled(bodyScale);
        MeshDefinition babyMesh = new MeshDefinition();
        for (Map.Entry<String, PartDefinition> entry : mesh.getRoot().getChildren()) {
            String name = entry.getKey();
            PartDefinition part = entry.getValue();
            babyMesh.getRoot().addOrReplaceChild(name, part.transformed(this.headParts.contains(name) ? headTransform : bodyTransform));
        }
        return babyMesh;
    }
}

