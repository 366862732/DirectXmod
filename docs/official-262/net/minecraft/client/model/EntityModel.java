/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public abstract class EntityModel<T extends EntityRenderState>
extends Model<T> {
    public static final float MODEL_Y_OFFSET = -1.501f;

    protected EntityModel(ModelPart root) {
        this(root, RenderTypes::entityCutout);
    }

    protected EntityModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }
}

