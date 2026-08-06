/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public interface ModelBaker {
    public ResolvedModel getModel(Identifier var1);

    public BlockStateModelPart missingBlockModelPart();

    public MaterialBaker materials();

    public Interner interner();

    public <T> T compute(SharedOperationKey<T> var1);

    @Environment(value=EnvType.CLIENT)
    public static interface Interner {
        public Vector3fc vector(Vector3fc var1);

        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo var1);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface SharedOperationKey<T> {
        public T compute(ModelBaker var1);
    }
}

