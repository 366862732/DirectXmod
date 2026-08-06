/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashBasedTable
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

@Environment(value=EnvType.CLIENT)
public class ClientRecipeBook
extends RecipeBook {
    private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<RecipeDisplayId, RecipeDisplayEntry>();
    private final Set<RecipeDisplayId> highlight = new HashSet<RecipeDisplayId>();
    private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
    private List<RecipeCollection> allCollections = List.of();

    public void add(RecipeDisplayEntry display) {
        this.known.put(display.id(), display);
    }

    public void remove(RecipeDisplayId id) {
        this.known.remove(id);
        this.highlight.remove(id);
    }

    public void clear() {
        this.known.clear();
        this.highlight.clear();
    }

    public boolean willHighlight(RecipeDisplayId recipe) {
        return this.highlight.contains(recipe);
    }

    public void removeHighlight(RecipeDisplayId id) {
        this.highlight.remove(id);
    }

    public void addHighlight(RecipeDisplayId id) {
        this.highlight.add(id);
    }

    public void rebuildCollections() {
        Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> recipeListsByCategory = ClientRecipeBook.categorizeAndGroupRecipes(this.known.values());
        HashMap<SearchRecipeBookCategory, List> byCategory = new HashMap<SearchRecipeBookCategory, List>();
        ImmutableList.Builder all = ImmutableList.builder();
        recipeListsByCategory.forEach((category, categoryRecipes) -> byCategory.put((SearchRecipeBookCategory)category, (List)categoryRecipes.stream().map(RecipeCollection::new).peek(arg_0 -> ((ImmutableList.Builder)all).add(arg_0)).collect(ImmutableList.toImmutableList())));
        for (SearchRecipeBookCategory searchCategory : SearchRecipeBookCategory.values()) {
            byCategory.put(searchCategory, (List)searchCategory.includedCategories().stream().flatMap(subCategory -> byCategory.getOrDefault(subCategory, List.of()).stream()).collect(ImmutableList.toImmutableList()));
        }
        this.collectionsByTab = Map.copyOf(byCategory);
        this.allCollections = all.build();
    }

    private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(Iterable<RecipeDisplayEntry> recipes) {
        HashMap<RecipeBookCategory, List<List<RecipeDisplayEntry>>> result = new HashMap<RecipeBookCategory, List<List<RecipeDisplayEntry>>>();
        HashBasedTable multiItemGroups = HashBasedTable.create();
        for (RecipeDisplayEntry entry : recipes) {
            RecipeBookCategory category = entry.category();
            OptionalInt groupId = entry.group();
            if (groupId.isEmpty()) {
                result.computeIfAbsent(category, key -> new ArrayList()).add(List.of(entry));
                continue;
            }
            ArrayList<RecipeDisplayEntry> groupRecipes = (ArrayList<RecipeDisplayEntry>)multiItemGroups.get((Object)category, (Object)groupId.getAsInt());
            if (groupRecipes == null) {
                groupRecipes = new ArrayList<RecipeDisplayEntry>();
                multiItemGroups.put((Object)category, (Object)groupId.getAsInt(), groupRecipes);
                result.computeIfAbsent(category, key -> new ArrayList()).add(groupRecipes);
            }
            groupRecipes.add(entry);
        }
        return result;
    }

    public List<RecipeCollection> getCollections() {
        return this.allCollections;
    }

    public List<RecipeCollection> getCollection(ExtendedRecipeBookCategory category) {
        return this.collectionsByTab.getOrDefault(category, Collections.emptyList());
    }
}

