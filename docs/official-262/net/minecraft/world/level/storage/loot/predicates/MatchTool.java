/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record MatchTool(Optional<ItemPredicate> predicate) implements LootItemCondition
{
    public static final MapCodec<MatchTool> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ItemPredicate.CODEC.optionalFieldOf("predicate").forGetter(MatchTool::predicate)).apply((Applicative)i, MatchTool::new));

    public MapCodec<MatchTool> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.TOOL);
    }

    @Override
    public boolean test(LootContext context) {
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        return tool != null && (this.predicate.isEmpty() || this.predicate.get().test(tool));
    }

    public static LootItemCondition.Builder toolMatches(ItemPredicate.Builder predicate) {
        return () -> new MatchTool(Optional.of(predicate.build()));
    }
}

