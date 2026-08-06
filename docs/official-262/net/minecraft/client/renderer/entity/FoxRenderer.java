/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.fox.AdultFoxModel;
import net.minecraft.client.model.animal.fox.BabyFoxModel;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fox.Fox;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class FoxRenderer
extends AgeableMobRenderer<Fox, FoxRenderState, FoxModel> {
    private static final Identifier RED_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox.png");
    private static final Identifier RED_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_sleep.png");
    private static final Identifier SNOW_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_snow.png");
    private static final Identifier SNOW_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_snow_sleep.png");
    private static final Identifier BABY_RED_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_baby.png");
    private static final Identifier BABY_RED_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_sleep_baby.png");
    private static final Identifier BABY_SNOW_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_snow_baby.png");
    private static final Identifier BABY_SNOW_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_snow_sleep_baby.png");
    private static final EnumMap<Fox.Variant, FoxTexturesByState> TEXTURES_BY_VARIANT = Maps.newEnumMap(Map.of(Fox.Variant.RED, new FoxTexturesByState(new FoxTexturesByAge(RED_FOX_TEXTURE, BABY_RED_FOX_TEXTURE), new FoxTexturesByAge(RED_FOX_SLEEP_TEXTURE, BABY_RED_FOX_SLEEP_TEXTURE)), Fox.Variant.SNOW, new FoxTexturesByState(new FoxTexturesByAge(SNOW_FOX_TEXTURE, BABY_SNOW_FOX_TEXTURE), new FoxTexturesByAge(SNOW_FOX_SLEEP_TEXTURE, BABY_SNOW_FOX_SLEEP_TEXTURE))));

    public FoxRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultFoxModel(context.bakeLayer(ModelLayers.FOX)), new BabyFoxModel(context.bakeLayer(ModelLayers.FOX_BABY)), 0.4f);
        this.addLayer(new FoxHeldItemLayer(this));
    }

    @Override
    protected void setupRotations(FoxRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (state.isPouncing || state.isFaceplanted) {
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-state.xRot));
        }
    }

    @Override
    public Identifier getTextureLocation(FoxRenderState state) {
        FoxTexturesByState byState = TEXTURES_BY_VARIANT.get(state.variant);
        if (byState == null) {
            return RED_FOX_TEXTURE;
        }
        FoxTexturesByAge ageTextures = state.isSleeping ? byState.sleeping() : byState.idle();
        return state.isBaby ? ageTextures.baby() : ageTextures.adult();
    }

    @Override
    public FoxRenderState createRenderState() {
        return new FoxRenderState();
    }

    @Override
    public void extractRenderState(Fox entity, FoxRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
        state.headRollAngle = entity.getHeadRollAngle(partialTicks);
        state.isCrouching = entity.isCrouching();
        state.crouchAmount = entity.getCrouchAmount(partialTicks);
        state.isSleeping = entity.isSleeping();
        state.isSitting = entity.isSitting();
        state.isFaceplanted = entity.isFaceplanted();
        state.isPouncing = entity.isPouncing();
        state.variant = entity.getVariant();
    }

    @Environment(value=EnvType.CLIENT)
    private record FoxTexturesByState(FoxTexturesByAge idle, FoxTexturesByAge sleeping) {
    }

    @Environment(value=EnvType.CLIENT)
    private record FoxTexturesByAge(Identifier adult, Identifier baby) {
    }
}

