/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.model.animal.bee.BabyBeeModel;
import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.bee.Bee;

@Environment(value=EnvType.CLIENT)
public class BeeRenderer
extends AgeableMobRenderer<Bee, BeeRenderState, BeeModel> {
    private static final Identifier ANGRY_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry.png");
    private static final Identifier ANGRY_NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry_nectar.png");
    private static final Identifier BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee.png");
    private static final Identifier NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_nectar.png");
    private static final Identifier ANGRY_BEE_BABY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry_baby.png");
    private static final Identifier ANGRY_NECTAR_BEE_BABY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry_nectar_baby.png");
    private static final Identifier BEE_BABY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_baby.png");
    private static final Identifier NECTAR_BEE_BABY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_nectar_baby.png");

    public BeeRenderer(EntityRendererProvider.Context context) {
        super(context, new AdultBeeModel(context.bakeLayer(ModelLayers.BEE)), new BabyBeeModel(context.bakeLayer(ModelLayers.BEE_BABY)), 0.4f);
    }

    @Override
    public Identifier getTextureLocation(BeeRenderState state) {
        if (state.isAngry) {
            if (state.hasNectar) {
                return state.isBaby ? ANGRY_NECTAR_BEE_BABY_TEXTURE : ANGRY_NECTAR_BEE_TEXTURE;
            }
            return state.isBaby ? ANGRY_BEE_BABY_TEXTURE : ANGRY_BEE_TEXTURE;
        }
        if (state.hasNectar) {
            return state.isBaby ? NECTAR_BEE_BABY_TEXTURE : NECTAR_BEE_TEXTURE;
        }
        return state.isBaby ? BEE_BABY_TEXTURE : BEE_TEXTURE;
    }

    @Override
    public BeeRenderState createRenderState() {
        return new BeeRenderState();
    }

    @Override
    public void extractRenderState(Bee entity, BeeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rollAmount = entity.getRollAmount(partialTicks);
        state.hasStinger = !entity.hasStung();
        state.isOnGround = entity.onGround() && entity.getDeltaMovement().lengthSqr() < 1.0E-7;
        state.isAngry = entity.isAngry();
        state.hasNectar = entity.hasNectar();
    }
}

