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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class SmithingTrimRecipe
extends SimpleSmithingRecipe {
    public static final MapCodec<SmithingTrimRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo), (App)Ingredient.CODEC.fieldOf("template").forGetter(o -> o.template), (App)Ingredient.CODEC.fieldOf("base").forGetter(o -> o.base), (App)Ingredient.CODEC.fieldOf("addition").forGetter(o -> o.addition), (App)TrimPattern.CODEC.fieldOf("pattern").forGetter(o -> o.pattern)).apply((Applicative)i, SmithingTrimRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.composite(Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo, Ingredient.CONTENTS_STREAM_CODEC, o -> o.template, Ingredient.CONTENTS_STREAM_CODEC, o -> o.base, Ingredient.CONTENTS_STREAM_CODEC, o -> o.addition, TrimPattern.STREAM_CODEC, o -> o.pattern, SmithingTrimRecipe::new);
    public static final RecipeSerializer<SmithingTrimRecipe> SERIALIZER = new RecipeSerializer<SmithingTrimRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final Holder<TrimPattern> pattern;

    public SmithingTrimRecipe(Recipe.CommonInfo commonInfo, Ingredient template, Ingredient base, Ingredient addition, Holder<TrimPattern> pattern) {
        super(commonInfo);
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.pattern = pattern;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return SmithingTrimRecipe.applyTrim(input.base(), input.addition(), this.pattern);
    }

    public static ItemStack applyTrim(ItemStack baseItem, ItemStack materialItem, Holder<TrimPattern> pattern) {
        Holder<TrimMaterial> material = materialItem.get(DataComponents.PROVIDES_TRIM_MATERIAL);
        if (material != null) {
            ArmorTrim newTrim;
            ArmorTrim existingTrim = baseItem.get(DataComponents.TRIM);
            if (Objects.equals(existingTrim, newTrim = new ArmorTrim(material, pattern))) {
                return ItemStack.EMPTY;
            }
            ItemStack trimmedItem = baseItem.copyWithCount(1);
            trimmedItem.set(DataComponents.TRIM, newTrim);
            return trimmedItem;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(this.template);
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(this.addition);
    }

    @Override
    public RecipeSerializer<SmithingTrimRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(this.template, this.base, this.addition));
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay base = this.base.display();
        SlotDisplay material = this.addition.display();
        SlotDisplay template = this.template.display();
        return List.of(new SmithingRecipeDisplay(template, base, material, new SlotDisplay.SmithingTrimDemoSlotDisplay(base, material, this.pattern), new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)));
    }
}

