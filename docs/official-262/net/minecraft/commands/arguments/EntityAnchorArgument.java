/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityAnchorArgument
implements ArgumentType<Anchor> {
    private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
    private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(name -> Component.translatableEscape("argument.anchor.invalid", name));

    public static Anchor getAnchor(CommandContext<CommandSourceStack> context, String name) {
        return (Anchor)((Object)context.getArgument(name, Anchor.class));
    }

    public static EntityAnchorArgument anchor() {
        return new EntityAnchorArgument();
    }

    public Anchor parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String name = reader.readUnquotedString();
        Anchor anchor = Anchor.getByName(name);
        if (anchor == null) {
            reader.setCursor(start);
            throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader, (Object)name);
        }
        return anchor;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Anchor.BY_NAME.keySet(), builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static enum Anchor {
        FEET("feet", (p, e) -> p),
        EYES("eyes", (p, e) -> new Vec3(p.x, p.y + (double)e.getEyeHeight(), p.z));

        private static final Map<String, Anchor> BY_NAME;
        private final String name;
        private final BiFunction<Vec3, Entity, Vec3> transform;

        private Anchor(String name, BiFunction<Vec3, Entity, Vec3> transform) {
            this.name = name;
            this.transform = transform;
        }

        public static @Nullable Anchor getByName(String name) {
            return BY_NAME.get(name);
        }

        public Vec3 apply(Entity entity) {
            return this.transform.apply(entity.position(), entity);
        }

        public Vec3 apply(CommandSourceStack source) {
            Entity entity = source.getEntity();
            if (entity == null) {
                return source.getPosition();
            }
            return this.transform.apply(source.getPosition(), entity);
        }

        static {
            BY_NAME = Util.make(Maps.newHashMap(), map -> {
                for (Anchor anchor : Anchor.values()) {
                    map.put(anchor.name, anchor);
                }
            });
        }
    }
}

