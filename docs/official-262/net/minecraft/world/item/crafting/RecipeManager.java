/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.JsonOps
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RecipeManager
extends SimplePreparableReloadListener<RecipeMap>
implements RecipeAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<RecipePropertySet>, IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(RecipePropertySet.SMITHING_ADDITION, recipe -> {
        Optional<Object> optional;
        if (recipe instanceof SmithingRecipe) {
            SmithingRecipe smithingRecipe = (SmithingRecipe)recipe;
            optional = smithingRecipe.additionIngredient();
        } else {
            optional = Optional.empty();
        }
        return optional;
    }, RecipePropertySet.SMITHING_BASE, recipe -> {
        Optional<Object> optional;
        if (recipe instanceof SmithingRecipe) {
            SmithingRecipe smithingRecipe = (SmithingRecipe)recipe;
            optional = Optional.of(smithingRecipe.baseIngredient());
        } else {
            optional = Optional.empty();
        }
        return optional;
    }, RecipePropertySet.SMITHING_TEMPLATE, recipe -> {
        Optional<Object> optional;
        if (recipe instanceof SmithingRecipe) {
            SmithingRecipe smithingRecipe = (SmithingRecipe)recipe;
            optional = smithingRecipe.templateIngredient();
        } else {
            optional = Optional.empty();
        }
        return optional;
    }, RecipePropertySet.FURNACE_INPUT, RecipeManager.forSingleInput(RecipeType.SMELTING), RecipePropertySet.BLAST_FURNACE_INPUT, RecipeManager.forSingleInput(RecipeType.BLASTING), RecipePropertySet.SMOKER_INPUT, RecipeManager.forSingleInput(RecipeType.SMOKING), RecipePropertySet.CAMPFIRE_INPUT, RecipeManager.forSingleInput(RecipeType.CAMPFIRE_COOKING));
    private static final FileToIdConverter RECIPE_LISTER = FileToIdConverter.registry(Registries.RECIPE);
    private final HolderLookup.Provider registries;
    private RecipeMap recipes = RecipeMap.EMPTY;
    private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
    private List<ServerDisplayInfo> allDisplays = List.of();
    private Map<ResourceKey<Recipe<?>>, List<ServerDisplayInfo>> recipeToDisplay = Map.of();

    public RecipeManager(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    protected RecipeMap prepare(ResourceManager manager, ProfilerFiller profiler) {
        TreeMap<Identifier, Recipe> recipes = new TreeMap<Identifier, Recipe>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, RECIPE_LISTER, this.registries.createSerializationContext(JsonOps.INSTANCE), Recipe.CODEC, recipes);
        ArrayList recipeHolders = new ArrayList(recipes.size());
        recipes.forEach((id, recipe) -> {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
            RecipeHolder<Recipe> holder = new RecipeHolder<Recipe>(key, (Recipe)recipe);
            recipeHolders.add(holder);
        });
        return RecipeMap.create(recipeHolders);
    }

    @Override
    protected void apply(RecipeMap recipes, ResourceManager manager, ProfilerFiller profiler) {
        this.recipes = recipes;
        LOGGER.info("Loaded {} recipes", (Object)recipes.values().size());
    }

    public void finalizeRecipeLoading(FeatureFlagSet enabledFlags) {
        ArrayList stonecutterRecipes = new ArrayList();
        List<IngredientCollector> propertySetCollectors = RECIPE_PROPERTY_SETS.entrySet().stream().map(e -> new IngredientCollector((ResourceKey)e.getKey(), (IngredientExtractor)e.getValue())).toList();
        this.recipes.values().forEach(recipeHolder -> {
            Object recipe = recipeHolder.value();
            if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", (Object)recipeHolder.id().identifier());
                return;
            }
            propertySetCollectors.forEach(c -> c.accept((Recipe<?>)recipe));
            if (recipe instanceof StonecutterRecipe) {
                StonecutterRecipe stonecutterRecipe = (StonecutterRecipe)recipe;
                RecipeHolder castHolder = recipeHolder;
                if (RecipeManager.isIngredientEnabled(enabledFlags, stonecutterRecipe.input()) && stonecutterRecipe.resultDisplay().isEnabled(enabledFlags)) {
                    stonecutterRecipes.add(new SelectableRecipe.SingleInputEntry(stonecutterRecipe.input(), new SelectableRecipe(stonecutterRecipe.resultDisplay(), Optional.of(castHolder))));
                }
            }
        });
        this.propertySets = propertySetCollectors.stream().collect(Collectors.toUnmodifiableMap(c -> c.key, c -> c.asPropertySet(enabledFlags)));
        this.stonecutterRecipes = new SelectableRecipe.SingleInputSet(stonecutterRecipes);
        this.allDisplays = RecipeManager.unpackRecipeInfo(this.recipes.values(), enabledFlags);
        this.recipeToDisplay = this.allDisplays.stream().collect(Collectors.groupingBy(r -> r.parent.id(), IdentityHashMap::new, Collectors.toList()));
    }

    private static List<Ingredient> filterDisabled(FeatureFlagSet enabledFlags, List<Ingredient> ingredients) {
        ingredients.removeIf(e -> !RecipeManager.isIngredientEnabled(enabledFlags, e));
        return ingredients;
    }

    private static boolean isIngredientEnabled(FeatureFlagSet enabledFlags, Ingredient ingredient) {
        return ingredient.items().allMatch(i -> ((Item)i.value()).isEnabled(enabledFlags));
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, @Nullable ResourceKey<Recipe<?>> recipeHint) {
        RecipeHolder<T> hintedRecipe = recipeHint != null ? this.byKeyTyped(type, recipeHint) : null;
        return this.getRecipeFor(type, input, level, hintedRecipe);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level, @Nullable RecipeHolder<T> recipeHint) {
        if (recipeHint != null && recipeHint.value().matches(input, level)) {
            return Optional.of(recipeHint);
        }
        return this.getRecipeFor(type, input, level);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level) {
        return this.recipes.getRecipesFor(type, input, level).findFirst();
    }

    public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> recipeId) {
        return Optional.ofNullable(this.recipes.byKey(recipeId));
    }

    private <T extends Recipe<?>> @Nullable RecipeHolder<T> byKeyTyped(RecipeType<T> type, ResourceKey<Recipe<?>> recipeId) {
        RecipeHolder<?> recipe = this.recipes.byKey(recipeId);
        if (recipe != null && recipe.value().getType().equals(type)) {
            return recipe;
        }
        return null;
    }

    public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
        return this.propertySets;
    }

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    @Override
    public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> id) {
        return this.propertySets.getOrDefault(id, RecipePropertySet.EMPTY);
    }

    @Override
    public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return this.recipes.values();
    }

    public @Nullable ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId id) {
        int index = id.index();
        return index >= 0 && index < this.allDisplays.size() ? this.allDisplays.get(index) : null;
    }

    public void listDisplaysForRecipe(ResourceKey<Recipe<?>> id, Consumer<RecipeDisplayEntry> output) {
        List<ServerDisplayInfo> recipes = this.recipeToDisplay.get(id);
        if (recipes != null) {
            recipes.forEach(e -> output.accept(e.display));
        }
    }

    @VisibleForTesting
    protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> id, JsonObject object, HolderLookup.Provider registries) {
        Recipe recipe = (Recipe)Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), (Object)object).getOrThrow(JsonParseException::new);
        return new RecipeHolder<Recipe>(id, recipe);
    }

    public static <I extends RecipeInput, T extends Recipe<I>> CachedCheck<I, T> createCheck(final RecipeType<T> type) {
        return new CachedCheck<I, T>(){
            private @Nullable ResourceKey<Recipe<?>> lastRecipe;

            @Override
            public Optional<RecipeHolder<T>> getRecipeFor(I input, ServerLevel level) {
                RecipeManager recipeManager = level.recipeAccess();
                Optional result = recipeManager.getRecipeFor(type, input, (Level)level, this.lastRecipe);
                if (result.isPresent()) {
                    RecipeHolder unpackedResult = result.get();
                    this.lastRecipe = unpackedResult.id();
                    return Optional.of(unpackedResult);
                }
                return Optional.empty();
            }
        };
    }

    private static List<ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> recipes, FeatureFlagSet enabledFeatures) {
        ArrayList<ServerDisplayInfo> result = new ArrayList<ServerDisplayInfo>();
        Object2IntOpenHashMap recipeGroups = new Object2IntOpenHashMap();
        for (RecipeHolder<?> recipeHolder : recipes) {
            Object recipe = recipeHolder.value();
            OptionalInt groupId = recipe.group().isEmpty() ? OptionalInt.empty() : OptionalInt.of(recipeGroups.computeIfAbsent((Object)recipe.group(), arg_0 -> RecipeManager.lambda$unpackRecipeInfo$0((Object2IntMap)recipeGroups, arg_0)));
            Optional<Object> placementCheck = recipe.isSpecial() ? Optional.empty() : Optional.of(recipe.placementInfo().ingredients());
            for (RecipeDisplay recipeDisplay : recipe.display()) {
                if (!recipeDisplay.isEnabled(enabledFeatures)) continue;
                int nextDisplayId = result.size();
                RecipeDisplayId id = new RecipeDisplayId(nextDisplayId);
                RecipeDisplayEntry entry = new RecipeDisplayEntry(id, recipeDisplay, groupId, recipe.recipeBookCategory(), placementCheck);
                result.add(new ServerDisplayInfo(entry, recipeHolder));
            }
        }
        return result;
    }

    private static IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> type) {
        return recipe -> {
            Optional<Object> optional;
            if (recipe.getType() == type && recipe instanceof SingleItemRecipe) {
                SingleItemRecipe singleItemRecipe = (SingleItemRecipe)recipe;
                optional = Optional.of(singleItemRecipe.input());
            } else {
                optional = Optional.empty();
            }
            return optional;
        };
    }

    private static /* synthetic */ int lambda$unpackRecipeInfo$0(Object2IntMap recipeGroups, Object id) {
        return recipeGroups.size();
    }

    public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
    }

    @FunctionalInterface
    public static interface IngredientExtractor {
        public Optional<Ingredient> apply(Recipe<?> var1);
    }

    public static class IngredientCollector
    implements Consumer<Recipe<?>> {
        private final ResourceKey<RecipePropertySet> key;
        private final IngredientExtractor extractor;
        private final List<Ingredient> ingredients = new ArrayList<Ingredient>();

        protected IngredientCollector(ResourceKey<RecipePropertySet> key, IngredientExtractor extractor) {
            this.key = key;
            this.extractor = extractor;
        }

        @Override
        public void accept(Recipe<?> recipe) {
            this.extractor.apply(recipe).ifPresent(this.ingredients::add);
        }

        public RecipePropertySet asPropertySet(FeatureFlagSet enabledFeatures) {
            return RecipePropertySet.create(RecipeManager.filterDisabled(enabledFeatures, this.ingredients));
        }
    }

    public static interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
        public Optional<RecipeHolder<T>> getRecipeFor(I var1, ServerLevel var2);
    }
}

