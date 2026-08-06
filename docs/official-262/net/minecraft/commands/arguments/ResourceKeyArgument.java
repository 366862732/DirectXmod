/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 */
package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T>
implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.place.feature.invalid", value));
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.place.structure.invalid", value));
    private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.place.jigsaw.invalid", value));
    private static final DynamicCommandExceptionType ERROR_INVALID_RECIPE = new DynamicCommandExceptionType(value -> Component.translatableEscape("recipe.notFound", value));
    private static final DynamicCommandExceptionType ERROR_INVALID_ADVANCEMENT = new DynamicCommandExceptionType(value -> Component.translatableEscape("advancement.advancementNotFound", value));
    private final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> key) {
        return new ResourceKeyArgument<T>(key);
    }

    public static <T> ResourceKey<T> getRegistryKey(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryKey, DynamicCommandExceptionType exceptionType) throws CommandSyntaxException {
        ResourceKey argument = (ResourceKey)context.getArgument(name, ResourceKey.class);
        Optional<ResourceKey<T>> value = argument.cast(registryKey);
        return value.orElseThrow(() -> exceptionType.create((Object)argument.identifier()));
    }

    private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> context, ResourceKey<? extends Registry<T>> registryKey) {
        return ((CommandSourceStack)context.getSource()).getServer().registryAccess().lookupOrThrow(registryKey);
    }

    private static <T> Holder.Reference<T> resolveKey(CommandContext<CommandSourceStack> context, String name, ResourceKey<Registry<T>> registryKey, DynamicCommandExceptionType exception) throws CommandSyntaxException {
        ResourceKey key = ResourceKeyArgument.getRegistryKey(context, name, registryKey, exception);
        return (Holder.Reference)ResourceKeyArgument.getRegistry(context, registryKey).get(key).orElseThrow(() -> exception.create((Object)key.identifier()));
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceKeyArgument.resolveKey(context, name, Registries.CONFIGURED_FEATURE, ERROR_INVALID_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceKeyArgument.resolveKey(context, name, Registries.STRUCTURE, ERROR_INVALID_STRUCTURE);
    }

    public static Holder.Reference<StructureTemplatePool> getStructureTemplatePool(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceKeyArgument.resolveKey(context, name, Registries.TEMPLATE_POOL, ERROR_INVALID_TEMPLATE_POOL);
    }

    public static RecipeHolder<?> getRecipe(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        RecipeManager recipeManager = ((CommandSourceStack)context.getSource()).getServer().getRecipeManager();
        ResourceKey<Recipe<?>> key = ResourceKeyArgument.getRegistryKey(context, name, Registries.RECIPE, ERROR_INVALID_RECIPE);
        return recipeManager.byKey(key).orElseThrow(() -> ERROR_INVALID_RECIPE.create((Object)key.identifier()));
    }

    public static AdvancementHolder getAdvancement(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceKey key = ResourceKeyArgument.getRegistryKey(context, name, Registries.ADVANCEMENT, ERROR_INVALID_ADVANCEMENT);
        AdvancementHolder advancement = ((CommandSourceStack)context.getSource()).getServer().getAdvancements().get(key.identifier());
        if (advancement == null) {
            throw ERROR_INVALID_ADVANCEMENT.create((Object)key.identifier());
        }
        return advancement;
    }

    public ResourceKey<T> parse(StringReader reader) throws CommandSyntaxException {
        Identifier resourceId = Identifier.read(reader);
        return ResourceKey.create(this.registryKey, resourceId);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T>
    implements ArgumentTypeInfo<ResourceKeyArgument<T>, Template> {
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
        public Template unpack(ResourceKeyArgument<T> argument) {
            return new Template(this, argument.registryKey);
        }

        public final class Template
        implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;
            final /* synthetic */ Info this$0;

            private Template(Info this$0, ResourceKey<? extends Registry<T>> registryKey) {
                Info info = this$0;
                Objects.requireNonNull(info);
                this.this$0 = info;
                this.registryKey = registryKey;
            }

            @Override
            public ResourceKeyArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceKeyArgument(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
                return this.this$0;
            }
        }
    }
}

