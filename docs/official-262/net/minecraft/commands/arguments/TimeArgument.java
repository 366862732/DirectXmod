/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class TimeArgument
implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType((Message)Component.translatable("argument.time.invalid_unit"));
    private static final Dynamic2CommandExceptionType ERROR_TICK_COUNT_TOO_LOW = new Dynamic2CommandExceptionType((value, limit) -> Component.translatableEscape("argument.time.tick_count_too_low", limit, value));
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap();
    private final int minimum;

    private TimeArgument(int minimum) {
        this.minimum = minimum;
    }

    public static TimeArgument time() {
        return new TimeArgument(0);
    }

    public static TimeArgument time(int minimum) {
        return new TimeArgument(minimum);
    }

    public Integer parse(StringReader reader) throws CommandSyntaxException {
        float value = reader.readFloat();
        String unit = reader.readUnquotedString();
        int factor = UNITS.getOrDefault((Object)unit, 0);
        if (factor == 0) {
            throw ERROR_INVALID_UNIT.createWithContext((ImmutableStringReader)reader);
        }
        int ticks = Math.round(value * (float)factor);
        if (ticks < this.minimum) {
            throw ERROR_TICK_COUNT_TOO_LOW.createWithContext((ImmutableStringReader)reader, (Object)ticks, (Object)this.minimum);
        }
        return ticks;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getRemaining());
        try {
            reader.readFloat();
        }
        catch (CommandSyntaxException ignored) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest((Iterable<String>)UNITS.keySet(), builder.createOffset(builder.getStart() + reader.getCursor()));
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    static {
        UNITS.put((Object)"d", 24000);
        UNITS.put((Object)"s", 20);
        UNITS.put((Object)"t", 1);
        UNITS.put((Object)"", 1);
    }

    public static class Info
    implements ArgumentTypeInfo<TimeArgument, Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf out) {
            out.writeInt(template.min);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf in) {
            int min = in.readInt();
            return new Template(this, min);
        }

        @Override
        public void serializeToJson(Template template, JsonObject out) {
            out.addProperty("min", (Number)template.min);
        }

        @Override
        public Template unpack(TimeArgument argument) {
            return new Template(this, argument.minimum);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<TimeArgument> {
            private final int min;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, int min) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.min = min;
            }

            @Override
            public TimeArgument instantiate(CommandBuildContext context) {
                return TimeArgument.time(this.min);
            }

            @Override
            public ArgumentTypeInfo<TimeArgument, ?> type() {
                return this.this$0;
            }
        }
    }
}

