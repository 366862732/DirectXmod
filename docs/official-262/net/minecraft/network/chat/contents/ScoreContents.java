/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.util.CompilableString;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

public record ScoreContents(Either<CompilableString<EntitySelector>, String> name, String objective) implements ComponentContents
{
    public static final MapCodec<ScoreContents> INNER_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.either(EntitySelector.COMPILABLE_CODEC, (Codec)Codec.STRING).fieldOf("name").forGetter(ScoreContents::name), (App)Codec.STRING.fieldOf("objective").forGetter(ScoreContents::objective)).apply((Applicative)i, ScoreContents::new));
    public static final MapCodec<ScoreContents> MAP_CODEC = INNER_CODEC.fieldOf("score");

    public MapCodec<ScoreContents> codec() {
        return MAP_CODEC;
    }

    private ScoreHolder findTargetName(CommandSourceStack source) throws CommandSyntaxException {
        Optional selector = this.name.left();
        if (selector.isPresent()) {
            List<? extends Entity> entities = ((EntitySelector)((CompilableString)selector.get()).compiled()).findEntities(source);
            if (!entities.isEmpty()) {
                if (entities.size() != 1) {
                    throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
                }
                return entities.getFirst();
            }
            return ScoreHolder.forNameOnly(((CompilableString)selector.get()).source());
        }
        return ScoreHolder.forNameOnly((String)this.name.right().orElseThrow());
    }

    private MutableComponent getScore(ScoreHolder name, CommandSourceStack source) {
        ReadOnlyScoreInfo scoreInfo;
        ServerScoreboard scoreboard;
        Objective objective;
        MinecraftServer server = source.getServer();
        if (server != null && (objective = (scoreboard = server.getScoreboard()).getObjective(this.objective)) != null && (scoreInfo = scoreboard.getPlayerScoreInfo(name, objective)) != null) {
            return scoreInfo.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
        }
        return Component.empty();
    }

    @Override
    public MutableComponent resolve(ResolutionContext context, int recursionDepth) throws CommandSyntaxException {
        CommandSourceStack source = context.source();
        if (source == null) {
            return Component.empty();
        }
        ScoreHolder scoreHolder = this.findTargetName(source);
        Entity entity = context.defaultScoreboardEntity();
        ScoreHolder scoreName = entity != null && scoreHolder.equals(ScoreHolder.WILDCARD) ? entity : scoreHolder;
        return this.getScore(scoreName, source);
    }

    @Override
    public String toString() {
        return "score{name='" + String.valueOf(this.name) + "', objective='" + this.objective + "'}";
    }
}

