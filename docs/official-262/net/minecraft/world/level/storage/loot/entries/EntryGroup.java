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
import java.util.List;
import net.minecraft.world.level.storage.loot.entries.ComposableEntryContainer;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class EntryGroup
extends CompositeEntryBase {
    public static final MapCodec<EntryGroup> MAP_CODEC = EntryGroup.createCodec(EntryGroup::new);

    public EntryGroup(List<LootPoolEntryContainer> children, List<LootItemCondition> conditions) {
        super(children, conditions);
    }

    public MapCodec<EntryGroup> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> entries) {
        return switch (entries.size()) {
            case 0 -> ALWAYS_TRUE;
            case 1 -> entries.get(0);
            case 2 -> {
                ComposableEntryContainer first = entries.get(0);
                ComposableEntryContainer second = entries.get(1);
                yield (context, output) -> {
                    first.expand(context, output);
                    second.expand(context, output);
                    return true;
                };
            }
            default -> (context, output) -> {
                for (ComposableEntryContainer entry : entries) {
                    entry.expand(context, output);
                }
                return true;
            };
        };
    }

    public static Builder list(LootPoolEntryContainer.Builder<?> ... entries) {
        return new Builder(entries);
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
        public Builder append(LootPoolEntryContainer.Builder<?> other) {
            this.entries.add((Object)other.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new EntryGroup((List<LootPoolEntryContainer>)this.entries.build(), this.getConditions());
        }
    }
}

