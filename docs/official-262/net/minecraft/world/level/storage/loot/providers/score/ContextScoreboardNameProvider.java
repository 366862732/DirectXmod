/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;

public record ContextScoreboardNameProvider(LootContext.EntityTarget target) implements ScoreboardNameProvider
{
    public static final MapCodec<ContextScoreboardNameProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LootContext.EntityTarget.CODEC.fieldOf("target").forGetter(ContextScoreboardNameProvider::target)).apply((Applicative)i, ContextScoreboardNameProvider::new));
    public static final Codec<ContextScoreboardNameProvider> INLINE_CODEC = LootContext.EntityTarget.CODEC.xmap(ContextScoreboardNameProvider::new, ContextScoreboardNameProvider::target);

    public static ScoreboardNameProvider forTarget(LootContext.EntityTarget target) {
        return new ContextScoreboardNameProvider(target);
    }

    public MapCodec<ContextScoreboardNameProvider> codec() {
        return MAP_CODEC;
    }

    @Override
    public @Nullable ScoreHolder getScoreHolder(LootContext context) {
        return context.getOptionalParameter(this.target.contextParam());
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.target.contextParam());
    }
}

