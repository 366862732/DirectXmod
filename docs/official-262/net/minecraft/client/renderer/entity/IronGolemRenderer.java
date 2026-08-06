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
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class IronGolemRenderer
extends MobRenderer<IronGolem, IronGolemRenderState, IronGolemModel> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier GOLEM_LOCATION = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");
    private final BlockModelResolver blockModelResolver;

    public IronGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7f);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new IronGolemCrackinessLayer(this));
        this.addLayer(new IronGolemFlowerLayer(this));
    }

    @Override
    public Identifier getTextureLocation(IronGolemRenderState state) {
        return GOLEM_LOCATION;
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public void extractRenderState(IronGolem entity, IronGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackTicksRemaining = (float)entity.getAttackAnimationTick() > 0.0f ? (float)entity.getAttackAnimationTick() - partialTicks : 0.0f;
        state.offerFlowerTick = entity.getOfferFlowerTick();
        if (state.offerFlowerTick > 0) {
            this.blockModelResolver.update(state.flowerBlock, Blocks.POPPY.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
        } else {
            state.flowerBlock.clear();
        }
        state.crackiness = entity.getCrackiness();
    }

    @Override
    protected void setupRotations(IronGolemRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if ((double)state.walkAnimationSpeed < 0.01) {
            return;
        }
        float p = 13.0f;
        float wp = state.walkAnimationPos + 6.0f;
        float triangleWave = (Math.abs(wp % 13.0f - 6.5f) - 3.25f) / 3.25f;
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(6.5f * triangleWave));
    }
}

