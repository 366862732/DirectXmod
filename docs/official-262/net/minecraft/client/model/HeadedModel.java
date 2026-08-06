/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;

@Environment(value=EnvType.CLIENT)
public interface HeadedModel {
    public ModelPart getHead();

    default public void translateToHead(PoseStack poseStack) {
        this.getHead().translateAndRotate(poseStack);
    }
}

