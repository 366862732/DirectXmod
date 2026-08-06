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
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public record LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate, LootContext.EntityTarget entityTarget) implements LootItemCondition
{
    public static final MapCodec<LootItemEntityPropertyCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(LootItemEntityPropertyCondition::predicate), (App)LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(LootItemEntityPropertyCondition::entityTarget)).apply((Applicative)i, LootItemEntityPropertyCondition::new));

    public MapCodec<LootItemEntityPropertyCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ORIGIN, this.entityTarget.contextParam());
    }

    @Override
    public boolean test(LootContext context) {
        Entity entity = context.getOptionalParameter(this.entityTarget.contextParam());
        Vec3 pos = context.getOptionalParameter(LootContextParams.ORIGIN);
        return this.predicate.isEmpty() || this.predicate.get().matches(context.getLevel(), pos, entity);
    }

    public static LootItemCondition.Builder entityPresent(LootContext.EntityTarget target) {
        return LootItemEntityPropertyCondition.hasProperties(target, EntityPredicate.Builder.entity());
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget target, EntityPredicate.Builder predicate) {
        return () -> new LootItemEntityPropertyCondition(Optional.of(predicate.build()), target);
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget target, EntityPredicate predicate) {
        return () -> new LootItemEntityPropertyCondition(Optional.of(predicate), target);
    }
}

