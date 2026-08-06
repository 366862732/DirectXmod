/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class AnyOfCondition
extends CompositeLootItemCondition {
    public static final MapCodec<AnyOfCondition> MAP_CODEC = AnyOfCondition.createCodec(AnyOfCondition::new);

    private AnyOfCondition(List<LootItemCondition> terms) {
        super(terms, Util.anyOf(terms));
    }

    public MapCodec<AnyOfCondition> codec() {
        return MAP_CODEC;
    }

    public static Builder anyOf(LootItemCondition.Builder ... terms) {
        return new Builder(terms);
    }

    public static class Builder
    extends CompositeLootItemCondition.Builder {
        public Builder(LootItemCondition.Builder ... terms) {
            super(terms);
        }

        @Override
        public Builder or(LootItemCondition.Builder term) {
            this.addTerm(term);
            return this;
        }

        @Override
        protected LootItemCondition create(List<LootItemCondition> terms) {
            return new AnyOfCondition(terms);
        }
    }
}

