/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeLootItemCondition
implements LootItemCondition {
    protected final List<LootItemCondition> terms;
    private final Predicate<LootContext> composedPredicate;

    protected CompositeLootItemCondition(List<LootItemCondition> terms, Predicate<LootContext> composedPredicate) {
        this.terms = terms;
        this.composedPredicate = composedPredicate;
    }

    public abstract MapCodec<? extends CompositeLootItemCondition> codec();

    protected static <T extends CompositeLootItemCondition> MapCodec<T> createCodec(Function<List<LootItemCondition>, T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)LootItemCondition.DIRECT_CODEC.listOf().fieldOf("terms").forGetter(condition -> condition.terms)).apply((Applicative)i, factory));
    }

    protected static <T extends CompositeLootItemCondition> Codec<T> createInlineCodec(Function<List<LootItemCondition>, T> factory) {
        return LootItemCondition.DIRECT_CODEC.listOf().xmap(factory, condition -> condition.terms);
    }

    @Override
    public final boolean test(LootContext context) {
        return this.composedPredicate.test(context);
    }

    @Override
    public void validate(ValidationContext output) {
        LootItemCondition.super.validate(output);
        Validatable.validate(output, "terms", this.terms);
    }

    public static abstract class Builder
    implements LootItemCondition.Builder {
        private final ImmutableList.Builder<LootItemCondition> terms = ImmutableList.builder();

        protected Builder(LootItemCondition.Builder ... terms) {
            for (LootItemCondition.Builder term : terms) {
                this.terms.add((Object)term.build());
            }
        }

        public void addTerm(LootItemCondition.Builder term) {
            this.terms.add((Object)term.build());
        }

        @Override
        public LootItemCondition build() {
            return this.create((List<LootItemCondition>)this.terms.build());
        }

        protected abstract LootItemCondition create(List<LootItemCondition> var1);
    }
}

