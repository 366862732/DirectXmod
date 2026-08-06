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
import java.util.ArrayList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FireworkRocketRecipe
extends CustomRecipe {
    public static final MapCodec<FireworkRocketRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Ingredient.CODEC.fieldOf("shell").forGetter(o -> o.shell), (App)Ingredient.CODEC.fieldOf("fuel").forGetter(o -> o.fuel), (App)Ingredient.CODEC.fieldOf("star").forGetter(o -> o.star), (App)ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)).apply((Applicative)i, FireworkRocketRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireworkRocketRecipe> STREAM_CODEC = StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, o -> o.shell, Ingredient.CONTENTS_STREAM_CODEC, o -> o.fuel, Ingredient.CONTENTS_STREAM_CODEC, o -> o.star, ItemStackTemplate.STREAM_CODEC, o -> o.result, FireworkRocketRecipe::new);
    public static final RecipeSerializer<FireworkRocketRecipe> SERIALIZER = new RecipeSerializer<FireworkRocketRecipe>(MAP_CODEC, STREAM_CODEC);
    private final Ingredient shell;
    private final Ingredient fuel;
    private final Ingredient star;
    private final ItemStackTemplate result;

    public FireworkRocketRecipe(Ingredient shell, Ingredient fuel, Ingredient star, ItemStackTemplate result) {
        this.shell = shell;
        this.fuel = fuel;
        this.star = star;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() < 2) {
            return false;
        }
        boolean hasShell = false;
        int fuelCount = 0;
        for (int slot = 0; slot < input.size(); ++slot) {
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.shell.test(itemStack)) {
                if (hasShell) {
                    return false;
                }
                hasShell = true;
                continue;
            }
            if (!(this.fuel.test(itemStack) ? ++fuelCount > 3 : !this.star.test(itemStack))) continue;
            return false;
        }
        return hasShell && fuelCount >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ArrayList<FireworkExplosion> explosions = new ArrayList<FireworkExplosion>();
        int fuelCount = 0;
        for (int slot = 0; slot < input.size(); ++slot) {
            FireworkExplosion explosion;
            ItemStack itemStack = input.getItem(slot);
            if (itemStack.isEmpty()) continue;
            if (this.fuel.test(itemStack)) {
                ++fuelCount;
                continue;
            }
            if (!this.star.test(itemStack) || (explosion = itemStack.get(DataComponents.FIREWORK_EXPLOSION)) == null) continue;
            explosions.add(explosion);
        }
        DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.FIREWORKS, new Fireworks(fuelCount, explosions)).build();
        return this.result.apply(components);
    }

    @Override
    public RecipeSerializer<FireworkRocketRecipe> getSerializer() {
        return SERIALIZER;
    }
}

