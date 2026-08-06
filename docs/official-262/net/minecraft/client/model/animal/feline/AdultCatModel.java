/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.animal.feline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.feline.AdultFelineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.entity.state.CatRenderState;

@Environment(value=EnvType.CLIENT)
public class AdultCatModel
extends AdultFelineModel<CatRenderState> {
    public static final MeshTransformer CAT_TRANSFORMER = MeshTransformer.scaling(0.8f);
    public static final CubeDeformation COLLAR_DEFORMATION = new CubeDeformation(0.01f);

    public AdultCatModel(ModelPart root) {
        super(root);
    }
}

