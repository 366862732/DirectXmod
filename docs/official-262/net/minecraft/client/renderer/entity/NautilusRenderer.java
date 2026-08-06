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
import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;

@Environment(value=EnvType.CLIENT)
public class NautilusRenderer<T extends AbstractNautilus>
extends AgeableMobRenderer<T, NautilusRenderState, NautilusModel> {
    private static final Identifier NAUTILUS_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus.png");
    private static final Identifier NAUTILUS_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus_baby.png");

    public NautilusRenderer(EntityRendererProvider.Context context) {
        super(context, new NautilusModel(context.bakeLayer(ModelLayers.NAUTILUS)), new NautilusModel(context.bakeLayer(ModelLayers.NAUTILUS_BABY)), 0.7f);
        this.addLayer(new SimpleEquipmentLayer<NautilusRenderState, NautilusModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.NAUTILUS_BODY, state -> state.bodyArmorItem, new NautilusArmorModel(context.bakeLayer(ModelLayers.NAUTILUS_ARMOR)), null));
        this.addLayer(new SimpleEquipmentLayer<NautilusRenderState, NautilusModel, Object>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.NAUTILUS_SADDLE, state -> state.saddle, new NautilusSaddleModel(context.bakeLayer(ModelLayers.NAUTILUS_SADDLE)), null));
    }

    @Override
    public Identifier getTextureLocation(NautilusRenderState state) {
        return state.isBaby ? NAUTILUS_BABY_LOCATION : NAUTILUS_LOCATION;
    }

    @Override
    public NautilusRenderState createRenderState() {
        return new NautilusRenderState();
    }

    @Override
    public void extractRenderState(T entity, NautilusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.saddle = ((LivingEntity)entity).getItemBySlot(EquipmentSlot.SADDLE).copy();
        state.bodyArmorItem = ((Mob)entity).getBodyArmorItem().copy();
    }
}

