/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState
extends LootItemConditionalFunction {
    public static final MapCodec<CopyBlockState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> CopyBlockState.commonFields(i).and(i.group((App)BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(f -> f.block), (App)Codec.STRING.listOf().fieldOf("properties").forGetter(f -> f.properties.stream().map(Property::getName).toList()))).apply((Applicative)i, CopyBlockState::new));
    private final Holder<Block> block;
    private final Set<Property<?>> properties;

    private CopyBlockState(List<LootItemCondition> predicates, Holder<Block> block, Set<Property<?>> properties) {
        super(predicates);
        this.block = block;
        this.properties = properties;
    }

    private CopyBlockState(List<LootItemCondition> predicates, Holder<Block> block, List<String> propertyNames) {
        this(predicates, block, propertyNames.stream().map(block.value().getStateDefinition()::getProperty).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    public MapCodec<CopyBlockState> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_STATE);
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (state != null) {
            itemStack.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY, itemState -> {
                for (Property<?> property : this.properties) {
                    if (!state.hasProperty(property)) continue;
                    itemState = itemState.with(property, state);
                }
                return itemState;
            });
        }
        return itemStack;
    }

    public static Builder copyState(Block block) {
        return new Builder(block);
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final Holder<Block> block;
        private final ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

        private Builder(Block block) {
            this.block = block.builtInRegistryHolder();
        }

        public Builder copy(Property<?> property) {
            if (!this.block.value().getStateDefinition().getProperties().contains(property)) {
                throw new IllegalStateException("Property " + String.valueOf(property) + " is not present on block " + String.valueOf(this.block));
            }
            this.properties.add(property);
            return this;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyBlockState(this.getConditions(), this.block, (Set<Property<?>>)this.properties.build());
        }
    }
}

