/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface NoDataSpecialModelRenderer
extends SpecialModelRenderer<Void> {
    @Override
    default public @Nullable Void extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    default public void submit(@Nullable Void argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        this.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, hasFoil, outlineColor);
    }

    public void submit(PoseStack var1, SubmitNodeCollector var2, int var3, int var4, boolean var5, int var6);

    @Environment(value=EnvType.CLIENT)
    public static interface Unbaked
    extends SpecialModelRenderer.Unbaked<Void> {
        @Override
        public MapCodec<? extends Unbaked> type();
    }
}

