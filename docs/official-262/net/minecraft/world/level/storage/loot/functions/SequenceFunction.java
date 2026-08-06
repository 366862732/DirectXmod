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
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

public class SequenceFunction
implements LootItemFunction {
    public static final MapCodec<SequenceFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LootItemFunctions.TYPED_CODEC.listOf().fieldOf("functions").forGetter(f -> f.functions)).apply((Applicative)i, SequenceFunction::new));
    public static final Codec<SequenceFunction> INLINE_CODEC = LootItemFunctions.TYPED_CODEC.listOf().xmap(SequenceFunction::new, f -> f.functions);
    private final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

    private SequenceFunction(List<LootItemFunction> functions) {
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
    }

    public static SequenceFunction of(List<LootItemFunction> functions) {
        return new SequenceFunction(List.copyOf(functions));
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        return this.compositeFunction.apply(stack, context);
    }

    @Override
    public void validate(ValidationContext output) {
        LootItemFunction.super.validate(output);
        Validatable.validate(output, "functions", this.functions);
    }

    public MapCodec<SequenceFunction> codec() {
        return MAP_CODEC;
    }
}

