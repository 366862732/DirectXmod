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
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition
{
    public static final MapCodec<DamageSourceCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DamageSourcePredicate.CODEC.optionalFieldOf("predicate").forGetter(DamageSourceCondition::predicate)).apply((Applicative)i, DamageSourceCondition::new));

    public MapCodec<DamageSourceCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
    }

    @Override
    public boolean test(LootContext context) {
        DamageSource damageSource = context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
        Vec3 pos = context.getOptionalParameter(LootContextParams.ORIGIN);
        if (pos == null || damageSource == null) {
            return false;
        }
        return this.predicate.isEmpty() || this.predicate.get().matches(context.getLevel(), pos, damageSource);
    }

    public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder builder) {
        return () -> new DamageSourceCondition(Optional.of(builder.build()));
    }
}

