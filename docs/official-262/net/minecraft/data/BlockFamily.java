/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class BlockFamily {
    private final Block baseBlock;
    private final Map<Variant, Block> variants = Maps.newHashMap();
    private boolean generateModel = true;
    private boolean generateCraftingRecipe = true;
    private boolean generateSmeltingRecipe = true;
    private boolean generateStonecutterRecipe = false;
    private @Nullable String recipeGroupPrefix;
    private @Nullable String recipeUnlockedBy;

    private BlockFamily(Block baseBlock) {
        this.baseBlock = baseBlock;
    }

    public Block getBaseBlock() {
        return this.baseBlock;
    }

    public Map<Variant, Block> getVariants() {
        return this.variants;
    }

    public Block get(Variant variant) {
        return this.variants.get((Object)variant);
    }

    public boolean shouldGenerateModel() {
        return this.generateModel;
    }

    public boolean shouldGenerateCraftingRecipe() {
        return this.generateCraftingRecipe;
    }

    public boolean shouldGenerateSmeltingRecipe() {
        return this.generateSmeltingRecipe;
    }

    public boolean shouldGenerateStonecutterRecipe() {
        return this.generateStonecutterRecipe;
    }

    public Optional<String> getRecipeGroupPrefix() {
        if (StringUtil.isBlank(this.recipeGroupPrefix)) {
            return Optional.empty();
        }
        return Optional.of(this.recipeGroupPrefix);
    }

    public Optional<String> getRecipeUnlockedBy() {
        if (StringUtil.isBlank(this.recipeUnlockedBy)) {
            return Optional.empty();
        }
        return Optional.of(this.recipeUnlockedBy);
    }

    public static class Builder {
        private final BlockFamily family;

        public Builder(Block baseBlock) {
            this.family = new BlockFamily(baseBlock);
        }

        public BlockFamily getFamily() {
            return this.family;
        }

        public Builder button(Block button) {
            this.family.variants.put(Variant.BUTTON, button);
            return this;
        }

        public Builder chiseled(Block chiseled) {
            this.family.variants.put(Variant.CHISELED, chiseled);
            return this;
        }

        public Builder mosaic(Block mosaic) {
            this.family.variants.put(Variant.MOSAIC, mosaic);
            return this;
        }

        public Builder cracked(Block cracked) {
            this.family.variants.put(Variant.CRACKED, cracked);
            return this;
        }

        public Builder tiles(Block tiles) {
            this.family.variants.put(Variant.TILES, tiles);
            return this;
        }

        public Builder pillar(Block pillar) {
            this.family.variants.put(Variant.PILLAR, pillar);
            return this;
        }

        public Builder cut(Block cut) {
            this.family.variants.put(Variant.CUT, cut);
            return this;
        }

        public Builder door(Block door) {
            this.family.variants.put(Variant.DOOR, door);
            return this;
        }

        public Builder customFence(Block fence) {
            this.family.variants.put(Variant.CUSTOM_FENCE, fence);
            return this;
        }

        public Builder fence(Block fence) {
            this.family.variants.put(Variant.FENCE, fence);
            return this;
        }

        public Builder customFenceGate(Block fenceGate) {
            this.family.variants.put(Variant.CUSTOM_FENCE_GATE, fenceGate);
            return this;
        }

        public Builder fenceGate(Block fenceGate) {
            this.family.variants.put(Variant.FENCE_GATE, fenceGate);
            return this;
        }

        public Builder sign(Block sign, Block wallSign) {
            this.family.variants.put(Variant.SIGN, sign);
            this.family.variants.put(Variant.WALL_SIGN, wallSign);
            return this;
        }

        public Builder customHangingSign(Block sign, Block wallSign) {
            this.family.variants.put(Variant.CUSTOM_HANGING_SIGN, sign);
            this.family.variants.put(Variant.CUSTOM_WALL_HANGING_SIGN, wallSign);
            return this;
        }

        public Builder hangingSign(Block sign, Block wallSign) {
            this.family.variants.put(Variant.HANGING_SIGN, sign);
            this.family.variants.put(Variant.WALL_HANGING_SIGN, wallSign);
            return this;
        }

        public Builder log(Block log) {
            this.family.variants.put(Variant.LOG, log);
            return this;
        }

        public Builder strippedLog(Block strippedLog) {
            this.family.variants.put(Variant.STRIPPED_LOG, strippedLog);
            return this;
        }

        public Builder slab(Block slab) {
            this.family.variants.put(Variant.SLAB, slab);
            return this;
        }

        public Builder stairs(Block stairs) {
            this.family.variants.put(Variant.STAIRS, stairs);
            return this;
        }

        public Builder pressurePlate(Block pressurePlate) {
            this.family.variants.put(Variant.PRESSURE_PLATE, pressurePlate);
            return this;
        }

        public Builder polished(Block polished) {
            this.family.variants.put(Variant.POLISHED, polished);
            return this;
        }

        public Builder trapdoor(Block trapdoor) {
            this.family.variants.put(Variant.TRAPDOOR, trapdoor);
            return this;
        }

        public Builder wall(Block wall) {
            this.family.variants.put(Variant.WALL, wall);
            return this;
        }

        public Builder cobbled(Block cobble) {
            this.family.variants.put(Variant.COBBLED, cobble);
            return this;
        }

        public Builder bricks(Block bricks) {
            this.family.variants.put(Variant.BRICKS, bricks);
            return this;
        }

        public Builder dontGenerateModel() {
            this.family.generateModel = false;
            return this;
        }

        public Builder dontGenerateCraftingRecipe() {
            this.family.generateCraftingRecipe = false;
            return this;
        }

        public Builder dontGenerateSmeltingRecipe() {
            this.family.generateSmeltingRecipe = false;
            return this;
        }

        public Builder generateStonecutterRecipe() {
            this.family.generateStonecutterRecipe = true;
            return this;
        }

        public Builder recipeGroupPrefix(String recipeGroupPrefix) {
            this.family.recipeGroupPrefix = recipeGroupPrefix;
            return this;
        }

        public Builder recipeUnlockedBy(String recipeUnlockedBy) {
            this.family.recipeUnlockedBy = recipeUnlockedBy;
            return this;
        }
    }

    public static enum Variant {
        BUTTON("button"),
        CHISELED("chiseled"){

            @Override
            public Variant getBaseVariantForCrafting() {
                return SLAB;
            }
        }
        ,
        CRACKED("cracked"),
        CUT("cut"){

            @Override
            public String getPrefixedRecipeGroup(String prefix) {
                return prefix;
            }
        }
        ,
        DOOR("door"),
        CUSTOM_FENCE("fence"),
        FENCE("fence"),
        CUSTOM_FENCE_GATE("fence_gate"),
        FENCE_GATE("fence_gate"),
        CUSTOM_HANGING_SIGN("hanging_sign"){

            @Override
            public Variant getBaseVariantForCrafting() {
                return STRIPPED_LOG;
            }
        }
        ,
        HANGING_SIGN("hanging_sign"){

            @Override
            public Variant getBaseVariantForCrafting() {
                return STRIPPED_LOG;
            }
        }
        ,
        LOG("log"),
        STRIPPED_LOG("stripped_log"),
        MOSAIC("mosaic"),
        SIGN("sign"),
        SLAB("slab"),
        STAIRS("stairs"),
        PRESSURE_PLATE("pressure_plate"),
        POLISHED("polished"),
        TRAPDOOR("trapdoor"),
        WALL("wall"),
        WALL_SIGN("wall_sign"),
        CUSTOM_WALL_HANGING_SIGN("wall_hanging_sign"),
        WALL_HANGING_SIGN("wall_hanging_sign"),
        BRICKS("bricks"),
        COBBLED("cobbled"),
        TILES("tiles"),
        PILLAR("pillar");

        private final String recipeGroup;

        private Variant(String recipeGroup) {
            this.recipeGroup = recipeGroup;
        }

        public String getRecipeGroup() {
            return this.recipeGroup;
        }

        public String getPrefixedRecipeGroup(String prefix) {
            return prefix + "_" + this.getRecipeGroup();
        }

        public @Nullable Variant getBaseVariantForCrafting() {
            return null;
        }
    }
}

