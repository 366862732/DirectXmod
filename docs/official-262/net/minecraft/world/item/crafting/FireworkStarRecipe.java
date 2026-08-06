/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Keyable
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.crafting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class FireworkStarRecipe
extends CustomRecipe {
    public static final MapCodec<FireworkStarRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.simpleMap(FireworkExplosion.Shape.CODEC, Ingredient.CODEC, (Keyable)StringRepresentable.keys(FireworkExplosion.Shape.values())).fieldOf("shapes").forGetter(o -> o.shapes), (App)Ingredient.CODEC.fieldOf("trail").forGetter(o -> o.trail), (App)Ingredient.CODEC.fieldOf("twinkle").forGetter(o -> o.twinkle), (App)Ingredient.CODEC.fieldOf("fuel").forGetter(o -> o.fuel), (App)Ingredient.CODEC.fieldOf("dye").forGetter(o -> o.dye), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, FireworkStarRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireworkStarRecipe> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.map(HashMap::new, FireworkExplosion.Shape.STREAM_CODEC, Ingredient.CONTENTS_STREAM_CODEC), o -> o.shapes, Ingredient.CONTENTS_STREAM_CODEC, o -> o.trail, Ingredient.CONTENTS_STREAM_CODEC, o -> o.twinkle, Ingredient.CONTENTS_STREAM_CODEC, o -> o.fuel, Ingredient.CONTENTS_STREAM_CODEC, o -> o.dye, ItemStackTemplate.STREAM_CODEC, o -> o.result, FireworkStarRecipe::new);
    public static final RecipeSerializer<FireworkStarRecipe> SERIALIZER = new RecipeSerializer<FireworkStarRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Map<FireworkExplosion.Shape, Ingredient> shapes;
    private final Ingredient trail;
    private final Ingredient twinkle;
    private final Ingredient fuel;
    private final Ingredient dye;
    private final ItemStackTemplate result;

    public FireworkStarRecipe(Map<FireworkExplosion.Shape, Ingredient> shapes, Ingredient trail, Ingredient twinkle, Ingredient fuel, Ingredient dye, ItemStackTemplate result) {
        this.shapes = shapes;
        this.trail = trail;
        this.twinkle = twinkle;
        this.fuel = fuel;
        this.dye = dye;
        this.result = result;
    }

    private @Nullable FireworkExplosion.Shape findShape(ItemStack itemStack) {
        for (Map.Entry<FireworkExplosion.Shape, Ingredient> e : this.shapes.entrySet()) {
            if (!e.getValue().test(itemStack)) continue;
            return e.getKey();
        }
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() < 2) {
            return false;
        }
        boolean hasFuel = false;
        boolean hasDye = false;
        boolean hasShape = false;
        boolean hasTrail = false;
        boolean hasTwinkle = false;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.twinkle.test(itemStack)) {
                if (hasTwinkle) {
                    return false;
                }
                hasTwinkle = true;
                continue;
            }
            if (this.trail.test(itemStack)) {
                if (hasTrail) {
                    return false;
                }
                hasTrail = true;
                continue;
            }
            if (this.fuel.test(itemStack)) {
                if (hasFuel) {
                    return false;
                }
                hasFuel = true;
                continue;
            }
            if (this.dye.test(itemStack) && itemStack.has(DataComponents.DYE)) {
                hasDye = true;
                continue;
            }
            FireworkExplosion.Shape shape = this.findShape(itemStack);
            if (shape != null) {
                if (hasShape) {
                    return false;
                }
                hasShape = true;
                continue;
            }
            return false;
        }
        return hasFuel && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
        boolean hasTwinkle = false;
        boolean hasTrail = false;
        IntArrayList colors = new IntArrayList();
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            FireworkExplosion.Shape maybeShape = this.findShape(itemStack);
            if (maybeShape != null) {
                shape = maybeShape;
                continue;
            }
            if (this.twinkle.test(itemStack)) {
                hasTwinkle = true;
                continue;
            }
            if (this.trail.test(itemStack)) {
                hasTrail = true;
                continue;
            }
            if (!this.dye.test(itemStack)) continue;
            DyeColor dye = itemStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
            colors.add(dye.getFireworkColor());
        }
        ItemStack star = this.result.create();
        star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(shape, (IntList)colors, IntList.of(), hasTrail, hasTwinkle));
        return star;
    }

    @Override
    public RecipeSerializer<FireworkStarRecipe> getSerializer() {
        return SERIALIZER;
    }
}

