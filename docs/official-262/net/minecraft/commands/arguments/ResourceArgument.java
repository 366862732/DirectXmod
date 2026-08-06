/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.timeline.Timeline;

public class ResourceArgument<T>
implements ArgumentType<Holder.Reference<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_NOT_SUMMONABLE_ENTITY = new DynamicCommandExceptionType(value -> Component.translatableEscape("entity.not_summonable", value));
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_RESOURCE = new Dynamic2CommandExceptionType((id, registry) -> Component.translatableEscape("argument.resource.not_found", id, registry));
    public static final Dynamic3CommandExceptionType ERROR_INVALID_RESOURCE_TYPE = new Dynamic3CommandExceptionType((id, actualRegistry, expectedRegistry) -> Component.translatableEscape("argument.resource.invalid_type", id, actualRegistry, expectedRegistry));
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    public ResourceArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.registryLookup = context.lookupOrThrow(registryKey);
    }

    public static <T> ResourceArgument<T> resource(CommandBuildContext context, ResourceKey<? extends Registry<T>> key) {
        return new ResourceArgument<T>(context, key);
    }

    public static <T> Holder.Reference<T> getResource(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryKey) throws CommandSyntaxException {
        Holder.Reference argument = (Holder.Reference)context.getArgument(name, Holder.Reference.class);
        ResourceKey argumentKey = argument.key();
        if (argumentKey.isFor(registryKey)) {
            return argument;
        }
        throw ERROR_INVALID_RESOURCE_TYPE.create((Object)argumentKey.identifier(), (Object)argumentKey.registry(), (Object)registryKey.identifier());
    }

    public static Holder.Reference<Attribute> getAttribute(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.ATTRIBUTE);
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.CONFIGURED_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.STRUCTURE);
    }

    public static Holder.Reference<EntityType<?>> getEntityType(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.ENTITY_TYPE);
    }

    public static Holder.Reference<EntityType<?>> getSummonableEntityType(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> result = ResourceArgument.getResource(context, name, Registries.ENTITY_TYPE);
        if (!((EntityType)result.value()).canSummon()) {
            throw ERROR_NOT_SUMMONABLE_ENTITY.create((Object)result.key().identifier().toString());
        }
        return result;
    }

    public static Holder.Reference<MobEffect> getMobEffect(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.MOB_EFFECT);
    }

    public static Holder.Reference<Enchantment> getEnchantment(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.ENCHANTMENT);
    }

    public static Holder.Reference<WorldClock> getClock(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.WORLD_CLOCK);
    }

    public static Holder.Reference<Timeline> getTimeline(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceArgument.getResource(context, name, Registries.TIMELINE);
    }

    public Holder.Reference<T> parse(StringReader reader) throws CommandSyntaxException {
        Identifier resourceId = Identifier.read(reader);
        ResourceKey keyInRegistry = ResourceKey.create(this.registryKey, resourceId);
        return this.registryLookup.get(keyInRegistry).orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext((ImmutableStringReader)reader, (Object)resourceId, (Object)this.registryKey.identifier()));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T>
    implements ArgumentTypeInfo<ResourceArgument<T>, Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf out) {
            out.writeResourceKey(template.registryKey);
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf in) {
            return new Template(this, in.readRegistryKey());
        }

        @Override
        public void serializeToJson(Template template, JsonObject out) {
            out.addProperty("registry", template.registryKey.identifier().toString());
        }

        @Override
        public Template unpack(ResourceArgument<T> argument) {
            return new Template(this, argument.registryKey);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ResourceArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, ResourceKey<? extends Registry<T>> registryKey) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.registryKey = registryKey;
            }

            @Override
            public ResourceArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceArgument(context, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceArgument<T>, ?> type() {
                return this.this$0;
            }
        }
    }
}

