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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetPotionFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetPotionFunction.commonFields(i).and((App)Potion.CODEC.fieldOf("id").forGetter(f -> f.potion)).apply((Applicative)i, SetPotionFunction::new));
    private final Holder<Potion> potion;

    private SetPotionFunction(List<LootItemCondition> predicates, Holder<Potion> potion) {
        super(predicates);
        this.potion = potion;
    }

    public MapCodec<SetPotionFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, this.potion, PotionContents::withPotion);
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setPotion(Holder<Potion> value) {
        return SetPotionFunction.simpleBuilder(conditions -> new SetPotionFunction((List<LootItemCondition>)conditions, value));
    }
}

