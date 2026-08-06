/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.properties.numeric.BundleFullness;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.Cooldown;
import net.minecraft.client.renderer.item.properties.numeric.Count;
import net.minecraft.client.renderer.item.properties.numeric.CrossbowPull;
import net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.Time;
import net.minecraft.client.renderer.item.properties.numeric.UseCycle;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

@Environment(value=EnvType.CLIENT)
public class RangeSelectItemModelProperties {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final MapCodec<RangeSelectItemModelProperty> MAP_CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatchMap("property", RangeSelectItemModelProperty::type, c -> c);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.withDefaultNamespace("custom_model_data"), CustomModelDataProperty.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("bundle/fullness"), BundleFullness.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("damage"), Damage.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("cooldown"), Cooldown.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("time"), Time.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("compass"), CompassAngle.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("crossbow/pull"), CrossbowPull.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("use_cycle"), UseCycle.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("use_duration"), UseDuration.MAP_CODEC);
        ID_MAPPER.put(Identifier.withDefaultNamespace("count"), Count.MAP_CODEC);
    }
}

