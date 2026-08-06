/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SmeltItemFunction
extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final boolean useInputCount;
    public static final MapCodec<SmeltItemFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SmeltItemFunction.commonFields(i).and((App)Codec.BOOL.optionalFieldOf("use_input_count", (Object)true).forGetter(o -> o.useInputCount)).apply((Applicative)i, SmeltItemFunction::new));

    private SmeltItemFunction(List<LootItemCondition> predicates, boolean useInputCount) {
        super(predicates);
        this.useInputCount = useInputCount;
    }

    public MapCodec<SmeltItemFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        ItemStack result;
        if (itemStack.isEmpty()) {
            return itemStack;
        }
        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = context.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING, input, context.getLevel());
        if (recipe.isPresent() && !(result = recipe.get().value().assemble(input)).isEmpty()) {
            int newCount = (this.useInputCount ? itemStack.count() : 1) * result.getCount();
            return result.copyWithCount(Math.min(newCount, result.getMaxStackSize()));
        }
        LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", (Object)itemStack);
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> smelted() {
        return SmeltItemFunction.smelted(true);
    }

    public static LootItemConditionalFunction.Builder<?> smelted(boolean useInputCount) {
        return SmeltItemFunction.simpleBuilder(predicates -> new SmeltItemFunction((List<LootItemCondition>)predicates, useInputCount));
    }
}

