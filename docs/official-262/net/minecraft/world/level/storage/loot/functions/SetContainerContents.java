/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerContents
extends LootItemConditionalFunction {
    public static final MapCodec<SetContainerContents> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetContainerContents.commonFields(i).and(i.group((App)ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(f -> f.component), (App)LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(f -> f.entries))).apply((Applicative)i, SetContainerContents::new));
    private final ContainerComponentManipulator<?> component;
    private final List<LootPoolEntryContainer> entries;

    private SetContainerContents(List<LootItemCondition> predicates, ContainerComponentManipulator<?> component, List<LootPoolEntryContainer> entries) {
        super(predicates);
        this.component = component;
        this.entries = List.copyOf(entries);
    }

    public MapCodec<SetContainerContents> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        if (itemStack.isEmpty()) {
            return itemStack;
        }
        Stream.Builder contents = Stream.builder();
        this.entries.forEach(e -> e.expand(context, entry -> entry.createItemStack(LootTable.createStackSplitter(context.getLevel(), contents::add), context)));
        this.component.setContents(itemStack, contents.build());
        return itemStack;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "entries", this.entries);
    }

    public static Builder setContents(ContainerComponentManipulator<?> component) {
        return new Builder(component);
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
        private final ContainerComponentManipulator<?> component;

        public Builder(ContainerComponentManipulator<?> component) {
            this.component = component;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withEntry(LootPoolEntryContainer.Builder<?> entry) {
            this.entries.add((Object)entry.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetContainerContents(this.getConditions(), this.component, (List<LootPoolEntryContainer>)this.entries.build());
        }
    }
}

