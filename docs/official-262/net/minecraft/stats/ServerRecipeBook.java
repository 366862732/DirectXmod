/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.slf4j.Logger;

public class ServerRecipeBook
extends RecipeBook {
    public static final String RECIPE_BOOK_TAG = "recipeBook";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DisplayResolver displayResolver;
    @VisibleForTesting
    protected final Set<ResourceKey<Recipe<?>>> known = Sets.newIdentityHashSet();
    @VisibleForTesting
    protected final Set<ResourceKey<Recipe<?>>> highlight = Sets.newIdentityHashSet();

    public ServerRecipeBook(DisplayResolver displayResolver) {
        this.displayResolver = displayResolver;
    }

    public void add(ResourceKey<Recipe<?>> id) {
        this.known.add(id);
    }

    public boolean contains(ResourceKey<Recipe<?>> id) {
        return this.known.contains(id);
    }

    public void remove(ResourceKey<Recipe<?>> id) {
        this.known.remove(id);
        this.highlight.remove(id);
    }

    public void removeHighlight(ResourceKey<Recipe<?>> id) {
        this.highlight.remove(id);
    }

    private void addHighlight(ResourceKey<Recipe<?>> id) {
        this.highlight.add(id);
    }

    public int addRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player) {
        ArrayList<ClientboundRecipeBookAddPacket.Entry> recipesToAdd = new ArrayList<ClientboundRecipeBookAddPacket.Entry>();
        for (RecipeHolder<?> recipe : recipes) {
            ResourceKey<Recipe<?>> id = recipe.id();
            if (this.known.contains(id) || recipe.value().isSpecial()) continue;
            this.add(id);
            this.addHighlight(id);
            this.displayResolver.displaysForRecipe(id, display -> recipesToAdd.add(new ClientboundRecipeBookAddPacket.Entry((RecipeDisplayEntry)display, recipe.value().showNotification(), true)));
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipe);
        }
        if (!recipesToAdd.isEmpty()) {
            player.connection.send(new ClientboundRecipeBookAddPacket(recipesToAdd, false));
        }
        return recipesToAdd.size();
    }

    public int removeRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player) {
        ArrayList recipesToRemove = Lists.newArrayList();
        for (RecipeHolder<?> recipe : recipes) {
            ResourceKey<Recipe<?>> id = recipe.id();
            if (!this.known.contains(id)) continue;
            this.remove(id);
            this.displayResolver.displaysForRecipe(id, display -> recipesToRemove.add(display.id()));
        }
        if (!recipesToRemove.isEmpty()) {
            player.connection.send(new ClientboundRecipeBookRemovePacket(recipesToRemove));
        }
        return recipesToRemove.size();
    }

    private void loadRecipes(List<ResourceKey<Recipe<?>>> recipes, Consumer<ResourceKey<Recipe<?>>> recipeAddingMethod, Predicate<ResourceKey<Recipe<?>>> validator) {
        for (ResourceKey<Recipe<?>> recipe : recipes) {
            if (!validator.test(recipe)) {
                LOGGER.error("Tried to load unrecognized recipe: {} removed now.", recipe);
                continue;
            }
            recipeAddingMethod.accept(recipe);
        }
    }

    public void sendInitialRecipeBook(ServerPlayer player) {
        player.connection.send(new ClientboundRecipeBookSettingsPacket(this.getBookSettings().copy()));
        ArrayList<ClientboundRecipeBookAddPacket.Entry> recipesToSend = new ArrayList<ClientboundRecipeBookAddPacket.Entry>(this.known.size());
        for (ResourceKey<Recipe<?>> id : this.known) {
            this.displayResolver.displaysForRecipe(id, r -> recipesToSend.add(new ClientboundRecipeBookAddPacket.Entry((RecipeDisplayEntry)r, false, this.highlight.contains(id))));
        }
        player.connection.send(new ClientboundRecipeBookAddPacket(recipesToSend, true));
    }

    public void copyOverData(ServerRecipeBook bookToCopy) {
        this.apply(bookToCopy.pack());
    }

    public Packed pack() {
        return new Packed(this.bookSettings.copy(), List.copyOf(this.known), List.copyOf(this.highlight));
    }

    private void apply(Packed packed) {
        this.known.clear();
        this.highlight.clear();
        this.bookSettings.replaceFrom(packed.settings);
        this.known.addAll(packed.known);
        this.highlight.addAll(packed.highlight);
    }

    public void loadUntrusted(Packed packed, Predicate<ResourceKey<Recipe<?>>> validator) {
        this.bookSettings.replaceFrom(packed.settings);
        this.loadRecipes(packed.known, this.known::add, validator);
        this.loadRecipes(packed.highlight, this.highlight::add, validator);
    }

    @FunctionalInterface
    public static interface DisplayResolver {
        public void displaysForRecipe(ResourceKey<Recipe<?>> var1, Consumer<RecipeDisplayEntry> var2);
    }

    public record Packed(RecipeBookSettings settings, List<ResourceKey<Recipe<?>>> known, List<ResourceKey<Recipe<?>>> highlight) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)RecipeBookSettings.MAP_CODEC.forGetter(Packed::settings), (App)Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(Packed::known), (App)Recipe.KEY_CODEC.listOf().fieldOf("toBeDisplayed").forGetter(Packed::highlight)).apply((Applicative)i, Packed::new));
    }
}

