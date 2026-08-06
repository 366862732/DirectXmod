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
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Phantom;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class PhantomRenderer
extends MobRenderer<Phantom, PhantomRenderState, PhantomModel> {
    private static final Identifier PHANTOM_LOCATION = Identifier.withDefaultNamespace("textures/entity/phantom/phantom.png");

    public PhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.75f);
        this.addLayer(new PhantomEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(PhantomRenderState state) {
        return PHANTOM_LOCATION;
    }

    @Override
    public PhantomRenderState createRenderState() {
        return new PhantomRenderState();
    }

    @Override
    public void extractRenderState(Phantom entity, PhantomRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flapTime = (float)entity.getUniqueFlapTickOffset() + state.ageInTicks;
        state.size = entity.getPhantomSize();
    }

    @Override
    protected void scale(PhantomRenderState state, PoseStack poseStack) {
        float scale = 1.0f + 0.15f * (float)state.size;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0f, 1.3125f, 0.1875f);
    }

    @Override
    protected void setupRotations(PhantomRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(state.xRot));
    }
}

