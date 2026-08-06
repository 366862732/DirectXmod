/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetItemCountFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetItemCountFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetItemCountFunction.commonFields(i).and(i.group((App)NumberProviders.CODEC.fieldOf("count").forGetter(f -> f.count), (App)Codec.BOOL.optionalFieldOf("add", (Object)false).forGetter(f -> f.add))).apply((Applicative)i, SetItemCountFunction::new));
    private final NumberProvider count;
    private final boolean add;

    private SetItemCountFunction(List<LootItemCondition> predicates, NumberProvider count, boolean add) {
        super(predicates);
        this.count = count;
        this.add = add;
    }

    public MapCodec<SetItemCountFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "count", this.count);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        int base = this.add ? itemStack.getCount() : 0;
        itemStack.setCount(base + this.count.getInt(context));
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider count) {
        return SetItemCountFunction.simpleBuilder(conditions -> new SetItemCountFunction((List<LootItemCondition>)conditions, count, false));
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider count, boolean add) {
        return SetItemCountFunction.simpleBuilder(conditions -> new SetItemCountFunction((List<LootItemCondition>)conditions, count, add));
    }
}

