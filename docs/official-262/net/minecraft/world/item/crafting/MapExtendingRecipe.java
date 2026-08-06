/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe
extends CustomRecipe {
    public static final MapCodec<MapExtendingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Ingredient.CODEC.fieldOf("map").forGetter(o -> o.map), (App)Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, MapExtendingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapExtendingRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, o -> o.map, Ingredient.CONTENTS_STREAM_CODEC, o -> o.material, ItemStackTemplate.STREAM_CODEC, o -> o.result, MapExtendingRecipe::new);
    public static final RecipeSerializer<MapExtendingRecipe> SERIALIZER = new RecipeSerializer<MapExtendingRecipe>(MAP_CODEC, STREAM_CODEC);
    private final ShapedRecipePattern pattern;
    private final Ingredient map;
    private final Ingredient material;
    private final ItemStackTemplate result;

    public MapExtendingRecipe(Ingredient map, Ingredient material, ItemStackTemplate result) {
        this.map = map;
        this.material = material;
        this.result = result;
        this.pattern = ShapedRecipePattern.of(Map.of(Character.valueOf('#'), material, Character.valueOf('x'), map), "###", "#x#", "###");
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!this.pattern.matches(input)) {
            return false;
        }
        ItemStack map = MapExtendingRecipe.findFilledMap(input);
        if (map.isEmpty()) {
            return false;
        }
        MapItemSavedData data = MapItem.getSavedData(map, level);
        if (data == null) {
            return false;
        }
        if (data.isExplorationMap()) {
            return false;
        }
        return data.scale < 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack sourceMap = MapExtendingRecipe.findFilledMap(input);
        ItemStack map = TransmuteRecipe.createWithOriginalComponents(this.result, sourceMap);
        map.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
        return map;
    }

    private static ItemStack findFilledMap(CraftingInput input) {
        for (int i = 0; i < input.size(); ++i) {
            ItemStack itemStack = input.getItem(i);
            if (!itemStack.has(DataComponents.MAP_ID)) continue;
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<MapExtendingRecipe> getSerializer() {
        return SERIALIZER;
    }
}

