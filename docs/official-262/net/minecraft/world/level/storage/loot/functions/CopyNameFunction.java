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
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyNameFunction
extends LootItemConditionalFunction {
    public static final MapCodec<CopyNameFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> CopyNameFunction.commonFields(i).and((App)LootContextArg.ENTITY_OR_BLOCK.fieldOf("source").forGetter(f -> f.source)).apply((Applicative)i, CopyNameFunction::new));
    private final LootContextArg<Object> source;

    private CopyNameFunction(List<LootItemCondition> predicates, LootContextArg<?> source) {
        super(predicates);
        this.source = LootContextArg.cast(source);
    }

    public MapCodec<CopyNameFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Object maybeNameable = this.source.get(context);
        if (maybeNameable instanceof Nameable) {
            Nameable nameable = (Nameable)maybeNameable;
            itemStack.set(DataComponents.CUSTOM_NAME, nameable.getCustomName());
        }
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> copyName(LootContextArg<?> target) {
        return CopyNameFunction.simpleBuilder(conditions -> new CopyNameFunction((List<LootItemCondition>)conditions, target));
    }
}

