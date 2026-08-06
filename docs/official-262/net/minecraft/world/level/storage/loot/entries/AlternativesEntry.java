/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.ComposableEntryContainer;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class AlternativesEntry
extends CompositeEntryBase {
    public static final MapCodec<AlternativesEntry> MAP_CODEC = AlternativesEntry.createCodec(AlternativesEntry::new);
    public static final ProblemReporter.Problem UNREACHABLE_PROBLEM = new ProblemReporter.Problem(){

        @Override
        public String description() {
            return "Unreachable entry!";
        }
    };

    public AlternativesEntry(List<LootPoolEntryContainer> children, List<LootItemCondition> conditions) {
        super(children, conditions);
    }

    public MapCodec<AlternativesEntry> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> entries) {
        return switch (entries.size()) {
            case 0 -> ALWAYS_FALSE;
            case 1 -> entries.get(0);
            case 2 -> entries.get(0).or(entries.get(1));
            default -> (context, output) -> {
                for (ComposableEntryContainer entry : entries) {
                    if (!entry.expand(context, output)) continue;
                    return true;
                }
                return false;
            };
        };
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        for (int i = 0; i < this.children.size() - 1; ++i) {
            if (!((LootPoolEntryContainer)this.children.get((int)i)).conditions.isEmpty()) continue;
            context.reportProblem(UNREACHABLE_PROBLEM);
        }
    }

    public static Builder alternatives(LootPoolEntryContainer.Builder<?> ... entries) {
        return new Builder(entries);
    }

    public static <E> Builder alternatives(Collection<E> items, Function<E, LootPoolEntryContainer.Builder<?>> provider) {
        return new Builder((LootPoolEntryContainer.Builder[])items.stream().map(provider::apply).toArray(LootPoolEntryContainer.Builder[]::new));
    }

    public static class Builder
    extends LootPoolEntryContainer.Builder<Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

        public Builder(LootPoolEntryContainer.Builder<?> ... entries) {
            for (LootPoolEntryContainer.Builder<?> entry : entries) {
                this.entries.add((Object)entry.build());
            }
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public Builder otherwise(LootPoolEntryContainer.Builder<?> other) {
            this.entries.add((Object)other.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new AlternativesEntry((List<LootPoolEntryContainer>)this.entries.build(), this.getConditions());
        }
    }
}

