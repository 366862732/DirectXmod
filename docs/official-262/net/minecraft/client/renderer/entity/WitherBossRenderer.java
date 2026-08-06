/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;

@Environment(value=EnvType.CLIENT)
public class WitherBossRenderer
extends MobRenderer<WitherBoss, WitherRenderState, WitherBossModel> {
    private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");

    public WitherBossRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherBossModel(context.bakeLayer(ModelLayers.WITHER)), 1.0f);
        this.addLayer(new WitherArmorLayer(this, context.getModelSet()));
    }

    @Override
    protected int getBlockLightLevel(WitherBoss entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(WitherRenderState state) {
        int invulnerableTicks = Mth.floor(state.invulnerableTicks);
        if (invulnerableTicks <= 0 || invulnerableTicks <= 80 && invulnerableTicks / 5 % 2 == 1) {
            return WITHER_LOCATION;
        }
        return WITHER_INVULNERABLE_LOCATION;
    }

    @Override
    public WitherRenderState createRenderState() {
        return new WitherRenderState();
    }

    @Override
    protected void scale(WitherRenderState state, PoseStack poseStack) {
        float scale = 2.0f;
        if (state.invulnerableTicks > 0.0f) {
            scale -= state.invulnerableTicks / 220.0f * 0.5f;
        }
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void extractRenderState(WitherBoss entity, WitherRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int invulnerableTicks = entity.getInvulnerableTicks();
        state.invulnerableTicks = invulnerableTicks > 0 ? (float)invulnerableTicks - partialTicks : 0.0f;
        System.arraycopy(entity.getHeadXRots(), 0, state.xHeadRots, 0, state.xHeadRots.length);
        System.arraycopy(entity.getHeadYRots(), 0, state.yHeadRots, 0, state.yHeadRots.length);
        state.isPowered = entity.isPowered();
    }
}

