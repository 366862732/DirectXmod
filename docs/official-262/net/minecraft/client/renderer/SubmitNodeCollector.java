/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;

@Environment(value=EnvType.CLIENT)
public interface SubmitNodeCollector
extends OrderedSubmitNodeCollector {
    public OrderedSubmitNodeCollector order(int var1);

    @Environment(value=EnvType.CLIENT)
    public static interface CustomGeometryRenderer {
        public void render(PoseStack.Pose var1, VertexConsumer var2);
    }
}

