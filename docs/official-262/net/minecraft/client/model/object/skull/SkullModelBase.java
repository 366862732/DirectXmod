/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.skull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Environment(value=EnvType.CLIENT)
public abstract class SkullModelBase
extends Model<State> {
    public SkullModelBase(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
    }

    @Environment(value=EnvType.CLIENT)
    public static class State {
        public float animationPos;
        public float yRot;
        public float xRot;
    }
}

