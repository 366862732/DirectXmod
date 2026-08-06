/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class AllOfCondition
extends CompositeLootItemCondition {
    public static final MapCodec<AllOfCondition> MAP_CODEC = AllOfCondition.createCodec(AllOfCondition::new);
    public static final Codec<AllOfCondition> INLINE_CODEC = AllOfCondition.createInlineCodec(AllOfCondition::new);

    private AllOfCondition(List<LootItemCondition> terms) {
        super(terms, Util.allOf(terms));
    }

    public static AllOfCondition allOf(List<LootItemCondition> terms) {
        return new AllOfCondition(List.copyOf(terms));
    }

    public MapCodec<AllOfCondition> codec() {
        return MAP_CODEC;
    }

    public static Builder allOf(LootItemCondition.Builder ... terms) {
        return new Builder(terms);
    }

    public static class Builder
    extends CompositeLootItemCondition.Builder {
        public Builder(LootItemCondition.Builder ... terms) {
            super(terms);
        }

        @Override
        public Builder and(LootItemCondition.Builder term) {
            this.addTerm(term);
            return this;
        }

        @Override
        protected LootItemCondition create(List<LootItemCondition> terms) {
            return new AllOfCondition(terms);
        }
    }
}

