/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record ComponentContents<T>(DataComponentType<T> componentType) implements SelectItemModelProperty<T>
{
    private static final SelectItemModelProperty.Type<? extends ComponentContents<?>, ?> TYPE = ComponentContents.createType();

    private static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> createType() {
        Codec rawComponentCodec;
        Codec componentCodec = rawComponentCodec = BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().validate(t -> {
            if (t.isTransient()) {
                return DataResult.error(() -> "Component can't be serialized");
            }
            return DataResult.success((Object)t);
        });
        MapCodec switchCodec = componentCodec.dispatchMap("component", switchObject -> ((ComponentContents)switchObject.property()).componentType, componentType -> SelectItemModelProperty.Type.createCasesFieldCodec(componentType.codecOrThrow()).xmap(cases -> new SelectItemModel.UnbakedSwitch(new ComponentContents(componentType), cases), SelectItemModel.UnbakedSwitch::cases));
        return new SelectItemModelProperty.Type(switchCodec);
    }

    public static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> castType() {
        return TYPE;
    }

    @Override
    public @Nullable T get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return itemStack.get(this.componentType);
    }

    @Override
    public SelectItemModelProperty.Type<ComponentContents<T>, T> type() {
        return ComponentContents.castType();
    }

    @Override
    public Codec<T> valueCodec() {
        return this.componentType.codecOrThrow();
    }
}

