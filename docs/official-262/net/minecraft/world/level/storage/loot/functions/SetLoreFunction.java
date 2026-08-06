/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ListOperation;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class SetLoreFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetLoreFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetLoreFunction.commonFields(i).and(i.group((App)ComponentSerialization.CODEC.sizeLimitedListOf(256).fieldOf("lore").forGetter(f -> f.lore), (App)ListOperation.codec(256).forGetter(f -> f.mode), (App)LootContext.EntityTarget.CODEC.optionalFieldOf("entity").forGetter(f -> f.resolutionContext))).apply((Applicative)i, SetLoreFunction::new));
    private final List<Component> lore;
    private final ListOperation mode;
    private final Optional<LootContext.EntityTarget> resolutionContext;

    public SetLoreFunction(List<LootItemCondition> predicates, List<Component> lore, ListOperation mode, Optional<LootContext.EntityTarget> resolutionContext) {
        super(predicates);
        this.lore = List.copyOf(lore);
        this.mode = mode;
        this.resolutionContext = resolutionContext;
    }

    public MapCodec<SetLoreFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return (Set)DataFixUtils.orElse(this.resolutionContext.map(target -> Set.of(target.contextParam())), Set.of());
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.LORE, ItemLore.EMPTY, oldLore -> new ItemLore(this.updateLore((ItemLore)oldLore, context)));
        return itemStack;
    }

    private List<Component> updateLore(@Nullable ItemLore itemLore, LootContext context) {
        if (itemLore == null && this.lore.isEmpty()) {
            return List.of();
        }
        UnaryOperator<Component> resolver = SetNameFunction.createResolver(context, this.resolutionContext.orElse(null));
        List resolvedLines = this.lore.stream().map(resolver).toList();
        return this.mode.apply(itemLore.lines(), resolvedLines, 256);
    }

    public static Builder setLore() {
        return new Builder();
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private Optional<LootContext.EntityTarget> resolutionContext = Optional.empty();
        private final ImmutableList.Builder<Component> lore = ImmutableList.builder();
        private ListOperation mode = ListOperation.Append.INSTANCE;

        public Builder setMode(ListOperation mode) {
            this.mode = mode;
            return this;
        }

        public Builder setResolutionContext(LootContext.EntityTarget resolutionContext) {
            this.resolutionContext = Optional.of(resolutionContext);
            return this;
        }

        public Builder addLine(Component line) {
            this.lore.add((Object)line);
            return this;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetLoreFunction(this.getConditions(), (List<Component>)this.lore.build(), this.mode, this.resolutionContext);
        }
    }
}

