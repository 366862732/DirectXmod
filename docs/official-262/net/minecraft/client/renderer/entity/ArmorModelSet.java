/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;

@Environment(value=EnvType.CLIENT)
public record ArmorModelSet<T>(T head, T chest, T legs, T feet) {
    public T get(EquipmentSlot slot) {
        return switch (slot) {
            case EquipmentSlot.HEAD -> this.head;
            case EquipmentSlot.CHEST -> this.chest;
            case EquipmentSlot.LEGS -> this.legs;
            case EquipmentSlot.FEET -> this.feet;
            default -> throw new IllegalStateException("No model for slot: " + String.valueOf(slot));
        };
    }

    public <U> ArmorModelSet<U> map(Function<? super T, ? extends U> mapper) {
        return new ArmorModelSet<U>(mapper.apply(this.head), mapper.apply(this.chest), mapper.apply(this.legs), mapper.apply(this.feet));
    }

    public void putFrom(ArmorModelSet<LayerDefinition> values, ImmutableMap.Builder<T, LayerDefinition> output) {
        output.put(this.head, (Object)((LayerDefinition)values.head));
        output.put(this.chest, (Object)((LayerDefinition)values.chest));
        output.put(this.legs, (Object)((LayerDefinition)values.legs));
        output.put(this.feet, (Object)((LayerDefinition)values.feet));
    }

    public static <M extends HumanoidModel<?>> ArmorModelSet<M> bake(ArmorModelSet<ModelLayerLocation> locations, EntityModelSet modelSet, Function<ModelPart, M> factory) {
        return locations.map(id -> (HumanoidModel)factory.apply(modelSet.bakeLayer((ModelLayerLocation)id)));
    }
}

