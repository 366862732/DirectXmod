/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class OminousItemSpawnerRenderer
extends EntityRenderer<OminousItemSpawner, ItemClusterRenderState> {
    private static final float ROTATION_SPEED = 40.0f;
    private static final int TICKS_SCALING = 50;
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    protected OminousItemSpawnerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public ItemClusterRenderState createRenderState() {
        return new ItemClusterRenderState();
    }

    @Override
    public void extractRenderState(OminousItemSpawner entity, ItemClusterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ItemStack item = entity.getItem();
        state.extractItemGroupRenderState(entity, item, this.itemModelResolver);
    }

    @Override
    public void submit(ItemClusterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        if (state.ageInTicks <= 50.0f) {
            float scale = Math.min(state.ageInTicks, 50.0f) / 50.0f;
            poseStack.scale(scale, scale, scale);
        }
        float currentSpin = Mth.wrapDegrees(state.ageInTicks * 40.0f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(currentSpin));
        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, 0xF000F0, state, this.random);
        poseStack.popPose();
    }
}

