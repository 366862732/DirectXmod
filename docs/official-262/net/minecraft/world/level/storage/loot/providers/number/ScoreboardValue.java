/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

public record ScoreboardValue(ScoreboardNameProvider target, String score, float scale) implements NumberProvider
{
    public static final MapCodec<ScoreboardValue> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ScoreboardNameProviders.CODEC.fieldOf("target").forGetter(ScoreboardValue::target), (App)Codec.STRING.fieldOf("score").forGetter(ScoreboardValue::score), (App)Codec.FLOAT.optionalFieldOf("scale", (Object)Float.valueOf(1.0f)).forGetter(ScoreboardValue::scale)).apply((Applicative)i, ScoreboardValue::new));

    public MapCodec<ScoreboardValue> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        NumberProvider.super.validate(context);
        Validatable.validate(context, "target", this.target);
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget entityTarget, String score) {
        return ScoreboardValue.fromScoreboard(entityTarget, score, 1.0f);
    }

    public static ScoreboardValue fromScoreboard(LootContext.EntityTarget entityTarget, String score, float scale) {
        return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(entityTarget), score, scale);
    }

    @Override
    public float getFloat(LootContext context) {
        ScoreHolder scoreHolder = this.target.getScoreHolder(context);
        if (scoreHolder == null) {
            return 0.0f;
        }
        ServerScoreboard scoreboard = context.getLevel().getScoreboard();
        Objective objective = scoreboard.getObjective(this.score);
        if (objective == null) {
            return 0.0f;
        }
        ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, objective);
        if (scoreInfo == null) {
            return 0.0f;
        }
        return (float)scoreInfo.value() * this.scale;
    }
}

