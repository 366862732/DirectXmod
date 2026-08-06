/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.model;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

@Environment(value=EnvType.CLIENT)
public class TexturedModel {
    public static final Provider CUBE = TexturedModel.createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL);
    public static final Provider CUBE_INNER_FACES = TexturedModel.createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL_INNER_FACES);
    public static final Provider CUBE_MIRRORED = TexturedModel.createDefault(TextureMapping::cube, ModelTemplates.CUBE_MIRRORED_ALL);
    public static final Provider COLUMN = TexturedModel.createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN);
    public static final Provider COLUMN_HORIZONTAL = TexturedModel.createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
    public static final Provider CUBE_TOP_BOTTOM = TexturedModel.createDefault(TextureMapping::cubeBottomTop, ModelTemplates.CUBE_BOTTOM_TOP);
    public static final Provider CUBE_TOP = TexturedModel.createDefault(TextureMapping::cubeTop, ModelTemplates.CUBE_TOP);
    public static final Provider ORIENTABLE_ONLY_TOP = TexturedModel.createDefault(TextureMapping::orientableCubeOnlyTop, ModelTemplates.CUBE_ORIENTABLE);
    public static final Provider ORIENTABLE = TexturedModel.createDefault(TextureMapping::orientableCube, ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM);
    public static final Provider CARPET = TexturedModel.createDefault(TextureMapping::wool, ModelTemplates.CARPET);
    public static final Provider MOSSY_CARPET_SIDE = TexturedModel.createDefault(TextureMapping::side, ModelTemplates.MOSSY_CARPET_SIDE);
    public static final Provider FLOWERBED_1 = TexturedModel.createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_1);
    public static final Provider FLOWERBED_2 = TexturedModel.createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_2);
    public static final Provider FLOWERBED_3 = TexturedModel.createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_3);
    public static final Provider FLOWERBED_4 = TexturedModel.createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_4);
    public static final Provider LEAF_LITTER_1 = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_1);
    public static final Provider LEAF_LITTER_2 = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_2);
    public static final Provider LEAF_LITTER_3 = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_3);
    public static final Provider LEAF_LITTER_4 = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_4);
    public static final Provider GLAZED_TERRACOTTA = TexturedModel.createDefault(TextureMapping::pattern, ModelTemplates.GLAZED_TERRACOTTA);
    public static final Provider CORAL_FAN = TexturedModel.createDefault(TextureMapping::fan, ModelTemplates.CORAL_FAN);
    public static final Provider ANVIL = TexturedModel.createDefault(TextureMapping::top, ModelTemplates.ANVIL);
    public static final Provider LEAVES = TexturedModel.createDefault(TextureMapping::cube, ModelTemplates.LEAVES);
    public static final Provider LANTERN = TexturedModel.createDefault(TextureMapping::lantern, ModelTemplates.LANTERN);
    public static final Provider HANGING_LANTERN = TexturedModel.createDefault(TextureMapping::lantern, ModelTemplates.HANGING_LANTERN);
    public static final Provider CHAIN = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.CHAIN);
    public static final Provider SEAGRASS = TexturedModel.createDefault(TextureMapping::defaultTexture, ModelTemplates.SEAGRASS);
    public static final Provider COLUMN_ALT = TexturedModel.createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN);
    public static final Provider COLUMN_HORIZONTAL_ALT = TexturedModel.createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
    public static final Provider TOP_BOTTOM_WITH_WALL = TexturedModel.createDefault(TextureMapping::cubeBottomTopWithWall, ModelTemplates.CUBE_BOTTOM_TOP);
    public static final Provider COLUMN_WITH_WALL = TexturedModel.createDefault(TextureMapping::columnWithWall, ModelTemplates.CUBE_COLUMN);
    private final TextureMapping mapping;
    private final ModelTemplate template;

    private TexturedModel(TextureMapping mapping, ModelTemplate template) {
        this.mapping = mapping;
        this.template = template;
    }

    public ModelTemplate getTemplate() {
        return this.template;
    }

    public TextureMapping getMapping() {
        return this.mapping;
    }

    public TexturedModel updateTextures(Consumer<TextureMapping> mutator) {
        mutator.accept(this.mapping);
        return this;
    }

    public Identifier create(Block block, BiConsumer<Identifier, ModelInstance> modelOutput) {
        return this.template.create(block, this.mapping, modelOutput);
    }

    public Identifier createWithSuffix(Block block, String extraSuffix, BiConsumer<Identifier, ModelInstance> modelOutput) {
        return this.template.createWithSuffix(block, extraSuffix, this.mapping, modelOutput);
    }

    private static Provider createDefault(Function<Block, TextureMapping> mapping, ModelTemplate template) {
        return block -> new TexturedModel((TextureMapping)mapping.apply(block), template);
    }

    public static TexturedModel createAllSame(Material material) {
        return new TexturedModel(TextureMapping.cube(material), ModelTemplates.CUBE_ALL);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Provider {
        public TexturedModel get(Block var1);

        default public Identifier create(Block block, BiConsumer<Identifier, ModelInstance> modelOutput) {
            return this.get(block).create(block, modelOutput);
        }

        default public Identifier createWithSuffix(Block block, String suffix, BiConsumer<Identifier, ModelInstance> modelOutput) {
            return this.get(block).createWithSuffix(block, suffix, modelOutput);
        }

        default public Provider updateTexture(Consumer<TextureMapping> mutator) {
            return block -> this.get(block).updateTextures(mutator);
        }
    }
}

