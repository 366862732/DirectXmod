/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.synchronization;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class SuggestionProviders {
    private static final Map<Identifier, SuggestionProvider<SharedSuggestionProvider>> PROVIDERS_BY_NAME = new HashMap<Identifier, SuggestionProvider<SharedSuggestionProvider>>();
    private static final Identifier ID_ASK_SERVER = Identifier.withDefaultNamespace("ask_server");
    public static final SuggestionProvider<SharedSuggestionProvider> ASK_SERVER = SuggestionProviders.register(ID_ASK_SERVER, (SuggestionProvider<SharedSuggestionProvider>)((SuggestionProvider)(c, p) -> ((SharedSuggestionProvider)c.getSource()).customSuggestion(c)));
    public static final SuggestionProvider<SharedSuggestionProvider> AVAILABLE_SOUNDS = SuggestionProviders.register(Identifier.withDefaultNamespace("available_sounds"), (SuggestionProvider<SharedSuggestionProvider>)((SuggestionProvider)(c, p) -> SharedSuggestionProvider.suggestResource(((SharedSuggestionProvider)c.getSource()).getAvailableSounds(), p)));
    public static final SuggestionProvider<SharedSuggestionProvider> SUMMONABLE_ENTITIES = SuggestionProviders.register(Identifier.withDefaultNamespace("summonable_entities"), (SuggestionProvider<SharedSuggestionProvider>)((SuggestionProvider)(c, p) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.stream().filter(entityType -> entityType.isEnabled(((SharedSuggestionProvider)c.getSource()).enabledFeatures()) && entityType.canSummon()), p, EntityType::getKey, EntityType::getDescription)));

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> register(Identifier name, SuggestionProvider<SharedSuggestionProvider> provider) {
        SuggestionProvider<SharedSuggestionProvider> previous = PROVIDERS_BY_NAME.putIfAbsent(name, provider);
        if (previous != null) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name '" + String.valueOf(name) + "'");
        }
        return new RegisteredSuggestion(name, provider);
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> cast(SuggestionProvider<SharedSuggestionProvider> provider) {
        return provider;
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> getProvider(Identifier name) {
        return SuggestionProviders.cast(PROVIDERS_BY_NAME.getOrDefault(name, ASK_SERVER));
    }

    public static Identifier getName(SuggestionProvider<?> provider) {
        Identifier identifier;
        if (provider instanceof RegisteredSuggestion) {
            RegisteredSuggestion registeredProvider = (RegisteredSuggestion)provider;
            identifier = registeredProvider.name;
        } else {
            identifier = ID_ASK_SERVER;
        }
        return identifier;
    }

    private record RegisteredSuggestion(Identifier name, SuggestionProvider<SharedSuggestionProvider> delegate) implements SuggestionProvider<SharedSuggestionProvider>
    {
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<SharedSuggestionProvider> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            return this.delegate.getSuggestions(context, builder);
        }
    }
}

