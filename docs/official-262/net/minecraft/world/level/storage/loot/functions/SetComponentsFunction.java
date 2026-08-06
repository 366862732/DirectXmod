/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetComponentsFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetComponentsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetComponentsFunction.commonFields(i).and((App)DataComponentPatch.CODEC.fieldOf("components").forGetter(f -> f.components)).apply((Applicative)i, SetComponentsFunction::new));
    private final DataComponentPatch components;

    private SetComponentsFunction(List<LootItemCondition> predicates, DataComponentPatch components) {
        super(predicates);
        this.components = components;
    }

    public MapCodec<SetComponentsFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.applyComponentsAndValidate(this.components);
        return itemStack;
    }

    public static <T> LootItemConditionalFunction.Builder<?> setComponent(DataComponentType<T> type, T value) {
        return SetComponentsFunction.simpleBuilder(conditions -> new SetComponentsFunction((List<LootItemCondition>)conditions, DataComponentPatch.builder().set(type, value).build()));
    }
}

