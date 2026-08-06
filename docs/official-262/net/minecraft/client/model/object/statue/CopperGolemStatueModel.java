/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.statue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

@Environment(value=EnvType.CLIENT)
public class CopperGolemStatueModel
extends Model<Unit> {
    public CopperGolemStatueModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    @Override
    public void setupAnim(Unit ignored) {
        this.root.y = 0.0f;
        this.root.zRot = (float)Math.PI;
    }
}

