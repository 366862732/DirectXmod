/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

public record EntityHasScoreCondition(Map<String, IntRange> scores, LootContext.EntityTarget entityTarget) implements LootItemCondition
{
    public static final MapCodec<EntityHasScoreCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.unboundedMap((Codec)Codec.STRING, IntRange.CODEC).fieldOf("scores").forGetter(EntityHasScoreCondition::scores), (App)LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(EntityHasScoreCondition::entityTarget)).apply((Applicative)i, EntityHasScoreCondition::new));

    public MapCodec<EntityHasScoreCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.entityTarget.contextParam());
    }

    @Override
    public void validate(ValidationContext context) {
        LootItemCondition.super.validate(context);
        this.scores.forEach((score, value) -> value.validate(context.forMapField("scores", (String)score)));
    }

    @Override
    public boolean test(LootContext context) {
        Entity entity = context.getOptionalParameter(this.entityTarget.contextParam());
        if (entity == null) {
            return false;
        }
        ServerScoreboard scoreboard = context.getLevel().getScoreboard();
        for (Map.Entry<String, IntRange> entry : this.scores.entrySet()) {
            if (this.hasScore(context, entity, scoreboard, entry.getKey(), entry.getValue())) continue;
            return false;
        }
        return true;
    }

    private boolean hasScore(LootContext context, Entity entity, Scoreboard scoreboard, String objectiveName, IntRange range) {
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return false;
        }
        ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(entity, objective);
        if (scoreInfo == null) {
            return false;
        }
        return range.test(context, scoreInfo.value());
    }

    public static Builder hasScores(LootContext.EntityTarget target) {
        return new Builder(target);
    }

    public static class Builder
    implements LootItemCondition.Builder {
        private final ImmutableMap.Builder<String, IntRange> scores = ImmutableMap.builder();
        private final LootContext.EntityTarget entityTarget;

        public Builder(LootContext.EntityTarget entityTarget) {
            this.entityTarget = entityTarget;
        }

        public Builder withScore(String score, IntRange bounds) {
            this.scores.put((Object)score, (Object)bounds);
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new EntityHasScoreCondition((Map<String, IntRange>)this.scores.build(), this.entityTarget);
        }
    }
}

