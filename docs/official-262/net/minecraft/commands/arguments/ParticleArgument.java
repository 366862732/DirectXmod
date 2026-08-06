/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.arguments;

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
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class ParticleArgument
implements ArgumentType<ParticleOptions> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle{foo:bar}");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType(value -> Component.translatableEscape("particle.notFound", value));
    public static final DynamicCommandExceptionType ERROR_INVALID_OPTIONS = new DynamicCommandExceptionType(message -> Component.translatableEscape("particle.invalidOptions", message));
    private final HolderLookup.Provider registries;
    private static final TagParser<?> VALUE_PARSER = TagParser.create(NbtOps.INSTANCE);

    public ParticleArgument(CommandBuildContext context) {
        this.registries = context;
    }

    public static ParticleArgument particle(CommandBuildContext context) {
        return new ParticleArgument(context);
    }

    public static ParticleOptions getParticle(CommandContext<CommandSourceStack> context, String name) {
        return (ParticleOptions)context.getArgument(name, ParticleOptions.class);
    }

    public ParticleOptions parse(StringReader reader) throws CommandSyntaxException {
        return ParticleArgument.readParticle(reader, this.registries);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static ParticleOptions readParticle(StringReader reader, HolderLookup.Provider registries) throws CommandSyntaxException {
        ParticleType<?> type = ParticleArgument.readParticleType(reader, registries.lookupOrThrow(Registries.PARTICLE_TYPE));
        return ParticleArgument.readParticle(VALUE_PARSER, reader, type, registries);
    }

    private static ParticleType<?> readParticleType(StringReader reader, HolderLookup<ParticleType<?>> particles) throws CommandSyntaxException {
        Identifier id = Identifier.read(reader);
        ResourceKey<ParticleType<?>> key = ResourceKey.create(Registries.PARTICLE_TYPE, id);
        return particles.get(key).orElseThrow(() -> ERROR_UNKNOWN_PARTICLE.createWithContext((ImmutableStringReader)reader, (Object)id)).value();
    }

    private static <T extends ParticleOptions, O> T readParticle(TagParser<O> parser, StringReader reader, ParticleType<T> type, HolderLookup.Provider registries) throws CommandSyntaxException {
        RegistryOps<O> ops = registries.createSerializationContext(parser.getOps());
        Object extraData = reader.canRead() && reader.peek() == '{' ? parser.parseAsArgument(reader) : ops.emptyMap();
        return (T)((ParticleOptions)type.codec().codec().parse(ops, extraData).getOrThrow(arg_0 -> ((DynamicCommandExceptionType)ERROR_INVALID_OPTIONS).create(arg_0)));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        HolderGetter particles = this.registries.lookupOrThrow(Registries.PARTICLE_TYPE);
        return SharedSuggestionProvider.suggestResource(particles.listElementIds().map(ResourceKey::identifier), builder);
    }
}

