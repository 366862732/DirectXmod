/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.util.Function3
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.data.models;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.block.dispatch.multipart.CombinedCondition;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.special.ConduitSpecialRenderer;
import net.minecraft.client.renderer.special.CopperGolemStatueSpecialRenderer;
import net.minecraft.client.renderer.special.DecoratedPotSpecialRenderer;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.DriedGhastBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnifferEggBlock;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.block.state.properties.Tilt;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BlockModelGenerators {
    private final Consumer<BlockModelDefinitionGenerator> blockStateOutput;
    private final ItemModelOutput itemModelOutput;
    private final BiConsumer<Identifier, ModelInstance> modelOutput;
    private static final List<Block> NON_ORIENTABLE_TRAPDOOR = List.of(Blocks.OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
    public static final VariantMutator NOP = v -> v;
    public static final VariantMutator UV_LOCK = VariantMutator.UV_LOCK.withValue(true);
    public static final VariantMutator X_ROT_90 = VariantMutator.X_ROT.withValue(Quadrant.R90);
    public static final VariantMutator X_ROT_180 = VariantMutator.X_ROT.withValue(Quadrant.R180);
    public static final VariantMutator X_ROT_270 = VariantMutator.X_ROT.withValue(Quadrant.R270);
    public static final VariantMutator Y_ROT_90 = VariantMutator.Y_ROT.withValue(Quadrant.R90);
    public static final VariantMutator Y_ROT_180 = VariantMutator.Y_ROT.withValue(Quadrant.R180);
    public static final VariantMutator Y_ROT_270 = VariantMutator.Y_ROT.withValue(Quadrant.R270);
    private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_1_SEGMENT_CONDITION = condition -> condition;
    private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_2_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.FLOWER_AMOUNT, Integer.valueOf(2), new Integer[]{3, 4});
    private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_3_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.FLOWER_AMOUNT, Integer.valueOf(3), new Integer[]{4});
    private static final Function<ConditionBuilder, ConditionBuilder> FLOWER_BED_MODEL_4_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.FLOWER_AMOUNT, 4);
    private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_1_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.SEGMENT_AMOUNT, 1);
    private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_2_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.SEGMENT_AMOUNT, Integer.valueOf(2), new Integer[]{3});
    private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_3_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.SEGMENT_AMOUNT, 3);
    private static final Function<ConditionBuilder, ConditionBuilder> LEAF_LITTER_MODEL_4_SEGMENT_CONDITION = condition -> condition.term(BlockStateProperties.SEGMENT_AMOUNT, 4);
    private static final Transformation SKULL_TRANSFORM = new Transformation((Vector3fc)new Vector3f(0.5f, 0.0f, 0.5f), (Quaternionfc)new Quaternionf().rotationX((float)Math.PI), null, null);
    private static final Map<Block, BlockStateGeneratorSupplier> FULL_BLOCK_MODEL_CUSTOM_GENERATORS = Map.of(Blocks.STONE, BlockModelGenerators::createMirroredCubeGenerator, Blocks.DEEPSLATE, BlockModelGenerators::createMirroredColumnGenerator, Blocks.MUD_BRICKS, BlockModelGenerators::createNorthWestMirroredCubeGenerator);
    private static final PropertyDispatch<VariantMutator> ROTATION_FACING = PropertyDispatch.modify(BlockStateProperties.FACING).select(Direction.DOWN, X_ROT_90).select(Direction.UP, X_ROT_270).select(Direction.NORTH, NOP).select(Direction.SOUTH, Y_ROT_180).select(Direction.WEST, Y_ROT_270).select(Direction.EAST, Y_ROT_90);
    private static final PropertyDispatch<VariantMutator> ROTATIONS_COLUMN_WITH_FACING = PropertyDispatch.modify(BlockStateProperties.FACING).select(Direction.DOWN, X_ROT_180).select(Direction.UP, NOP).select(Direction.NORTH, X_ROT_90).select(Direction.SOUTH, X_ROT_90.then(Y_ROT_180)).select(Direction.WEST, X_ROT_90.then(Y_ROT_270)).select(Direction.EAST, X_ROT_90.then(Y_ROT_90));
    private static final PropertyDispatch<VariantMutator> ROTATION_TORCH = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING).select(Direction.EAST, NOP).select(Direction.SOUTH, Y_ROT_90).select(Direction.WEST, Y_ROT_180).select(Direction.NORTH, Y_ROT_270);
    private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING_ALT = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING).select(Direction.SOUTH, NOP).select(Direction.WEST, Y_ROT_90).select(Direction.NORTH, Y_ROT_180).select(Direction.EAST, Y_ROT_270);
    private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING).select(Direction.EAST, Y_ROT_90).select(Direction.SOUTH, Y_ROT_180).select(Direction.WEST, Y_ROT_270).select(Direction.NORTH, NOP);
    private static final Map<Block, TexturedModel> TEXTURED_MODELS = ImmutableMap.builder().put((Object)Blocks.SANDSTONE, (Object)TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.SANDSTONE)).put((Object)Blocks.RED_SANDSTONE, (Object)TexturedModel.TOP_BOTTOM_WITH_WALL.get(Blocks.RED_SANDSTONE)).put((Object)Blocks.SMOOTH_SANDSTONE, (Object)TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"))).put((Object)Blocks.SMOOTH_RED_SANDSTONE, (Object)TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"))).put((Object)Blocks.CUT_SANDSTONE, (Object)TexturedModel.COLUMN.get(Blocks.SANDSTONE).updateTextures(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_SANDSTONE)))).put((Object)Blocks.CUT_RED_SANDSTONE, (Object)TexturedModel.COLUMN.get(Blocks.RED_SANDSTONE).updateTextures(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CUT_RED_SANDSTONE)))).put((Object)Blocks.QUARTZ_BLOCK, (Object)TexturedModel.COLUMN.get(Blocks.QUARTZ_BLOCK)).put((Object)Blocks.SMOOTH_QUARTZ, (Object)TexturedModel.createAllSame(TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom"))).put((Object)Blocks.BLACKSTONE, (Object)TexturedModel.COLUMN_WITH_WALL.get(Blocks.BLACKSTONE)).put((Object)Blocks.DEEPSLATE, (Object)TexturedModel.COLUMN_WITH_WALL.get(Blocks.DEEPSLATE)).put((Object)Blocks.CHISELED_QUARTZ_BLOCK, (Object)TexturedModel.COLUMN.get(Blocks.CHISELED_QUARTZ_BLOCK).updateTextures(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_QUARTZ_BLOCK)))).put((Object)Blocks.CHISELED_SANDSTONE, (Object)TexturedModel.COLUMN.get(Blocks.CHISELED_SANDSTONE).updateTextures(m -> {
        m.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top"));
        m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_SANDSTONE));
    })).put((Object)Blocks.CHISELED_RED_SANDSTONE, (Object)TexturedModel.COLUMN.get(Blocks.CHISELED_RED_SANDSTONE).updateTextures(m -> {
        m.put(TextureSlot.END, TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE, "_top"));
        m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CHISELED_RED_SANDSTONE));
    })).put((Object)Blocks.CHISELED_TUFF_BRICKS, (Object)TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF_BRICKS)).put((Object)Blocks.CHISELED_TUFF, (Object)TexturedModel.COLUMN_WITH_WALL.get(Blocks.CHISELED_TUFF)).build();
    private static final Map<BlockFamily.Variant, BiConsumer<BlockFamilyProvider, Block>> SHAPE_CONSUMERS = ImmutableMap.builder().put((Object)BlockFamily.Variant.BUTTON, BlockFamilyProvider::button).put((Object)BlockFamily.Variant.DOOR, BlockFamilyProvider::door).put((Object)BlockFamily.Variant.CHISELED, BlockFamilyProvider::fullBlockVariant).put((Object)BlockFamily.Variant.CRACKED, BlockFamilyProvider::fullBlockVariant).put((Object)BlockFamily.Variant.CUSTOM_FENCE, BlockFamilyProvider::customFence).put((Object)BlockFamily.Variant.FENCE, BlockFamilyProvider::fence).put((Object)BlockFamily.Variant.CUSTOM_FENCE_GATE, BlockFamilyProvider::customFenceGate).put((Object)BlockFamily.Variant.FENCE_GATE, BlockFamilyProvider::fenceGate).put((Object)BlockFamily.Variant.SIGN, BlockFamilyProvider::sign).put((Object)BlockFamily.Variant.CUSTOM_HANGING_SIGN, BlockFamilyProvider::customHangingSign).put((Object)BlockFamily.Variant.HANGING_SIGN, BlockFamilyProvider::hangingSign).put((Object)BlockFamily.Variant.SLAB, BlockFamilyProvider::slab).put((Object)BlockFamily.Variant.STAIRS, BlockFamilyProvider::stairs).put((Object)BlockFamily.Variant.PRESSURE_PLATE, BlockFamilyProvider::pressurePlate).put((Object)BlockFamily.Variant.TRAPDOOR, BlockFamilyProvider::trapdoor).put((Object)BlockFamily.Variant.WALL, BlockFamilyProvider::wall).put((Object)BlockFamily.Variant.BRICKS, BlockFamilyProvider::fullBlockVariant).put((Object)BlockFamily.Variant.TILES, BlockFamilyProvider::fullBlockVariant).put((Object)BlockFamily.Variant.COBBLED, BlockFamilyProvider::fullBlockVariant).put((Object)BlockFamily.Variant.PILLAR, BlockFamilyProvider::pillar).build();
    private static final Map<Direction, VariantMutator> MULTIFACE_GENERATOR = ImmutableMap.of((Object)Direction.NORTH, (Object)NOP, (Object)Direction.EAST, (Object)Y_ROT_90.then(UV_LOCK), (Object)Direction.SOUTH, (Object)Y_ROT_180.then(UV_LOCK), (Object)Direction.WEST, (Object)Y_ROT_270.then(UV_LOCK), (Object)Direction.UP, (Object)X_ROT_270.then(UV_LOCK), (Object)Direction.DOWN, (Object)X_ROT_90.then(UV_LOCK));
    private static final Map<BookSlotModelCacheKey, Identifier> CHISELED_BOOKSHELF_SLOT_MODEL_CACHE = new HashMap<BookSlotModelCacheKey, Identifier>();

    private static Variant plainModel(Identifier model) {
        return new Variant(model);
    }

    private static MultiVariant variant(Variant variant) {
        return new MultiVariant(WeightedList.of(variant));
    }

    private static MultiVariant variants(Variant ... variant) {
        return new MultiVariant(WeightedList.of(Arrays.stream(variant).map(v -> new Weighted<Variant>((Variant)v, 1)).toList()));
    }

    private static MultiVariant plainVariant(Identifier model) {
        return BlockModelGenerators.variant(BlockModelGenerators.plainModel(model));
    }

    private static ConditionBuilder condition() {
        return new ConditionBuilder();
    }

    @SafeVarargs
    private static <T extends Enum<T>> ConditionBuilder condition(EnumProperty<T> property, T term, T ... additionalTerms) {
        return BlockModelGenerators.condition().term(property, (Comparable)((Object)term), (Comparable[])additionalTerms);
    }

    private static ConditionBuilder condition(BooleanProperty property, boolean term) {
        return BlockModelGenerators.condition().term(property, term);
    }

    private static Condition or(ConditionBuilder ... terms) {
        return new CombinedCondition(CombinedCondition.Operation.OR, Stream.of(terms).map(ConditionBuilder::build).toList());
    }

    private static Condition and(ConditionBuilder ... terms) {
        return new CombinedCondition(CombinedCondition.Operation.AND, Stream.of(terms).map(ConditionBuilder::build).toList());
    }

    private static BlockModelDefinitionGenerator createMirroredCubeGenerator(Block block, Variant normal, TextureMapping mapping, BiConsumer<Identifier, ModelInstance> modelOutput) {
        Variant mirrored = BlockModelGenerators.plainModel(ModelTemplates.CUBE_MIRRORED_ALL.create(block, mapping, modelOutput));
        return MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(normal, mirrored));
    }

    private static BlockModelDefinitionGenerator createNorthWestMirroredCubeGenerator(Block block, Variant normal, TextureMapping mapping, BiConsumer<Identifier, ModelInstance> modelOutput) {
        MultiVariant northWestMirrored = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_NORTH_WEST_MIRRORED_ALL.create(block, mapping, modelOutput));
        return BlockModelGenerators.createSimpleBlock(block, northWestMirrored);
    }

    private static BlockModelDefinitionGenerator createMirroredColumnGenerator(Block block, Variant normal, TextureMapping mapping, BiConsumer<Identifier, ModelInstance> modelOutput) {
        Variant mirrored = BlockModelGenerators.plainModel(ModelTemplates.CUBE_COLUMN_MIRRORED.create(block, mapping, modelOutput));
        return MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(normal, mirrored)).with(BlockModelGenerators.createRotatedPillar());
    }

    public BlockModelGenerators(Consumer<BlockModelDefinitionGenerator> blockStateOutput, ItemModelOutput itemModelOutput, BiConsumer<Identifier, ModelInstance> modelOutput) {
        this.blockStateOutput = blockStateOutput;
        this.itemModelOutput = itemModelOutput;
        this.modelOutput = modelOutput;
    }

    private void registerSimpleItemModel(Item item, Identifier model) {
        this.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
    }

    private void registerSimpleItemModel(Block block, Identifier model) {
        this.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(model));
    }

    private void registerSimpleTintedItemModel(Block block, Identifier model, ItemTintSource tint) {
        this.itemModelOutput.accept(block.asItem(), ItemModelUtils.tintedModel(model, tint));
    }

    private Identifier createFlatItemModel(Item item) {
        return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), this.modelOutput);
    }

    private Identifier createFlatItemModelWithBlockTexture(Item item, Block block) {
        return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(block), this.modelOutput);
    }

    private Identifier createFlatItemModelWithBlockTexture(Item item, Block block, String suffix) {
        return ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(TextureMapping.getBlockTexture(block, suffix)), this.modelOutput);
    }

    private Identifier createFlatItemModelWithBlockTextureAndOverlay(Item item, Block block, String overlaySuffix) {
        Material base = TextureMapping.getBlockTexture(block);
        Material overlay = TextureMapping.getBlockTexture(block, overlaySuffix);
        return ModelTemplates.TWO_LAYERED_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layered(base, overlay), this.modelOutput);
    }

    private void registerSimpleFlatItemModel(Item item) {
        this.registerSimpleItemModel(item, this.createFlatItemModel(item));
    }

    private void registerSimpleFlatItemModel(Block block) {
        Item blockItem = block.asItem();
        if (blockItem != Items.AIR) {
            this.registerSimpleItemModel(blockItem, this.createFlatItemModelWithBlockTexture(blockItem, block));
        }
    }

    private void registerSimpleFlatItemModel(Block block, String suffix) {
        Item blockItem = block.asItem();
        if (blockItem != Items.AIR) {
            this.registerSimpleItemModel(blockItem, this.createFlatItemModelWithBlockTexture(blockItem, block, suffix));
        }
    }

    private void registerTwoLayerFlatItemModel(Block block, String overlaySuffix) {
        Item blockItem = block.asItem();
        if (blockItem != Items.AIR) {
            Identifier model = this.createFlatItemModelWithBlockTextureAndOverlay(blockItem, block, overlaySuffix);
            this.registerSimpleItemModel(blockItem, model);
        }
    }

    private static MultiVariant createRotatedVariants(Variant base) {
        return BlockModelGenerators.variants(base, base.with(Y_ROT_90), base.with(Y_ROT_180), base.with(Y_ROT_270));
    }

    private static MultiVariant createRotatedVariants(Variant normal, Variant mirrored) {
        return BlockModelGenerators.variants(normal, mirrored, normal.with(Y_ROT_180), mirrored.with(Y_ROT_180));
    }

    private static PropertyDispatch<MultiVariant> createBooleanModelDispatch(BooleanProperty property, MultiVariant onTrue, MultiVariant onFalse) {
        return PropertyDispatch.initial(property).select(true, onTrue).select(false, onFalse);
    }

    private void createRotatedMirroredVariantBlock(Block block) {
        Variant normal = BlockModelGenerators.plainModel(TexturedModel.CUBE.create(block, this.modelOutput));
        Variant mirrored = BlockModelGenerators.plainModel(TexturedModel.CUBE_MIRRORED.create(block, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(normal, mirrored)));
    }

    private void createRotatedVariantBlock(Block block) {
        Variant normal = BlockModelGenerators.plainModel(TexturedModel.CUBE.create(block, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(normal)));
    }

    private void createBrushableBlock(Block block) {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.DUSTED).generate(dustProgress -> {
            String suffix = "_" + dustProgress;
            Material texture = TextureMapping.getBlockTexture(block, suffix);
            Identifier model = ModelTemplates.CUBE_ALL.createWithSuffix(block, suffix, new TextureMapping().put(TextureSlot.ALL, texture), this.modelOutput);
            return BlockModelGenerators.plainVariant(model);
        })));
        this.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block, "_0"));
    }

    private static BlockModelDefinitionGenerator createButton(Block block, MultiVariant normal, MultiVariant pressed) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.POWERED).select(false, normal).select(true, pressed)).with(PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING).select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90).select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270).select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180).select(AttachFace.FLOOR, Direction.NORTH, NOP).select(AttachFace.WALL, Direction.EAST, Y_ROT_90.then(X_ROT_90).then(UV_LOCK)).select(AttachFace.WALL, Direction.WEST, Y_ROT_270.then(X_ROT_90).then(UV_LOCK)).select(AttachFace.WALL, Direction.SOUTH, Y_ROT_180.then(X_ROT_90).then(UV_LOCK)).select(AttachFace.WALL, Direction.NORTH, X_ROT_90.then(UV_LOCK)).select(AttachFace.CEILING, Direction.EAST, Y_ROT_270.then(X_ROT_180)).select(AttachFace.CEILING, Direction.WEST, Y_ROT_90.then(X_ROT_180)).select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180).select(AttachFace.CEILING, Direction.NORTH, Y_ROT_180.then(X_ROT_180)));
    }

    private static BlockModelDefinitionGenerator createDoor(Block block, MultiVariant bottomLeft, MultiVariant bottomLeftOpen, MultiVariant bottomRight, MultiVariant bottomRightOpen, MultiVariant topLeft, MultiVariant topLeftOpen, MultiVariant topRight, MultiVariant topRightOpen) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.DOOR_HINGE, BlockStateProperties.OPEN).select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, bottomLeft).select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_90)).select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_180)).select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, false, bottomLeft.with(Y_ROT_270)).select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, bottomRight).select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_90)).select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_180)).select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, false, bottomRight.with(Y_ROT_270)).select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_90)).select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_180)).select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, bottomLeftOpen.with(Y_ROT_270)).select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.LEFT, true, bottomLeftOpen).select(Direction.EAST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_270)).select(Direction.SOUTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, bottomRightOpen).select(Direction.WEST, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_90)).select(Direction.NORTH, DoubleBlockHalf.LOWER, DoorHingeSide.RIGHT, true, bottomRightOpen.with(Y_ROT_180)).select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, topLeft).select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_90)).select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_180)).select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, false, topLeft.with(Y_ROT_270)).select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, topRight).select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_90)).select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_180)).select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, false, topRight.with(Y_ROT_270)).select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_90)).select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_180)).select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, topLeftOpen.with(Y_ROT_270)).select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.LEFT, true, topLeftOpen).select(Direction.EAST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_270)).select(Direction.SOUTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, topRightOpen).select(Direction.WEST, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_90)).select(Direction.NORTH, DoubleBlockHalf.UPPER, DoorHingeSide.RIGHT, true, topRightOpen.with(Y_ROT_180)));
    }

    private static BlockModelDefinitionGenerator createCustomFence(Block block, MultiVariant post, MultiVariant north, MultiVariant east, MultiVariant south, MultiVariant west) {
        return MultiPartGenerator.multiPart(block).with(post).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), north).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), east).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), south).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), west);
    }

    private static BlockModelDefinitionGenerator createFence(Block block, MultiVariant post, MultiVariant side) {
        return MultiPartGenerator.multiPart(block).with(post).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), side.with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), side.with(Y_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), side.with(Y_ROT_180).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), side.with(Y_ROT_270).with(UV_LOCK));
    }

    private static BlockModelDefinitionGenerator createWall(Block block, MultiVariant post, MultiVariant lowSide, MultiVariant tallSide) {
        return MultiPartGenerator.multiPart(block).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), post).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH_WALL, WallSide.LOW), lowSide.with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST_WALL, WallSide.LOW), lowSide.with(Y_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.LOW), lowSide.with(Y_ROT_180).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST_WALL, WallSide.LOW), lowSide.with(Y_ROT_270).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH_WALL, WallSide.TALL), tallSide.with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST_WALL, WallSide.TALL), tallSide.with(Y_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH_WALL, WallSide.TALL), tallSide.with(Y_ROT_180).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST_WALL, WallSide.TALL), tallSide.with(Y_ROT_270).with(UV_LOCK));
    }

    private static BlockModelDefinitionGenerator createFenceGate(Block block, MultiVariant open, MultiVariant closed, MultiVariant openWall, MultiVariant closedWall, boolean uvLock) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.IN_WALL, BlockStateProperties.OPEN).select(false, false, closed).select(true, false, closedWall).select(false, true, open).select(true, true, openWall)).with(uvLock ? UV_LOCK : NOP).with(ROTATION_HORIZONTAL_FACING_ALT);
    }

    private static BlockModelDefinitionGenerator createStairs(Block block, MultiVariant inner, MultiVariant straight, MultiVariant outer) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE).select(Direction.EAST, Half.BOTTOM, StairsShape.STRAIGHT, straight).select(Direction.WEST, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.BOTTOM, StairsShape.STRAIGHT, straight.with(Y_ROT_270).with(UV_LOCK)).select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer).select(Direction.WEST, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.BOTTOM, StairsShape.OUTER_RIGHT, outer.with(Y_ROT_270).with(UV_LOCK)).select(Direction.EAST, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_270).with(UV_LOCK)).select(Direction.WEST, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_90).with(UV_LOCK)).select(Direction.SOUTH, Half.BOTTOM, StairsShape.OUTER_LEFT, outer).select(Direction.NORTH, Half.BOTTOM, StairsShape.OUTER_LEFT, outer.with(Y_ROT_180).with(UV_LOCK)).select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_RIGHT, inner).select(Direction.WEST, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.BOTTOM, StairsShape.INNER_RIGHT, inner.with(Y_ROT_270).with(UV_LOCK)).select(Direction.EAST, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_270).with(UV_LOCK)).select(Direction.WEST, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_90).with(UV_LOCK)).select(Direction.SOUTH, Half.BOTTOM, StairsShape.INNER_LEFT, inner).select(Direction.NORTH, Half.BOTTOM, StairsShape.INNER_LEFT, inner.with(Y_ROT_180).with(UV_LOCK)).select(Direction.EAST, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(UV_LOCK)).select(Direction.WEST, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.TOP, StairsShape.STRAIGHT, straight.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK)).select(Direction.EAST, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK)).select(Direction.WEST, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK)).select(Direction.SOUTH, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK)).select(Direction.NORTH, Half.TOP, StairsShape.OUTER_RIGHT, outer.with(X_ROT_180).with(UV_LOCK)).select(Direction.EAST, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(UV_LOCK)).select(Direction.WEST, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.TOP, StairsShape.OUTER_LEFT, outer.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK)).select(Direction.EAST, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK)).select(Direction.WEST, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK)).select(Direction.SOUTH, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK)).select(Direction.NORTH, Half.TOP, StairsShape.INNER_RIGHT, inner.with(X_ROT_180).with(UV_LOCK)).select(Direction.EAST, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(UV_LOCK)).select(Direction.WEST, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_180).with(UV_LOCK)).select(Direction.SOUTH, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_90).with(UV_LOCK)).select(Direction.NORTH, Half.TOP, StairsShape.INNER_LEFT, inner.with(X_ROT_180).with(Y_ROT_270).with(UV_LOCK)));
    }

    private static BlockModelDefinitionGenerator createOrientableTrapdoor(Block block, MultiVariant top, MultiVariant bottom, MultiVariant open) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN).select(Direction.NORTH, Half.BOTTOM, false, bottom).select(Direction.SOUTH, Half.BOTTOM, false, bottom.with(Y_ROT_180)).select(Direction.EAST, Half.BOTTOM, false, bottom.with(Y_ROT_90)).select(Direction.WEST, Half.BOTTOM, false, bottom.with(Y_ROT_270)).select(Direction.NORTH, Half.TOP, false, top).select(Direction.SOUTH, Half.TOP, false, top.with(Y_ROT_180)).select(Direction.EAST, Half.TOP, false, top.with(Y_ROT_90)).select(Direction.WEST, Half.TOP, false, top.with(Y_ROT_270)).select(Direction.NORTH, Half.BOTTOM, true, open).select(Direction.SOUTH, Half.BOTTOM, true, open.with(Y_ROT_180)).select(Direction.EAST, Half.BOTTOM, true, open.with(Y_ROT_90)).select(Direction.WEST, Half.BOTTOM, true, open.with(Y_ROT_270)).select(Direction.NORTH, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_180)).select(Direction.SOUTH, Half.TOP, true, open.with(X_ROT_180)).select(Direction.EAST, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_270)).select(Direction.WEST, Half.TOP, true, open.with(X_ROT_180).with(Y_ROT_90)));
    }

    private static BlockModelDefinitionGenerator createTrapdoor(Block block, MultiVariant top, MultiVariant bottom, MultiVariant open) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.OPEN).select(Direction.NORTH, Half.BOTTOM, false, bottom).select(Direction.SOUTH, Half.BOTTOM, false, bottom).select(Direction.EAST, Half.BOTTOM, false, bottom).select(Direction.WEST, Half.BOTTOM, false, bottom).select(Direction.NORTH, Half.TOP, false, top).select(Direction.SOUTH, Half.TOP, false, top).select(Direction.EAST, Half.TOP, false, top).select(Direction.WEST, Half.TOP, false, top).select(Direction.NORTH, Half.BOTTOM, true, open).select(Direction.SOUTH, Half.BOTTOM, true, open.with(Y_ROT_180)).select(Direction.EAST, Half.BOTTOM, true, open.with(Y_ROT_90)).select(Direction.WEST, Half.BOTTOM, true, open.with(Y_ROT_270)).select(Direction.NORTH, Half.TOP, true, open).select(Direction.SOUTH, Half.TOP, true, open.with(Y_ROT_180)).select(Direction.EAST, Half.TOP, true, open.with(Y_ROT_90)).select(Direction.WEST, Half.TOP, true, open.with(Y_ROT_270)));
    }

    private static BlockModelDefinitionGenerator createBed(Block block, MultiVariant headModel, MultiVariant footModel) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BED_PART).select(Direction.NORTH, BedPart.HEAD, headModel).select(Direction.SOUTH, BedPart.HEAD, headModel.with(Y_ROT_180)).select(Direction.EAST, BedPart.HEAD, headModel.with(Y_ROT_90)).select(Direction.WEST, BedPart.HEAD, headModel.with(Y_ROT_270)).select(Direction.NORTH, BedPart.FOOT, footModel).select(Direction.SOUTH, BedPart.FOOT, footModel.with(Y_ROT_180)).select(Direction.EAST, BedPart.FOOT, footModel.with(Y_ROT_90)).select(Direction.WEST, BedPart.FOOT, footModel.with(Y_ROT_270)));
    }

    private static BlockModelDefinitionGenerator createSign(Block block, MultiVariant rot0, MultiVariant rot1, MultiVariant rot2, MultiVariant rot3) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.ROTATION_16).select(0, rot0).select(1, rot1).select(2, rot2).select(3, rot3).select(4, rot0.with(Y_ROT_90)).select(5, rot1.with(Y_ROT_90)).select(6, rot2.with(Y_ROT_90)).select(7, rot3.with(Y_ROT_90)).select(8, rot0.with(Y_ROT_180)).select(9, rot1.with(Y_ROT_180)).select(10, rot2.with(Y_ROT_180)).select(11, rot3.with(Y_ROT_180)).select(12, rot0.with(Y_ROT_270)).select(13, rot1.with(Y_ROT_270)).select(14, rot2.with(Y_ROT_270)).select(15, rot3.with(Y_ROT_270)));
    }

    private static BlockModelDefinitionGenerator createHangingSign(Block block, MultiVariant rot0, MultiVariant rot1, MultiVariant rot2, MultiVariant rot3, MultiVariant attachedRot0, MultiVariant attachedRot1, MultiVariant attachedRot2, MultiVariant attachedRot3) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.ATTACHED, BlockStateProperties.ROTATION_16).select(false, 0, rot0).select(false, 1, rot1).select(false, 2, rot2).select(false, 3, rot3).select(false, 4, rot0.with(Y_ROT_90)).select(false, 5, rot1.with(Y_ROT_90)).select(false, 6, rot2.with(Y_ROT_90)).select(false, 7, rot3.with(Y_ROT_90)).select(false, 8, rot0.with(Y_ROT_180)).select(false, 9, rot1.with(Y_ROT_180)).select(false, 10, rot2.with(Y_ROT_180)).select(false, 11, rot3.with(Y_ROT_180)).select(false, 12, rot0.with(Y_ROT_270)).select(false, 13, rot1.with(Y_ROT_270)).select(false, 14, rot2.with(Y_ROT_270)).select(false, 15, rot3.with(Y_ROT_270)).select(true, 0, attachedRot0).select(true, 1, attachedRot1).select(true, 2, attachedRot2).select(true, 3, attachedRot3).select(true, 4, attachedRot0.with(Y_ROT_90)).select(true, 5, attachedRot1.with(Y_ROT_90)).select(true, 6, attachedRot2.with(Y_ROT_90)).select(true, 7, attachedRot3.with(Y_ROT_90)).select(true, 8, attachedRot0.with(Y_ROT_180)).select(true, 9, attachedRot1.with(Y_ROT_180)).select(true, 10, attachedRot2.with(Y_ROT_180)).select(true, 11, attachedRot3.with(Y_ROT_180)).select(true, 12, attachedRot0.with(Y_ROT_270)).select(true, 13, attachedRot1.with(Y_ROT_270)).select(true, 14, attachedRot2.with(Y_ROT_270)).select(true, 15, attachedRot3.with(Y_ROT_270)));
    }

    private static MultiVariantGenerator createSimpleBlock(Block block, MultiVariant variant) {
        return MultiVariantGenerator.dispatch(block, variant);
    }

    private static PropertyDispatch<VariantMutator> createRotatedPillar() {
        return PropertyDispatch.modify(BlockStateProperties.AXIS).select(Direction.Axis.Y, NOP).select(Direction.Axis.Z, X_ROT_90).select(Direction.Axis.X, X_ROT_90.then(Y_ROT_90));
    }

    private static BlockModelDefinitionGenerator createPillarBlockUVLocked(Block block, TextureMapping mapping, BiConsumer<Identifier, ModelInstance> modelOutput) {
        MultiVariant xAxisModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_X.create(block, mapping, modelOutput));
        MultiVariant yAxisModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_Y.create(block, mapping, modelOutput));
        MultiVariant zAxisModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN_UV_LOCKED_Z.create(block, mapping, modelOutput));
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.AXIS).select(Direction.Axis.X, xAxisModel).select(Direction.Axis.Y, yAxisModel).select(Direction.Axis.Z, zAxisModel));
    }

    private static BlockModelDefinitionGenerator createAxisAlignedPillarBlock(Block block, MultiVariant model) {
        return MultiVariantGenerator.dispatch(block, model).with(BlockModelGenerators.createRotatedPillar());
    }

    private void createAxisAlignedPillarBlockCustomModel(Block block, MultiVariant model) {
        this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block, model));
    }

    private void createAxisAlignedPillarBlock(Block block, TexturedModel.Provider modelProvider) {
        MultiVariant model = BlockModelGenerators.plainVariant(modelProvider.create(block, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block, model));
    }

    private void createHorizontallyRotatedBlock(Block block, TexturedModel.Provider modelProvider) {
        MultiVariant model = BlockModelGenerators.plainVariant(modelProvider.create(block, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, model).with(ROTATION_HORIZONTAL_FACING));
    }

    private static BlockModelDefinitionGenerator createRotatedPillarWithHorizontalVariant(Block block, MultiVariant model, MultiVariant horizontalModel) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.AXIS).select(Direction.Axis.Y, model).select(Direction.Axis.Z, horizontalModel.with(X_ROT_90)).select(Direction.Axis.X, horizontalModel.with(X_ROT_90).with(Y_ROT_90)));
    }

    private void createRotatedPillarWithHorizontalVariant(Block block, TexturedModel.Provider verticalProvider, TexturedModel.Provider horizontalProvider) {
        MultiVariant model = BlockModelGenerators.plainVariant(verticalProvider.create(block, this.modelOutput));
        MultiVariant horizontalModel = BlockModelGenerators.plainVariant(horizontalProvider.create(block, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(block, model, horizontalModel));
    }

    private void createCreakingHeart(Block block) {
        MultiVariant model = BlockModelGenerators.plainVariant(TexturedModel.COLUMN_ALT.create(block, this.modelOutput));
        MultiVariant horizontalModel = BlockModelGenerators.plainVariant(TexturedModel.COLUMN_HORIZONTAL_ALT.create(block, this.modelOutput));
        MultiVariant activeModel = BlockModelGenerators.plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_ALT, block, "_awake"));
        MultiVariant activeHorizontalModel = BlockModelGenerators.plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_HORIZONTAL_ALT, block, "_awake"));
        MultiVariant dormantModel = BlockModelGenerators.plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_ALT, block, "_dormant"));
        MultiVariant dormantHorizontalModel = BlockModelGenerators.plainVariant(this.createCreakingHeartModel(TexturedModel.COLUMN_HORIZONTAL_ALT, block, "_dormant"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.AXIS, CreakingHeartBlock.STATE).select(Direction.Axis.Y, CreakingHeartState.UPROOTED, model).select(Direction.Axis.Z, CreakingHeartState.UPROOTED, horizontalModel.with(X_ROT_90)).select(Direction.Axis.X, CreakingHeartState.UPROOTED, horizontalModel.with(X_ROT_90).with(Y_ROT_90)).select(Direction.Axis.Y, CreakingHeartState.DORMANT, dormantModel).select(Direction.Axis.Z, CreakingHeartState.DORMANT, dormantHorizontalModel.with(X_ROT_90)).select(Direction.Axis.X, CreakingHeartState.DORMANT, dormantHorizontalModel.with(X_ROT_90).with(Y_ROT_90)).select(Direction.Axis.Y, CreakingHeartState.AWAKE, activeModel).select(Direction.Axis.Z, CreakingHeartState.AWAKE, activeHorizontalModel.with(X_ROT_90)).select(Direction.Axis.X, CreakingHeartState.AWAKE, activeHorizontalModel.with(X_ROT_90).with(Y_ROT_90))));
    }

    private Identifier createCreakingHeartModel(TexturedModel.Provider provider, Block block, String suffix) {
        return provider.updateTexture(t -> t.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, suffix)).put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_top" + suffix))).createWithSuffix(block, suffix, this.modelOutput);
    }

    private Identifier createSuffixedVariant(Block block, String suffix, ModelTemplate template, Function<Material, TextureMapping> textureMapping) {
        return template.createWithSuffix(block, suffix, textureMapping.apply(TextureMapping.getBlockTexture(block, suffix)), this.modelOutput);
    }

    private static BlockModelDefinitionGenerator createPressurePlate(Block block, MultiVariant off, MultiVariant on) {
        return MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, on, off));
    }

    private static BlockModelDefinitionGenerator createSlab(Block block, MultiVariant bottom, MultiVariant top, MultiVariant full) {
        return MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.SLAB_TYPE).select(SlabType.BOTTOM, bottom).select(SlabType.TOP, top).select(SlabType.DOUBLE, full));
    }

    private void createTrivialCube(Block block) {
        this.createTrivialBlock(block, TexturedModel.CUBE);
    }

    private void createTrivialBlock(Block block, TexturedModel.Provider modelProvider) {
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(modelProvider.create(block, this.modelOutput))));
    }

    public void createTintedLeaves(Block block, TexturedModel.Provider modelProvider, int tintColor) {
        Identifier blockModel = modelProvider.create(block, this.modelOutput);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(blockModel)));
        this.registerSimpleTintedItemModel(block, blockModel, ItemModelUtils.constantTint(tintColor));
    }

    private void createVine() {
        this.createMultifaceBlockStates(Blocks.VINE);
        Identifier itemModel = this.createFlatItemModelWithBlockTexture(Items.VINE, Blocks.VINE);
        this.registerSimpleTintedItemModel(Blocks.VINE, itemModel, ItemModelUtils.constantTint(-12012264));
    }

    private void createItemWithGrassTint(Block block) {
        Identifier itemModel = this.createFlatItemModelWithBlockTexture(block.asItem(), block);
        this.registerSimpleTintedItemModel(block, itemModel, new GrassColorSource());
    }

    private BlockFamilyProvider family(Block block) {
        TexturedModel model = TEXTURED_MODELS.getOrDefault(block, TexturedModel.CUBE.get(block));
        return new BlockFamilyProvider(this, model.getMapping()).fullBlock(block, model.getTemplate());
    }

    private void createDoor(Block door) {
        TextureMapping mapping = TextureMapping.door(door);
        MultiVariant doorBottomLeft = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT.create(door, mapping, this.modelOutput));
        MultiVariant doorBottomLeftOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.create(door, mapping, this.modelOutput));
        MultiVariant doorBottomRight = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT.create(door, mapping, this.modelOutput));
        MultiVariant doorBottomRightOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.create(door, mapping, this.modelOutput));
        MultiVariant doorTopLeft = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_LEFT.create(door, mapping, this.modelOutput));
        MultiVariant doorTopLeftOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_LEFT_OPEN.create(door, mapping, this.modelOutput));
        MultiVariant doorTopRight = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_RIGHT.create(door, mapping, this.modelOutput));
        MultiVariant doorTopRightOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_RIGHT_OPEN.create(door, mapping, this.modelOutput));
        this.registerSimpleFlatItemModel(door.asItem());
        this.blockStateOutput.accept(BlockModelGenerators.createDoor(door, doorBottomLeft, doorBottomLeftOpen, doorBottomRight, doorBottomRightOpen, doorTopLeft, doorTopLeftOpen, doorTopRight, doorTopRightOpen));
    }

    private void copyDoorModel(Block donor, Block acceptor) {
        MultiVariant doorBottomLeft = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(donor));
        MultiVariant doorBottomLeftOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(donor));
        MultiVariant doorBottomRight = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(donor));
        MultiVariant doorBottomRightOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(donor));
        MultiVariant doorTopLeft = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(donor));
        MultiVariant doorTopLeftOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(donor));
        MultiVariant doorTopRight = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(donor));
        MultiVariant doorTopRightOpen = BlockModelGenerators.plainVariant(ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(donor));
        this.itemModelOutput.copy(donor.asItem(), acceptor.asItem());
        this.blockStateOutput.accept(BlockModelGenerators.createDoor(acceptor, doorBottomLeft, doorBottomLeftOpen, doorBottomRight, doorBottomRightOpen, doorTopLeft, doorTopLeftOpen, doorTopRight, doorTopRightOpen));
    }

    private void createOrientableTrapdoor(Block trapdoor) {
        TextureMapping mapping = TextureMapping.defaultTexture(trapdoor);
        MultiVariant top = BlockModelGenerators.plainVariant(ModelTemplates.ORIENTABLE_TRAPDOOR_TOP.create(trapdoor, mapping, this.modelOutput));
        Identifier bottom = ModelTemplates.ORIENTABLE_TRAPDOOR_BOTTOM.create(trapdoor, mapping, this.modelOutput);
        MultiVariant open = BlockModelGenerators.plainVariant(ModelTemplates.ORIENTABLE_TRAPDOOR_OPEN.create(trapdoor, mapping, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createOrientableTrapdoor(trapdoor, top, BlockModelGenerators.plainVariant(bottom), open));
        this.registerSimpleItemModel(trapdoor, bottom);
    }

    private void createTrapdoor(Block trapdoor) {
        TextureMapping mapping = TextureMapping.defaultTexture(trapdoor);
        MultiVariant top = BlockModelGenerators.plainVariant(ModelTemplates.TRAPDOOR_TOP.create(trapdoor, mapping, this.modelOutput));
        Identifier bottom = ModelTemplates.TRAPDOOR_BOTTOM.create(trapdoor, mapping, this.modelOutput);
        MultiVariant open = BlockModelGenerators.plainVariant(ModelTemplates.TRAPDOOR_OPEN.create(trapdoor, mapping, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createTrapdoor(trapdoor, top, BlockModelGenerators.plainVariant(bottom), open));
        this.registerSimpleItemModel(trapdoor, bottom);
    }

    private void copyTrapdoorModel(Block donor, Block acceptor) {
        MultiVariant top = BlockModelGenerators.plainVariant(ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(donor));
        MultiVariant bottom = BlockModelGenerators.plainVariant(ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(donor));
        MultiVariant open = BlockModelGenerators.plainVariant(ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(donor));
        this.itemModelOutput.copy(donor.asItem(), acceptor.asItem());
        this.blockStateOutput.accept(BlockModelGenerators.createTrapdoor(acceptor, top, bottom, open));
    }

    private void createBigDripLeafBlock() {
        MultiVariant noTilt = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF));
        MultiVariant partialTilt = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_partial_tilt"));
        MultiVariant fullTilt = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BIG_DRIPLEAF, "_full_tilt"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.BIG_DRIPLEAF).with(PropertyDispatch.initial(BlockStateProperties.TILT).select(Tilt.NONE, noTilt).select(Tilt.UNSTABLE, noTilt).select(Tilt.PARTIAL, partialTilt).select(Tilt.FULL, fullTilt)).with(ROTATION_HORIZONTAL_FACING));
    }

    private WoodProvider woodProvider(Block log) {
        return new WoodProvider(this, TextureMapping.logColumn(log));
    }

    private void createNonTemplateModelBlock(Block block) {
        this.createNonTemplateModelBlock(block, block);
    }

    private void createNonTemplateModelBlock(Block block, Block donor) {
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor))));
    }

    private void createCrossBlockWithDefaultItem(Block block, PlantType plantType) {
        this.registerSimpleItemModel(block.asItem(), plantType.createItemModel(this, block));
        this.createCrossBlock(block, plantType);
    }

    private void createCrossBlockWithDefaultItem(Block block, PlantType plantType, TextureMapping textures) {
        this.registerSimpleFlatItemModel(block);
        this.createCrossBlock(block, plantType, textures);
    }

    private void createCrossBlock(Block block, PlantType plantType) {
        TextureMapping textures = plantType.getTextureMapping(block);
        this.createCrossBlock(block, plantType, textures);
    }

    private void createCrossBlock(Block block, PlantType plantType, TextureMapping textures) {
        MultiVariant model = BlockModelGenerators.plainVariant(plantType.getCross().create(block, textures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, model));
    }

    private void createCrossBlock(Block block, PlantType plantType, Property<Integer> property, int ... stages) {
        if (property.getPossibleValues().size() != stages.length) {
            throw new IllegalArgumentException("missing values for property: " + String.valueOf(property));
        }
        this.registerSimpleFlatItemModel(block.asItem());
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(property).generate(i -> {
            String suffix = "_stage" + stages[i];
            TextureMapping texture = TextureMapping.cross(TextureMapping.getBlockTexture(block, suffix));
            return BlockModelGenerators.plainVariant(plantType.getCross().createWithSuffix(block, suffix, texture, this.modelOutput));
        })));
    }

    private void createPlantWithDefaultItem(Block standAlone, Block potted, PlantType plantType) {
        this.registerSimpleItemModel(standAlone.asItem(), plantType.createItemModel(this, standAlone));
        this.createPlant(standAlone, potted, plantType);
    }

    private void createPlant(Block standAlone, Block potted, PlantType plantType) {
        this.createCrossBlock(standAlone, plantType);
        TextureMapping textures = plantType.getPlantTextureMapping(standAlone);
        MultiVariant model = BlockModelGenerators.plainVariant(plantType.getCrossPot().create(potted, textures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(potted, model));
    }

    private void createCoralFans(Block fan, Block wallFan) {
        TexturedModel fanTemplate = TexturedModel.CORAL_FAN.get(fan);
        MultiVariant fanModel = BlockModelGenerators.plainVariant(fanTemplate.create(fan, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(fan, fanModel));
        MultiVariant wallFanModel = BlockModelGenerators.plainVariant(ModelTemplates.CORAL_WALL_FAN.create(wallFan, fanTemplate.getMapping(), this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(wallFan, wallFanModel).with(ROTATION_HORIZONTAL_FACING));
        this.registerSimpleFlatItemModel(fan);
    }

    private void createStems(Block growingStem, Block attachedStem) {
        this.registerSimpleFlatItemModel(growingStem.asItem());
        TextureMapping growingMapping = TextureMapping.stem(growingStem);
        TextureMapping attachedMapping = TextureMapping.attachedStem(growingStem, attachedStem);
        MultiVariant attachedStemModel = BlockModelGenerators.plainVariant(ModelTemplates.ATTACHED_STEM.create(attachedStem, attachedMapping, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(attachedStem, attachedStemModel).with(PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING).select(Direction.WEST, NOP).select(Direction.SOUTH, Y_ROT_270).select(Direction.NORTH, Y_ROT_90).select(Direction.EAST, Y_ROT_180)));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(growingStem).with(PropertyDispatch.initial(BlockStateProperties.AGE_7).generate(i -> BlockModelGenerators.plainVariant(ModelTemplates.STEMS[i].create(growingStem, growingMapping, this.modelOutput)))));
    }

    private void createPitcherPlant() {
        Block block = Blocks.PITCHER_PLANT;
        this.registerSimpleFlatItemModel(block.asItem());
        MultiVariant topModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_top"));
        MultiVariant bottomModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_bottom"));
        this.createDoubleBlock(block, topModel, bottomModel);
    }

    private void createPitcherCrop() {
        Block block = Blocks.PITCHER_CROP;
        this.registerSimpleFlatItemModel(block.asItem());
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(PitcherCropBlock.AGE, BlockStateProperties.DOUBLE_BLOCK_HALF).generate((age, shape) -> switch (shape) {
            default -> throw new MatchException(null, null);
            case DoubleBlockHalf.UPPER -> BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_top_stage_" + age));
            case DoubleBlockHalf.LOWER -> BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_bottom_stage_" + age));
        })));
    }

    private void createCoral(Block plant, Block deadPlant, Block block, Block deadBlock, Block fan, Block deadFan, Block wallFan, Block deadWallFan) {
        this.createCrossBlockWithDefaultItem(plant, PlantType.NOT_TINTED);
        this.createCrossBlockWithDefaultItem(deadPlant, PlantType.NOT_TINTED);
        this.createTrivialCube(block);
        this.createTrivialCube(deadBlock);
        this.createCoralFans(fan, wallFan);
        this.createCoralFans(deadFan, deadWallFan);
    }

    private void createDoublePlant(Block block, PlantType plantType) {
        MultiVariant topModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_top", plantType.getCross(), TextureMapping::cross));
        MultiVariant bottomModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_bottom", plantType.getCross(), TextureMapping::cross));
        this.createDoubleBlock(block, topModel, bottomModel);
    }

    private void createDoublePlantWithDefaultItem(Block block, PlantType plantType) {
        this.registerSimpleFlatItemModel(block, "_top");
        this.createDoublePlant(block, plantType);
    }

    private void createTintedDoublePlant(Block block) {
        Identifier itemModel = this.createFlatItemModelWithBlockTexture(block.asItem(), block, "_top");
        this.registerSimpleTintedItemModel(block, itemModel, new GrassColorSource());
        this.createDoublePlant(block, PlantType.TINTED);
    }

    private void createSunflower() {
        this.registerSimpleFlatItemModel(Blocks.SUNFLOWER, "_front");
        MultiVariant topModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SUNFLOWER, "_top"));
        MultiVariant bottomModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.SUNFLOWER, "_bottom", PlantType.NOT_TINTED.getCross(), TextureMapping::cross));
        this.createDoubleBlock(Blocks.SUNFLOWER, topModel, bottomModel);
    }

    private void createTallSeagrass() {
        MultiVariant topModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_top", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture));
        MultiVariant bottomModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.TALL_SEAGRASS, "_bottom", ModelTemplates.SEAGRASS, TextureMapping::defaultTexture));
        this.createDoubleBlock(Blocks.TALL_SEAGRASS, topModel, bottomModel);
    }

    private void createSmallDripleaf() {
        MultiVariant topModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_top"));
        MultiVariant bottomModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SMALL_DRIPLEAF, "_bottom"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SMALL_DRIPLEAF).with(PropertyDispatch.initial(BlockStateProperties.DOUBLE_BLOCK_HALF).select(DoubleBlockHalf.LOWER, bottomModel).select(DoubleBlockHalf.UPPER, topModel)).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createDoubleBlock(Block block, MultiVariant topModel, MultiVariant bottomModel) {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.DOUBLE_BLOCK_HALF).select(DoubleBlockHalf.LOWER, bottomModel).select(DoubleBlockHalf.UPPER, topModel)));
    }

    private void createPassiveRail(Block block) {
        TextureMapping texture = TextureMapping.rail(block);
        TextureMapping cornerTexture = TextureMapping.rail(TextureMapping.getBlockTexture(block, "_corner"));
        MultiVariant flat = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_FLAT.create(block, texture, this.modelOutput));
        MultiVariant curved = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_CURVED.create(block, cornerTexture, this.modelOutput));
        MultiVariant risingNE = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_NE.create(block, texture, this.modelOutput));
        MultiVariant risingSW = BlockModelGenerators.plainVariant(ModelTemplates.RAIL_RAISED_SW.create(block, texture, this.modelOutput));
        this.registerSimpleFlatItemModel(block);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.RAIL_SHAPE).select(RailShape.NORTH_SOUTH, flat).select(RailShape.EAST_WEST, flat.with(Y_ROT_90)).select(RailShape.ASCENDING_EAST, risingNE.with(Y_ROT_90)).select(RailShape.ASCENDING_WEST, risingSW.with(Y_ROT_90)).select(RailShape.ASCENDING_NORTH, risingNE).select(RailShape.ASCENDING_SOUTH, risingSW).select(RailShape.SOUTH_EAST, curved).select(RailShape.SOUTH_WEST, curved.with(Y_ROT_90)).select(RailShape.NORTH_WEST, curved.with(Y_ROT_180)).select(RailShape.NORTH_EAST, curved.with(Y_ROT_270))));
    }

    private void createActiveRail(Block block) {
        MultiVariant flat = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "", ModelTemplates.RAIL_FLAT, TextureMapping::rail));
        MultiVariant risingNE = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail));
        MultiVariant risingSW = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail));
        MultiVariant flatOn = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_on", ModelTemplates.RAIL_FLAT, TextureMapping::rail));
        MultiVariant risingNEOn = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_on", ModelTemplates.RAIL_RAISED_NE, TextureMapping::rail));
        MultiVariant risingSWOn = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_on", ModelTemplates.RAIL_RAISED_SW, TextureMapping::rail));
        this.registerSimpleFlatItemModel(block);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.POWERED, BlockStateProperties.RAIL_SHAPE_STRAIGHT).generate((powered, railShape) -> switch (railShape) {
            case RailShape.NORTH_SOUTH -> {
                if (powered.booleanValue()) {
                    yield flatOn;
                }
                yield flat;
            }
            case RailShape.EAST_WEST -> (powered != false ? flatOn : flat).with(Y_ROT_90);
            case RailShape.ASCENDING_EAST -> (powered != false ? risingNEOn : risingNE).with(Y_ROT_90);
            case RailShape.ASCENDING_WEST -> (powered != false ? risingSWOn : risingSW).with(Y_ROT_90);
            case RailShape.ASCENDING_NORTH -> {
                if (powered.booleanValue()) {
                    yield risingNEOn;
                }
                yield risingNE;
            }
            case RailShape.ASCENDING_SOUTH -> {
                if (powered.booleanValue()) {
                    yield risingSWOn;
                }
                yield risingSW;
            }
            default -> throw new UnsupportedOperationException("Fix you generator!");
        })));
    }

    private void createAirLikeBlock(Block block, Item particleItem) {
        MultiVariant dummyModel = BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particleFromItem(particleItem), this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, dummyModel));
    }

    private void createAirLikeBlock(Block block, Material particle) {
        MultiVariant dummyModel = BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particle), this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, dummyModel));
    }

    private MultiVariant createParticleOnlyBlockModel(Block block, Block particleDonor) {
        return BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particleDonor), this.modelOutput));
    }

    public void createParticleOnlyBlock(Block block, Block particleDonor) {
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, this.createParticleOnlyBlockModel(block, particleDonor)));
    }

    private void createParticleOnlyBlock(Block block) {
        this.createParticleOnlyBlock(block, block);
    }

    private void createFullAndCarpetBlocks(Block block, Block carpet) {
        this.createTrivialCube(block);
        MultiVariant model = BlockModelGenerators.plainVariant(TexturedModel.CARPET.get(block).create(carpet, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(carpet, model));
    }

    private void createLeafLitter(Block block) {
        MultiVariant model1 = BlockModelGenerators.plainVariant(TexturedModel.LEAF_LITTER_1.create(block, this.modelOutput));
        MultiVariant model2 = BlockModelGenerators.plainVariant(TexturedModel.LEAF_LITTER_2.create(block, this.modelOutput));
        MultiVariant model3 = BlockModelGenerators.plainVariant(TexturedModel.LEAF_LITTER_3.create(block, this.modelOutput));
        MultiVariant model4 = BlockModelGenerators.plainVariant(TexturedModel.LEAF_LITTER_4.create(block, this.modelOutput));
        this.registerSimpleFlatItemModel(block.asItem());
        this.createSegmentedBlock(block, model1, LEAF_LITTER_MODEL_1_SEGMENT_CONDITION, model2, LEAF_LITTER_MODEL_2_SEGMENT_CONDITION, model3, LEAF_LITTER_MODEL_3_SEGMENT_CONDITION, model4, LEAF_LITTER_MODEL_4_SEGMENT_CONDITION);
    }

    private void createFlowerBed(Block flowerbed) {
        MultiVariant model1 = BlockModelGenerators.plainVariant(TexturedModel.FLOWERBED_1.create(flowerbed, this.modelOutput));
        MultiVariant model2 = BlockModelGenerators.plainVariant(TexturedModel.FLOWERBED_2.create(flowerbed, this.modelOutput));
        MultiVariant model3 = BlockModelGenerators.plainVariant(TexturedModel.FLOWERBED_3.create(flowerbed, this.modelOutput));
        MultiVariant model4 = BlockModelGenerators.plainVariant(TexturedModel.FLOWERBED_4.create(flowerbed, this.modelOutput));
        this.registerSimpleFlatItemModel(flowerbed.asItem());
        this.createSegmentedBlock(flowerbed, model1, FLOWER_BED_MODEL_1_SEGMENT_CONDITION, model2, FLOWER_BED_MODEL_2_SEGMENT_CONDITION, model3, FLOWER_BED_MODEL_3_SEGMENT_CONDITION, model4, FLOWER_BED_MODEL_4_SEGMENT_CONDITION);
    }

    private void createSegmentedBlock(Block segmentedProperty, MultiVariant model1, Function<ConditionBuilder, ConditionBuilder> model1SegmentCondition, MultiVariant model2, Function<ConditionBuilder, ConditionBuilder> model2SegmentCondition, MultiVariant model3, Function<ConditionBuilder, ConditionBuilder> model3SegmentCondition, MultiVariant model4, Function<ConditionBuilder, ConditionBuilder> model4SegmentCondition) {
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(segmentedProperty).with(model1SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), model1).with(model1SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), model1.with(Y_ROT_90)).with(model1SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), model1.with(Y_ROT_180)).with(model1SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), model1.with(Y_ROT_270)).with(model2SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), model2).with(model2SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), model2.with(Y_ROT_90)).with(model2SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), model2.with(Y_ROT_180)).with(model2SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), model2.with(Y_ROT_270)).with(model3SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), model3).with(model3SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), model3.with(Y_ROT_90)).with(model3SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), model3.with(Y_ROT_180)).with(model3SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), model3.with(Y_ROT_270)).with(model4SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)), model4).with(model4SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), model4.with(Y_ROT_90)).with(model4SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)), model4.with(Y_ROT_180)).with(model4SegmentCondition.apply(BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)), model4.with(Y_ROT_270)));
    }

    private void createColoredBlockWithRandomRotations(TexturedModel.Provider modelProvider, List<Block> blocks) {
        for (Block block : blocks) {
            Variant model = BlockModelGenerators.plainModel(modelProvider.create(block, this.modelOutput));
            this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.createRotatedVariants(model)));
        }
    }

    private void createColoredBlockWithStateRotations(TexturedModel.Provider modelProvider, List<Block> blocks) {
        for (Block block : blocks) {
            MultiVariant model = BlockModelGenerators.plainVariant(modelProvider.create(block, this.modelOutput));
            this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, model).with(ROTATION_HORIZONTAL_FACING_ALT));
        }
    }

    private void createGlassBlocks(Block block, Block pane) {
        this.createTrivialBlock(block, TexturedModel.CUBE.updateTexture(mapping -> mapping.forceAllTranslucent()));
        TextureMapping paneMapping = TextureMapping.pane(block, pane).forceAllTranslucent();
        MultiVariant post = BlockModelGenerators.plainVariant(ModelTemplates.STAINED_GLASS_PANE_POST.create(pane, paneMapping, this.modelOutput));
        MultiVariant side = BlockModelGenerators.plainVariant(ModelTemplates.STAINED_GLASS_PANE_SIDE.create(pane, paneMapping, this.modelOutput));
        MultiVariant sideAlt = BlockModelGenerators.plainVariant(ModelTemplates.STAINED_GLASS_PANE_SIDE_ALT.create(pane, paneMapping, this.modelOutput));
        MultiVariant noSide = BlockModelGenerators.plainVariant(ModelTemplates.STAINED_GLASS_PANE_NOSIDE.create(pane, paneMapping, this.modelOutput));
        MultiVariant noSideAlt = BlockModelGenerators.plainVariant(ModelTemplates.STAINED_GLASS_PANE_NOSIDE_ALT.create(pane, paneMapping, this.modelOutput));
        Item paneItem = pane.asItem();
        this.registerSimpleItemModel(paneItem, this.createFlatItemModelWithBlockTexture(paneItem, block));
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(pane).with(post).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), side).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), side.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), sideAlt).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), sideAlt.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), noSide).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, false), noSideAlt).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), noSideAlt.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, false), noSide.with(Y_ROT_270)));
    }

    private void createCommandBlock(Block block) {
        TextureMapping normalTextures = TextureMapping.commandBlock(block);
        MultiVariant normalModel = BlockModelGenerators.plainVariant(ModelTemplates.COMMAND_BLOCK.create(block, normalTextures, this.modelOutput));
        MultiVariant conditionalModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(block, "_conditional", ModelTemplates.COMMAND_BLOCK, id -> normalTextures.copyAndUpdate(TextureSlot.SIDE, (Material)id)));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.CONDITIONAL, conditionalModel, normalModel)).with(ROTATION_FACING));
    }

    private void createAnvil(Block block) {
        MultiVariant anvilModel = BlockModelGenerators.plainVariant(TexturedModel.ANVIL.create(block, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, anvilModel).with(ROTATION_HORIZONTAL_FACING_ALT));
    }

    private static MultiVariant createBambooModels(int age) {
        String ageSuffix = "_age" + age;
        return new MultiVariant(WeightedList.of(IntStream.range(1, 5).mapToObj(i -> new Weighted<Variant>(BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, i + ageSuffix)), 1)).collect(Collectors.toList())));
    }

    private void createBamboo() {
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.BAMBOO).with(BlockModelGenerators.condition().term(BlockStateProperties.AGE_1, 0), BlockModelGenerators.createBambooModels(0)).with(BlockModelGenerators.condition().term(BlockStateProperties.AGE_1, 1), BlockModelGenerators.createBambooModels(1)).with(BlockModelGenerators.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.SMALL), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_small_leaves"))).with(BlockModelGenerators.condition().term(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.LARGE), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BAMBOO, "_large_leaves"))));
    }

    private void createBarrel() {
        Material openTop = TextureMapping.getBlockTexture(Blocks.BARREL, "_top_open");
        MultiVariant closedModel = BlockModelGenerators.plainVariant(TexturedModel.CUBE_TOP_BOTTOM.create(Blocks.BARREL, this.modelOutput));
        MultiVariant openModel = BlockModelGenerators.plainVariant(TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.BARREL).updateTextures(t -> t.put(TextureSlot.TOP, openTop)).createWithSuffix(Blocks.BARREL, "_open", this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.BARREL).with(PropertyDispatch.initial(BlockStateProperties.OPEN).select(false, closedModel).select(true, openModel)).with(ROTATIONS_COLUMN_WITH_FACING));
    }

    private static <T extends Comparable<T>> PropertyDispatch<MultiVariant> createEmptyOrFullDispatch(Property<T> property, T threshold, MultiVariant fullModel, MultiVariant emptyModel) {
        return PropertyDispatch.initial(property).generate(value -> {
            boolean isFull = value.compareTo(threshold) >= 0;
            return isFull ? fullModel : emptyModel;
        });
    }

    private void createBeeNest(Block block, Function<Block, TextureMapping> mappingFunction) {
        TextureMapping emptyMapping = mappingFunction.apply(block).copyForced(TextureSlot.SIDE, TextureSlot.PARTICLE);
        TextureMapping fullMapping = emptyMapping.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_honey"));
        Identifier emptyModel = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(block, "_empty", emptyMapping, this.modelOutput);
        Identifier fullModel = ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM.createWithSuffix(block, "_honey", fullMapping, this.modelOutput);
        this.itemModelOutput.accept(block.asItem(), ItemModelUtils.selectBlockItemProperty(BeehiveBlock.HONEY_LEVEL, ItemModelUtils.plainModel(emptyModel), Map.of(5, ItemModelUtils.plainModel(fullModel))));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createEmptyOrFullDispatch(BeehiveBlock.HONEY_LEVEL, 5, BlockModelGenerators.plainVariant(fullModel), BlockModelGenerators.plainVariant(emptyModel))).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createCropBlock(Block block, Property<Integer> property, int ... stages) {
        this.registerSimpleFlatItemModel(block.asItem());
        if (property.getPossibleValues().size() != stages.length) {
            throw new IllegalArgumentException();
        }
        Int2ObjectOpenHashMap models = new Int2ObjectOpenHashMap();
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(property).generate(arg_0 -> this.lambda$createCropBlock$0(stages, (Int2ObjectMap)models, block, arg_0))));
    }

    private void createBell() {
        MultiVariant floor = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_floor"));
        MultiVariant ceiling = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_ceiling"));
        MultiVariant wall = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_wall"));
        MultiVariant betweenWalls = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BELL, "_between_walls"));
        this.registerSimpleFlatItemModel(Items.BELL);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.BELL).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BELL_ATTACHMENT).select(Direction.NORTH, BellAttachType.FLOOR, floor).select(Direction.SOUTH, BellAttachType.FLOOR, floor.with(Y_ROT_180)).select(Direction.EAST, BellAttachType.FLOOR, floor.with(Y_ROT_90)).select(Direction.WEST, BellAttachType.FLOOR, floor.with(Y_ROT_270)).select(Direction.NORTH, BellAttachType.CEILING, ceiling).select(Direction.SOUTH, BellAttachType.CEILING, ceiling.with(Y_ROT_180)).select(Direction.EAST, BellAttachType.CEILING, ceiling.with(Y_ROT_90)).select(Direction.WEST, BellAttachType.CEILING, ceiling.with(Y_ROT_270)).select(Direction.NORTH, BellAttachType.SINGLE_WALL, wall.with(Y_ROT_270)).select(Direction.SOUTH, BellAttachType.SINGLE_WALL, wall.with(Y_ROT_90)).select(Direction.EAST, BellAttachType.SINGLE_WALL, wall).select(Direction.WEST, BellAttachType.SINGLE_WALL, wall.with(Y_ROT_180)).select(Direction.SOUTH, BellAttachType.DOUBLE_WALL, betweenWalls.with(Y_ROT_90)).select(Direction.NORTH, BellAttachType.DOUBLE_WALL, betweenWalls.with(Y_ROT_270)).select(Direction.EAST, BellAttachType.DOUBLE_WALL, betweenWalls).select(Direction.WEST, BellAttachType.DOUBLE_WALL, betweenWalls.with(Y_ROT_180))));
    }

    private void createGrindstone() {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.GRINDSTONE, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.GRINDSTONE))).with(PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING).select(AttachFace.FLOOR, Direction.NORTH, NOP).select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90).select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180).select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270).select(AttachFace.WALL, Direction.NORTH, X_ROT_90).select(AttachFace.WALL, Direction.EAST, X_ROT_90.then(Y_ROT_90)).select(AttachFace.WALL, Direction.SOUTH, X_ROT_90.then(Y_ROT_180)).select(AttachFace.WALL, Direction.WEST, X_ROT_90.then(Y_ROT_270)).select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180).select(AttachFace.CEILING, Direction.WEST, X_ROT_180.then(Y_ROT_90)).select(AttachFace.CEILING, Direction.NORTH, X_ROT_180.then(Y_ROT_180)).select(AttachFace.CEILING, Direction.EAST, X_ROT_180.then(Y_ROT_270))));
    }

    private void createFurnace(Block furnace, TexturedModel.Provider provider) {
        MultiVariant normalModel = BlockModelGenerators.plainVariant(provider.create(furnace, this.modelOutput));
        Material frontTexture = TextureMapping.getBlockTexture(furnace, "_front_on");
        MultiVariant litModel = BlockModelGenerators.plainVariant(provider.get(furnace).updateTextures(t -> t.put(TextureSlot.FRONT, frontTexture)).createWithSuffix(furnace, "_on", this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(furnace).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, litModel, normalModel)).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createCampfires(Block ... campFires) {
        MultiVariant offModel = BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("campfire_off"));
        for (Block campFire : campFires) {
            MultiVariant litModel = BlockModelGenerators.plainVariant(ModelTemplates.CAMPFIRE.create(campFire, TextureMapping.campfire(campFire), this.modelOutput));
            this.registerSimpleFlatItemModel(campFire.asItem());
            this.blockStateOutput.accept(MultiVariantGenerator.dispatch(campFire).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, litModel, offModel)).with(ROTATION_HORIZONTAL_FACING_ALT));
        }
    }

    private void createAzalea(Block block) {
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.AZALEA.create(block, TextureMapping.cubeTop(block), this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, model));
    }

    private void createPottedAzalea(Block block) {
        MultiVariant model = block == Blocks.POTTED_FLOWERING_AZALEA ? BlockModelGenerators.plainVariant(ModelTemplates.POTTED_FLOWERING_AZALEA.create(block, TextureMapping.pottedAzalea(block), this.modelOutput)) : BlockModelGenerators.plainVariant(ModelTemplates.POTTED_AZALEA.create(block, TextureMapping.pottedAzalea(block), this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, model));
    }

    private void createBookshelf() {
        TextureMapping textures = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.BOOKSHELF), TextureMapping.getBlockTexture(Blocks.OAK_PLANKS));
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.create(Blocks.BOOKSHELF, textures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.BOOKSHELF, model));
    }

    private void createRedstoneWire() {
        this.registerSimpleFlatItemModel(Items.REDSTONE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.REDSTONE_WIRE).with(BlockModelGenerators.or(BlockModelGenerators.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.NONE).term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.NONE).term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.NONE).term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.NONE), BlockModelGenerators.condition().term(BlockStateProperties.NORTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}).term(BlockStateProperties.EAST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.condition().term(BlockStateProperties.EAST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}).term(BlockStateProperties.SOUTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.condition().term(BlockStateProperties.SOUTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}).term(BlockStateProperties.WEST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.condition().term(BlockStateProperties.WEST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}).term(BlockStateProperties.NORTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP})), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_dot"))).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side0"))).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt0"))).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side_alt1")).with(Y_ROT_270)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST_REDSTONE, (Comparable)((Object)RedstoneSide.SIDE), (Comparable[])new RedstoneSide[]{RedstoneSide.UP}), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_side1")).with(Y_ROT_270)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH_REDSTONE, RedstoneSide.UP), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up"))).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST_REDSTONE, RedstoneSide.UP), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH_REDSTONE, RedstoneSide.UP), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_180)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST_REDSTONE, RedstoneSide.UP), BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("redstone_dust_up")).with(Y_ROT_270)));
    }

    private void createComparator() {
        this.registerSimpleFlatItemModel(Items.COMPARATOR);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.COMPARATOR).with(PropertyDispatch.initial(BlockStateProperties.MODE_COMPARATOR, BlockStateProperties.POWERED).select(ComparatorMode.COMPARE, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR))).select(ComparatorMode.COMPARE, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on"))).select(ComparatorMode.SUBTRACT, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_subtract"))).select(ComparatorMode.SUBTRACT, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPARATOR, "_on_subtract")))).with(ROTATION_HORIZONTAL_FACING_ALT));
    }

    private void createSmoothStoneSlab() {
        TextureMapping smoothStoneTextures = TextureMapping.cube(Blocks.SMOOTH_STONE);
        TextureMapping smoothStoneSlabTextures = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.SMOOTH_STONE_SLAB, "_side"), smoothStoneTextures.get(TextureSlot.TOP));
        MultiVariant bottom = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_BOTTOM.create(Blocks.SMOOTH_STONE_SLAB, smoothStoneSlabTextures, this.modelOutput));
        MultiVariant top = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_TOP.create(Blocks.SMOOTH_STONE_SLAB, smoothStoneSlabTextures, this.modelOutput));
        MultiVariant doubleSlab = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.createWithOverride(Blocks.SMOOTH_STONE_SLAB, "_double", smoothStoneSlabTextures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSlab(Blocks.SMOOTH_STONE_SLAB, bottom, top, doubleSlab));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.SMOOTH_STONE, BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(Blocks.SMOOTH_STONE, smoothStoneTextures, this.modelOutput))));
    }

    private void createBrewingStand() {
        this.registerSimpleFlatItemModel(Items.BREWING_STAND);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.BREWING_STAND).with(BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_0, true), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_bottle0"))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_1, true), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_bottle1"))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_2, true), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_bottle2"))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_0, false), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_empty0"))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_1, false), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_empty1"))).with(BlockModelGenerators.condition().term(BlockStateProperties.HAS_BOTTLE_2, false), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.BREWING_STAND, "_empty2"))));
    }

    private void createMushroomBlock(Block block) {
        MultiVariant skin = BlockModelGenerators.plainVariant(ModelTemplates.SINGLE_FACE.create(block, TextureMapping.defaultTexture(block), this.modelOutput));
        MultiVariant skinless = BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside"));
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(block).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), skin).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), skin.with(Y_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), skin.with(Y_ROT_180).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), skin.with(Y_ROT_270).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), skin.with(X_ROT_270).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, true), skin.with(X_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), skinless).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, false), skinless.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), skinless.with(Y_ROT_180)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, false), skinless.with(Y_ROT_270)).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, false), skinless.with(X_ROT_270)).with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, false), skinless.with(X_ROT_90)));
        this.registerSimpleItemModel(block, TexturedModel.CUBE.createWithSuffix(block, "_inventory", this.modelOutput));
    }

    private void createCakeBlock() {
        this.registerSimpleFlatItemModel(Items.CAKE);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CAKE).with(PropertyDispatch.initial(BlockStateProperties.BITES).select(0, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE))).select(1, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice1"))).select(2, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice2"))).select(3, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice3"))).select(4, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice4"))).select(5, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice5"))).select(6, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CAKE, "_slice6")))));
    }

    private void createCartographyTable() {
        TextureMapping mapping = new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.DARK_OAK_PLANKS)).put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side3")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side1")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.CARTOGRAPHY_TABLE, "_side2"));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.CARTOGRAPHY_TABLE, BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(Blocks.CARTOGRAPHY_TABLE, mapping, this.modelOutput))));
    }

    private void createSmithingTable() {
        TextureMapping mapping = new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_bottom")).put(TextureSlot.UP, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_front")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SMITHING_TABLE, "_side"));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.SMITHING_TABLE, BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(Blocks.SMITHING_TABLE, mapping, this.modelOutput))));
    }

    private void createCraftingTableLike(Block block, Block bottomBlock, BiFunction<Block, Block, TextureMapping> mappingProvider) {
        TextureMapping mapping = mappingProvider.apply(block, bottomBlock);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(ModelTemplates.CUBE.create(block, mapping, this.modelOutput))));
    }

    private void createPumpkins() {
        TextureMapping pumpkinTextures = TextureMapping.column(Blocks.PUMPKIN);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.PUMPKIN, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PUMPKIN))));
        this.createPumpkinVariant(Blocks.CARVED_PUMPKIN, pumpkinTextures);
        this.createPumpkinVariant(Blocks.JACK_O_LANTERN, pumpkinTextures);
    }

    private void createPumpkinVariant(Block block, TextureMapping textures) {
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(block, textures.copyAndUpdate(TextureSlot.FRONT, TextureMapping.getBlockTexture(block)), this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, model).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createCauldrons() {
        this.registerSimpleFlatItemModel(Items.CAULDRON);
        this.createNonTemplateModelBlock(Blocks.CAULDRON);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.LAVA_CAULDRON, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_FULL.create(Blocks.LAVA_CAULDRON, TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.LAVA, "_still")), this.modelOutput))));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.WATER_CAULDRON).with(PropertyDispatch.initial(LayeredCauldronBlock.LEVEL).select(1, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_LEVEL1.createWithSuffix(Blocks.WATER_CAULDRON, "_level1", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput))).select(2, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_LEVEL2.createWithSuffix(Blocks.WATER_CAULDRON, "_level2", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput))).select(3, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_FULL.createWithSuffix(Blocks.WATER_CAULDRON, "_full", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.WATER, "_still")), this.modelOutput)))));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.POWDER_SNOW_CAULDRON).with(PropertyDispatch.initial(LayeredCauldronBlock.LEVEL).select(1, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_LEVEL1.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_level1", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput))).select(2, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_LEVEL2.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_level2", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput))).select(3, BlockModelGenerators.plainVariant(ModelTemplates.CAULDRON_FULL.createWithSuffix(Blocks.POWDER_SNOW_CAULDRON, "_full", TextureMapping.cauldron(TextureMapping.getBlockTexture(Blocks.POWDER_SNOW)), this.modelOutput)))));
    }

    private void createChorusFlower() {
        TextureMapping aliveTextures = TextureMapping.defaultTexture(Blocks.CHORUS_FLOWER);
        MultiVariant aliveModel = BlockModelGenerators.plainVariant(ModelTemplates.CHORUS_FLOWER.create(Blocks.CHORUS_FLOWER, aliveTextures, this.modelOutput));
        MultiVariant deadModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.CHORUS_FLOWER, "_dead", ModelTemplates.CHORUS_FLOWER, id -> aliveTextures.copyAndUpdate(TextureSlot.TEXTURE, (Material)id)));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CHORUS_FLOWER).with(BlockModelGenerators.createEmptyOrFullDispatch(BlockStateProperties.AGE_5, 5, deadModel, aliveModel)));
    }

    private void createCrafterBlock() {
        MultiVariant off = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER));
        MultiVariant triggeredLocation = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_triggered"));
        MultiVariant craftingLocation = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting"));
        MultiVariant craftingTriggeredLocation = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CRAFTER, "_crafting_triggered"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CRAFTER).with(PropertyDispatch.initial(BlockStateProperties.TRIGGERED, CrafterBlock.CRAFTING).select(false, false, off).select(true, true, craftingTriggeredLocation).select(true, false, triggeredLocation).select(false, true, craftingLocation)).with(PropertyDispatch.modify(BlockStateProperties.ORIENTATION).generate(BlockModelGenerators::applyRotation)));
    }

    private void createDispenserBlock(Block block) {
        TextureMapping horizontalTextures = new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front"));
        TextureMapping verticalTextures = new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.FURNACE, "_top")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_vertical"));
        MultiVariant horizontalModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(block, horizontalTextures, this.modelOutput));
        MultiVariant verticalModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(block, verticalTextures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.FACING).select(Direction.DOWN, verticalModel.with(X_ROT_180)).select(Direction.UP, verticalModel).select(Direction.NORTH, horizontalModel).select(Direction.EAST, horizontalModel.with(Y_ROT_90)).select(Direction.SOUTH, horizontalModel.with(Y_ROT_180)).select(Direction.WEST, horizontalModel.with(Y_ROT_270))));
    }

    private void createEndPortalFrame() {
        MultiVariant empty = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME));
        MultiVariant filled = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.END_PORTAL_FRAME, "_filled"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.END_PORTAL_FRAME).with(PropertyDispatch.initial(BlockStateProperties.EYE).select(false, empty).select(true, filled)).with(ROTATION_HORIZONTAL_FACING_ALT));
    }

    private void createChorusPlant() {
        MultiVariant side = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_side"));
        Variant noside = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside"));
        Variant noside1 = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside1"));
        Variant noside2 = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside2"));
        Variant noside3 = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.CHORUS_PLANT, "_noside3"));
        Variant nosideUvLock = noside.with(UV_LOCK);
        Variant noside1uvLock = noside1.with(UV_LOCK);
        Variant noside2uvLock = noside2.with(UV_LOCK);
        Variant noside3uvLock = noside3.with(UV_LOCK);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.CHORUS_PLANT).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), side).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), side.with(Y_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), side.with(Y_ROT_180).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), side.with(Y_ROT_270).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), side.with(X_ROT_270).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, true), side.with(X_ROT_90).with(UV_LOCK)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(noside, 2), new Weighted<Variant>(noside1, 1), new Weighted<Variant>(noside2, 1), new Weighted<Variant>(noside3, 1)))).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(noside1uvLock.with(Y_ROT_90), 1), new Weighted<Variant>(noside2uvLock.with(Y_ROT_90), 1), new Weighted<Variant>(noside3uvLock.with(Y_ROT_90), 1), new Weighted<Variant>(nosideUvLock.with(Y_ROT_90), 2)))).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(noside2uvLock.with(Y_ROT_180), 1), new Weighted<Variant>(noside3uvLock.with(Y_ROT_180), 1), new Weighted<Variant>(nosideUvLock.with(Y_ROT_180), 2), new Weighted<Variant>(noside1uvLock.with(Y_ROT_180), 1)))).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(noside3uvLock.with(Y_ROT_270), 1), new Weighted<Variant>(nosideUvLock.with(Y_ROT_270), 2), new Weighted<Variant>(noside1uvLock.with(Y_ROT_270), 1), new Weighted<Variant>(noside2uvLock.with(Y_ROT_270), 1)))).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(nosideUvLock.with(X_ROT_270), 2), new Weighted<Variant>(noside3uvLock.with(X_ROT_270), 1), new Weighted<Variant>(noside1uvLock.with(X_ROT_270), 1), new Weighted<Variant>(noside2uvLock.with(X_ROT_270), 1)))).with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, false), new MultiVariant(WeightedList.of(new Weighted<Variant>(noside3uvLock.with(X_ROT_90), 1), new Weighted<Variant>(noside2uvLock.with(X_ROT_90), 1), new Weighted<Variant>(noside1uvLock.with(X_ROT_90), 1), new Weighted<Variant>(nosideUvLock.with(X_ROT_90), 2)))));
    }

    private void createComposter() {
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.COMPOSTER).with(BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 1), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents1"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 2), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents2"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 3), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents3"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 4), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents4"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 5), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents5"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 6), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents6"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 7), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents7"))).with(BlockModelGenerators.condition().term(BlockStateProperties.LEVEL_COMPOSTER, 8), BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COMPOSTER, "_contents_ready"))));
    }

    private void createCopperBulb(Block copperBulb) {
        MultiVariant baseModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(copperBulb, TextureMapping.cube(copperBulb), this.modelOutput));
        MultiVariant baseModelPowered = BlockModelGenerators.plainVariant(this.createSuffixedVariant(copperBulb, "_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube));
        MultiVariant litModel = BlockModelGenerators.plainVariant(this.createSuffixedVariant(copperBulb, "_lit", ModelTemplates.CUBE_ALL, TextureMapping::cube));
        MultiVariant litModelPowered = BlockModelGenerators.plainVariant(this.createSuffixedVariant(copperBulb, "_lit_powered", ModelTemplates.CUBE_ALL, TextureMapping::cube));
        this.blockStateOutput.accept(BlockModelGenerators.createCopperBulb(copperBulb, baseModel, litModel, baseModelPowered, litModelPowered));
    }

    private static BlockModelDefinitionGenerator createCopperBulb(Block copperBulb, MultiVariant baseModel, MultiVariant litModel, MultiVariant baseModelPowered, MultiVariant litModelPowered) {
        return MultiVariantGenerator.dispatch(copperBulb).with(PropertyDispatch.initial(BlockStateProperties.LIT, BlockStateProperties.POWERED).generate((emittingLight, powered) -> {
            if (emittingLight.booleanValue()) {
                return powered != false ? litModelPowered : litModel;
            }
            return powered != false ? baseModelPowered : baseModel;
        }));
    }

    private void copyCopperBulbModel(Block donor, Block acceptor) {
        MultiVariant baseModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor));
        MultiVariant baseModelPowered = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor, "_powered"));
        MultiVariant litModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor, "_lit"));
        MultiVariant litModelPowered = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor, "_lit_powered"));
        this.itemModelOutput.copy(donor.asItem(), acceptor.asItem());
        this.blockStateOutput.accept(BlockModelGenerators.createCopperBulb(acceptor, baseModel, litModel, baseModelPowered, litModelPowered));
    }

    private void createAmethystCluster(Block clusterBlock) {
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CROSS.create(clusterBlock, TextureMapping.cross(clusterBlock), this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(clusterBlock, model).with(ROTATIONS_COLUMN_WITH_FACING));
    }

    private void createAmethystClusters() {
        this.createAmethystCluster(Blocks.SMALL_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.MEDIUM_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.LARGE_AMETHYST_BUD);
        this.createAmethystCluster(Blocks.AMETHYST_CLUSTER);
    }

    private void createSpeleothem(Block block) {
        PropertyDispatch.C2<MultiVariant, Direction, SpeleothemThickness> generator = PropertyDispatch.initial(BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.SPELEOTHEM_THICKNESS);
        for (SpeleothemThickness speleothemThickness : SpeleothemThickness.values()) {
            generator.select(Direction.UP, speleothemThickness, this.createSpeleothemVariant(Direction.UP, speleothemThickness, block));
        }
        for (SpeleothemThickness speleothemThickness : SpeleothemThickness.values()) {
            generator.select(Direction.DOWN, speleothemThickness, this.createSpeleothemVariant(Direction.DOWN, speleothemThickness, block));
        }
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(generator));
    }

    private MultiVariant createSpeleothemVariant(Direction direction, SpeleothemThickness speleothemThickness, Block block) {
        String suffix = "_" + direction.getSerializedName() + "_" + speleothemThickness.getSerializedName();
        TextureMapping texture = TextureMapping.cross(TextureMapping.getBlockTexture(block, suffix));
        return BlockModelGenerators.plainVariant(ModelTemplates.POINTED_DRIPSTONE.createWithSuffix(block, suffix, texture, this.modelOutput));
    }

    private void createNyliumBlock(Block block) {
        TextureMapping mapping = new TextureMapping().put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.NETHERRACK)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block)).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side"));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP.create(block, mapping, this.modelOutput))));
    }

    private void createDaylightDetector() {
        Material sideTexture = TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_side");
        TextureMapping normalTextures = new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_top")).put(TextureSlot.SIDE, sideTexture);
        TextureMapping invertedTextures = new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DAYLIGHT_DETECTOR, "_inverted_top")).put(TextureSlot.SIDE, sideTexture);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.DAYLIGHT_DETECTOR).with(PropertyDispatch.initial(BlockStateProperties.INVERTED).select(false, BlockModelGenerators.plainVariant(ModelTemplates.DAYLIGHT_DETECTOR.create(Blocks.DAYLIGHT_DETECTOR, normalTextures, this.modelOutput))).select(true, BlockModelGenerators.plainVariant(ModelTemplates.DAYLIGHT_DETECTOR.create(ModelLocationUtils.getModelLocation(Blocks.DAYLIGHT_DETECTOR, "_inverted"), invertedTextures, this.modelOutput)))));
    }

    private void createRotatableColumn(Block block) {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block))).with(ROTATIONS_COLUMN_WITH_FACING));
    }

    private void createLightningRod(Block block, Block waxedBlock) {
        MultiVariant on = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.LIGHTNING_ROD.weathering().unaffected(), "_on"));
        MultiVariant off = BlockModelGenerators.plainVariant(ModelTemplates.LIGHTNING_ROD.create(block, TextureMapping.defaultTexture(block), this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, on, off)).with(ROTATIONS_COLUMN_WITH_FACING));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(waxedBlock).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, on, off)).with(ROTATIONS_COLUMN_WITH_FACING));
        this.itemModelOutput.copy(block.asItem(), waxedBlock.asItem());
    }

    private void createFarmland() {
        TextureMapping dryTextures = new TextureMapping().put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND));
        TextureMapping moistTextures = new TextureMapping().put(TextureSlot.DIRT, TextureMapping.getBlockTexture(Blocks.DIRT)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.FARMLAND, "_moist"));
        MultiVariant dryModel = BlockModelGenerators.plainVariant(ModelTemplates.FARMLAND.create(Blocks.FARMLAND, dryTextures, this.modelOutput));
        MultiVariant moistModel = BlockModelGenerators.plainVariant(ModelTemplates.FARMLAND.create(ModelLocationUtils.getModelLocation(Blocks.FARMLAND, "_moist"), moistTextures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.FARMLAND).with(BlockModelGenerators.createEmptyOrFullDispatch(BlockStateProperties.MOISTURE, 7, moistModel, dryModel)));
    }

    private MultiVariant createFloorFireModels(Block block) {
        return BlockModelGenerators.variants(BlockModelGenerators.plainModel(ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(block, "_floor0"), TextureMapping.fire0(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_FLOOR.create(ModelLocationUtils.getModelLocation(block, "_floor1"), TextureMapping.fire1(block), this.modelOutput)));
    }

    private MultiVariant createSideFireModels(Block block) {
        return BlockModelGenerators.variants(BlockModelGenerators.plainModel(ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(block, "_side0"), TextureMapping.fire0(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_SIDE.create(ModelLocationUtils.getModelLocation(block, "_side1"), TextureMapping.fire1(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(block, "_side_alt0"), TextureMapping.fire0(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_SIDE_ALT.create(ModelLocationUtils.getModelLocation(block, "_side_alt1"), TextureMapping.fire1(block), this.modelOutput)));
    }

    private MultiVariant createTopFireModels(Block block) {
        return BlockModelGenerators.variants(BlockModelGenerators.plainModel(ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(block, "_up0"), TextureMapping.fire0(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_UP.create(ModelLocationUtils.getModelLocation(block, "_up1"), TextureMapping.fire1(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(block, "_up_alt0"), TextureMapping.fire0(block), this.modelOutput)), BlockModelGenerators.plainModel(ModelTemplates.FIRE_UP_ALT.create(ModelLocationUtils.getModelLocation(block, "_up_alt1"), TextureMapping.fire1(block), this.modelOutput)));
    }

    private void createFire() {
        ConditionBuilder noSides = BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false).term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false).term(BlockStateProperties.WEST, false).term(BlockStateProperties.UP, false);
        MultiVariant floorFireModels = this.createFloorFireModels(Blocks.FIRE);
        MultiVariant sideFireModels = this.createSideFireModels(Blocks.FIRE);
        MultiVariant topFireModels = this.createTopFireModels(Blocks.FIRE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.FIRE).with(noSides, floorFireModels).with(BlockModelGenerators.or(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), noSides), sideFireModels).with(BlockModelGenerators.or(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), noSides), sideFireModels.with(Y_ROT_90)).with(BlockModelGenerators.or(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), noSides), sideFireModels.with(Y_ROT_180)).with(BlockModelGenerators.or(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), noSides), sideFireModels.with(Y_ROT_270)).with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), topFireModels));
    }

    private void createSoulFire() {
        MultiVariant floorFireModels = this.createFloorFireModels(Blocks.SOUL_FIRE);
        MultiVariant sideFireModels = this.createSideFireModels(Blocks.SOUL_FIRE);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.SOUL_FIRE).with(floorFireModels).with(sideFireModels).with(sideFireModels.with(Y_ROT_90)).with(sideFireModels.with(Y_ROT_180)).with(sideFireModels.with(Y_ROT_270)));
    }

    private void createLantern(Block block) {
        MultiVariant ground = BlockModelGenerators.plainVariant(TexturedModel.LANTERN.create(block, this.modelOutput));
        MultiVariant hanging = BlockModelGenerators.plainVariant(TexturedModel.HANGING_LANTERN.create(block, this.modelOutput));
        this.registerSimpleFlatItemModel(block.asItem());
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.HANGING, hanging, ground)));
    }

    private void createCopperLantern(Block unwaxed, Block waxed) {
        Identifier ground = TexturedModel.LANTERN.create(unwaxed, this.modelOutput);
        Identifier hanging = TexturedModel.HANGING_LANTERN.create(unwaxed, this.modelOutput);
        this.registerSimpleFlatItemModel(unwaxed.asItem());
        this.itemModelOutput.copy(unwaxed.asItem(), waxed.asItem());
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(unwaxed).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.HANGING, BlockModelGenerators.plainVariant(hanging), BlockModelGenerators.plainVariant(ground))));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(waxed).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.HANGING, BlockModelGenerators.plainVariant(hanging), BlockModelGenerators.plainVariant(ground))));
    }

    private void createCopperChain(Block unwaxed, Block waxed) {
        MultiVariant model = BlockModelGenerators.plainVariant(TexturedModel.CHAIN.create(unwaxed, this.modelOutput));
        this.createAxisAlignedPillarBlockCustomModel(unwaxed, model);
        this.createAxisAlignedPillarBlockCustomModel(waxed, model);
    }

    private void createMuddyMangroveRoots() {
        TextureMapping textures = TextureMapping.column(TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_side"), TextureMapping.getBlockTexture(Blocks.MUDDY_MANGROVE_ROOTS, "_top"));
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.create(Blocks.MUDDY_MANGROVE_ROOTS, textures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(Blocks.MUDDY_MANGROVE_ROOTS, model));
    }

    private void createMangrovePropagule() {
        this.registerSimpleFlatItemModel(Items.MANGROVE_PROPAGULE);
        Block block = Blocks.MANGROVE_PROPAGULE;
        MultiVariant plantedModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.MANGROVE_PROPAGULE).with(PropertyDispatch.initial(MangrovePropaguleBlock.HANGING, MangrovePropaguleBlock.AGE).generate((hanging, age) -> hanging != false ? BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_hanging_" + age)) : plantedModel)));
    }

    private void createFrostedIce() {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.FROSTED_ICE).with(PropertyDispatch.initial(BlockStateProperties.AGE_3).select(0, BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_0", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(1, BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_1", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(2, BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_2", ModelTemplates.CUBE_ALL, TextureMapping::cube))).select(3, BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.FROSTED_ICE, "_3", ModelTemplates.CUBE_ALL, TextureMapping::cube)))));
    }

    private void createGrassBlocks() {
        Material bottomTexture = TextureMapping.getBlockTexture(Blocks.DIRT);
        TextureMapping snowyMapping = new TextureMapping().put(TextureSlot.BOTTOM, bottomTexture).copyForced(TextureSlot.BOTTOM, TextureSlot.PARTICLE).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.GRASS_BLOCK, "_snow"));
        MultiVariant snowyGrass = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.GRASS_BLOCK, "_snow", snowyMapping, this.modelOutput));
        Identifier plainGrassModel = ModelLocationUtils.getModelLocation(Blocks.GRASS_BLOCK);
        this.createGrassLikeBlock(Blocks.GRASS_BLOCK, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(plainGrassModel)), snowyGrass);
        this.registerSimpleTintedItemModel(Blocks.GRASS_BLOCK, plainGrassModel, new GrassColorSource());
        MultiVariant myceliumModel = BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.MYCELIUM).updateTextures(m -> m.put(TextureSlot.BOTTOM, bottomTexture)).create(Blocks.MYCELIUM, this.modelOutput)));
        this.createGrassLikeBlock(Blocks.MYCELIUM, myceliumModel, snowyGrass);
        MultiVariant podzolModel = BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(TexturedModel.CUBE_TOP_BOTTOM.get(Blocks.PODZOL).updateTextures(m -> m.put(TextureSlot.BOTTOM, bottomTexture)).create(Blocks.PODZOL, this.modelOutput)));
        this.createGrassLikeBlock(Blocks.PODZOL, podzolModel, snowyGrass);
    }

    private void createGrassLikeBlock(Block block, MultiVariant normal, MultiVariant snowy) {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.SNOWY).select(true, snowy).select(false, normal)));
    }

    private void createCocoa() {
        this.registerSimpleFlatItemModel(Items.COCOA_BEANS);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.COCOA).with(PropertyDispatch.initial(BlockStateProperties.AGE_2).select(0, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage0"))).select(1, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage1"))).select(2, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.COCOA, "_stage2")))).with(ROTATION_HORIZONTAL_FACING_ALT));
    }

    private void createDirtPath() {
        Variant model = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.DIRT_PATH));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.DIRT_PATH, BlockModelGenerators.createRotatedVariants(model)));
    }

    private void createWeightedPressurePlate(Block block, Block appearance) {
        TextureMapping textures = TextureMapping.defaultTexture(appearance);
        MultiVariant up = BlockModelGenerators.plainVariant(ModelTemplates.PRESSURE_PLATE_UP.create(block, textures, this.modelOutput));
        MultiVariant down = BlockModelGenerators.plainVariant(ModelTemplates.PRESSURE_PLATE_DOWN.create(block, textures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createEmptyOrFullDispatch(BlockStateProperties.POWER, 1, down, up)));
    }

    private void createHopper() {
        MultiVariant downBlock = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.HOPPER));
        MultiVariant sideBlock = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.HOPPER, "_side"));
        this.registerSimpleFlatItemModel(Items.HOPPER);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.HOPPER).with(PropertyDispatch.initial(BlockStateProperties.FACING_HOPPER).select(Direction.DOWN, downBlock).select(Direction.NORTH, sideBlock).select(Direction.EAST, sideBlock.with(Y_ROT_90)).select(Direction.SOUTH, sideBlock.with(Y_ROT_180)).select(Direction.WEST, sideBlock.with(Y_ROT_270))));
    }

    private void copyModel(Block donor, Block acceptor) {
        MultiVariant model = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(donor));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(acceptor, model));
        this.itemModelOutput.copy(donor.asItem(), acceptor.asItem());
    }

    private void createBarsAndItem(Block block) {
        TextureMapping textures = TextureMapping.bars(block);
        this.createBars(block, ModelTemplates.BARS_POST_ENDS.create(block, textures, this.modelOutput), ModelTemplates.BARS_POST.create(block, textures, this.modelOutput), ModelTemplates.BARS_CAP.create(block, textures, this.modelOutput), ModelTemplates.BARS_CAP_ALT.create(block, textures, this.modelOutput), ModelTemplates.BARS_POST_SIDE.create(block, textures, this.modelOutput), ModelTemplates.BARS_POST_SIDE_ALT.create(block, textures, this.modelOutput));
        this.registerSimpleFlatItemModel(block);
    }

    private void createBarsAndItem(Block unwaxed, Block waxed) {
        TextureMapping textures = TextureMapping.bars(unwaxed);
        Identifier postEndResource = ModelTemplates.BARS_POST_ENDS.create(unwaxed, textures, this.modelOutput);
        Identifier postResource = ModelTemplates.BARS_POST.create(unwaxed, textures, this.modelOutput);
        Identifier capResource = ModelTemplates.BARS_CAP.create(unwaxed, textures, this.modelOutput);
        Identifier capAltResource = ModelTemplates.BARS_CAP_ALT.create(unwaxed, textures, this.modelOutput);
        Identifier sideResource = ModelTemplates.BARS_POST_SIDE.create(unwaxed, textures, this.modelOutput);
        Identifier sideAltResource = ModelTemplates.BARS_POST_SIDE_ALT.create(unwaxed, textures, this.modelOutput);
        this.createBars(unwaxed, postEndResource, postResource, capResource, capAltResource, sideResource, sideAltResource);
        this.createBars(waxed, postEndResource, postResource, capResource, capAltResource, sideResource, sideAltResource);
        this.registerSimpleFlatItemModel(unwaxed);
        this.itemModelOutput.copy(unwaxed.asItem(), waxed.asItem());
    }

    private void createBars(Block block, Identifier postEndResource, Identifier postResource, Identifier capResource, Identifier capAltResource, Identifier sideResource, Identifier sideAltResource) {
        MultiVariant postEnds = BlockModelGenerators.plainVariant(postEndResource);
        MultiVariant post = BlockModelGenerators.plainVariant(postResource);
        MultiVariant cap = BlockModelGenerators.plainVariant(capResource);
        MultiVariant capAlt = BlockModelGenerators.plainVariant(capAltResource);
        MultiVariant side = BlockModelGenerators.plainVariant(sideResource);
        MultiVariant sideAlt = BlockModelGenerators.plainVariant(sideAltResource);
        this.blockStateOutput.accept(MultiPartGenerator.multiPart(block).with(postEnds).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false).term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false).term(BlockStateProperties.WEST, false), post).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true).term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false).term(BlockStateProperties.WEST, false), cap).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false).term(BlockStateProperties.EAST, true).term(BlockStateProperties.SOUTH, false).term(BlockStateProperties.WEST, false), cap.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false).term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, true).term(BlockStateProperties.WEST, false), capAlt).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false).term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false).term(BlockStateProperties.WEST, true), capAlt.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), side).with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), side.with(Y_ROT_90)).with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), sideAlt).with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), sideAlt.with(Y_ROT_90)));
    }

    private void createNonTemplateHorizontalBlock(Block block) {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block))).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createLever() {
        MultiVariant off = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.LEVER));
        MultiVariant on = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.LEVER, "_on"));
        this.registerSimpleFlatItemModel(Blocks.LEVER);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.LEVER).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, off, on)).with(PropertyDispatch.modify(BlockStateProperties.ATTACH_FACE, BlockStateProperties.HORIZONTAL_FACING).select(AttachFace.CEILING, Direction.NORTH, X_ROT_180.then(Y_ROT_180)).select(AttachFace.CEILING, Direction.EAST, X_ROT_180.then(Y_ROT_270)).select(AttachFace.CEILING, Direction.SOUTH, X_ROT_180).select(AttachFace.CEILING, Direction.WEST, X_ROT_180.then(Y_ROT_90)).select(AttachFace.FLOOR, Direction.NORTH, NOP).select(AttachFace.FLOOR, Direction.EAST, Y_ROT_90).select(AttachFace.FLOOR, Direction.SOUTH, Y_ROT_180).select(AttachFace.FLOOR, Direction.WEST, Y_ROT_270).select(AttachFace.WALL, Direction.NORTH, X_ROT_90).select(AttachFace.WALL, Direction.EAST, X_ROT_90.then(Y_ROT_90)).select(AttachFace.WALL, Direction.SOUTH, X_ROT_90.then(Y_ROT_180)).select(AttachFace.WALL, Direction.WEST, X_ROT_90.then(Y_ROT_270))));
    }

    private void createLilyPad() {
        Identifier itemModel = this.createFlatItemModelWithBlockTexture(Items.LILY_PAD, Blocks.LILY_PAD);
        this.registerSimpleTintedItemModel(Blocks.LILY_PAD, itemModel, ItemModelUtils.constantTint(-9321636));
        Variant blockModel = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.LILY_PAD));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.LILY_PAD, BlockModelGenerators.createRotatedVariants(blockModel)));
    }

    private void createFrogspawnBlock() {
        this.registerSimpleFlatItemModel(Blocks.FROGSPAWN);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.FROGSPAWN, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.FROGSPAWN))));
    }

    private void createNetherPortalBlock() {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.NETHER_PORTAL).with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_AXIS).select(Direction.Axis.X, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ns"))).select(Direction.Axis.Z, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.NETHER_PORTAL, "_ew")))));
    }

    private void createNetherrack() {
        Variant model = BlockModelGenerators.plainModel(TexturedModel.CUBE.create(Blocks.NETHERRACK, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.NETHERRACK, BlockModelGenerators.variants(model, model.with(X_ROT_90), model.with(X_ROT_180), model.with(X_ROT_270), model.with(Y_ROT_90), model.with(Y_ROT_90.then(X_ROT_90)), model.with(Y_ROT_90.then(X_ROT_180)), model.with(Y_ROT_90.then(X_ROT_270)), model.with(Y_ROT_180), model.with(Y_ROT_180.then(X_ROT_90)), model.with(Y_ROT_180.then(X_ROT_180)), model.with(Y_ROT_180.then(X_ROT_270)), model.with(Y_ROT_270), model.with(Y_ROT_270.then(X_ROT_90)), model.with(Y_ROT_270.then(X_ROT_180)), model.with(Y_ROT_270.then(X_ROT_270)))));
    }

    private void createObserver() {
        MultiVariant off = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.OBSERVER));
        MultiVariant on = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.OBSERVER, "_on"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.OBSERVER).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, on, off)).with(ROTATION_FACING));
    }

    private void createPistons() {
        TextureMapping commonMapping = new TextureMapping().put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.PISTON, "_bottom")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        Material topSticky = TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky");
        Material top = TextureMapping.getBlockTexture(Blocks.PISTON, "_top");
        TextureMapping stickyTextures = commonMapping.copyAndUpdate(TextureSlot.PLATFORM, topSticky);
        TextureMapping normalTextures = commonMapping.copyAndUpdate(TextureSlot.PLATFORM, top);
        MultiVariant extendedPiston = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PISTON, "_base"));
        this.createPistonVariant(Blocks.PISTON, extendedPiston, normalTextures);
        this.createPistonVariant(Blocks.STICKY_PISTON, extendedPiston, stickyTextures);
        Identifier normalInventory = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.PISTON, "_inventory", commonMapping.copyAndUpdate(TextureSlot.TOP, top), this.modelOutput);
        Identifier stickyInventory = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.STICKY_PISTON, "_inventory", commonMapping.copyAndUpdate(TextureSlot.TOP, topSticky), this.modelOutput);
        this.registerSimpleItemModel(Blocks.PISTON, normalInventory);
        this.registerSimpleItemModel(Blocks.STICKY_PISTON, stickyInventory);
    }

    private void createPistonVariant(Block block, MultiVariant extended, TextureMapping textures) {
        MultiVariant retracted = BlockModelGenerators.plainVariant(ModelTemplates.PISTON.create(block, textures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.EXTENDED, extended, retracted)).with(ROTATION_FACING));
    }

    private void createPistonHeads() {
        TextureMapping commonMapping = new TextureMapping().put(TextureSlot.UNSTICKY, TextureMapping.getBlockTexture(Blocks.PISTON, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        TextureMapping stickyTextures = commonMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top_sticky"));
        TextureMapping normalTextures = commonMapping.copyAndUpdate(TextureSlot.PLATFORM, TextureMapping.getBlockTexture(Blocks.PISTON, "_top"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.PISTON_HEAD).with(PropertyDispatch.initial(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE).select(false, PistonType.DEFAULT, BlockModelGenerators.plainVariant(ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head", normalTextures, this.modelOutput))).select(false, PistonType.STICKY, BlockModelGenerators.plainVariant(ModelTemplates.PISTON_HEAD.createWithSuffix(Blocks.PISTON, "_head_sticky", stickyTextures, this.modelOutput))).select(true, PistonType.DEFAULT, BlockModelGenerators.plainVariant(ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short", normalTextures, this.modelOutput))).select(true, PistonType.STICKY, BlockModelGenerators.plainVariant(ModelTemplates.PISTON_HEAD_SHORT.createWithSuffix(Blocks.PISTON, "_head_short_sticky", stickyTextures, this.modelOutput)))).with(ROTATION_FACING));
    }

    private void createTrialSpawner() {
        Block block = Blocks.TRIAL_SPAWNER;
        TextureMapping inactiveTextures = TextureMapping.trialSpawner(block, "_side_inactive", "_top_inactive");
        TextureMapping activeTextures = TextureMapping.trialSpawner(block, "_side_active", "_top_active");
        TextureMapping ejectingRewardTextures = TextureMapping.trialSpawner(block, "_side_active", "_top_ejecting_reward");
        TextureMapping ominousInactiveTextures = TextureMapping.trialSpawner(block, "_side_inactive_ominous", "_top_inactive_ominous");
        TextureMapping ominousActiveTextures = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_active_ominous");
        TextureMapping ominousEjectingRewardTextures = TextureMapping.trialSpawner(block, "_side_active_ominous", "_top_ejecting_reward_ominous");
        Identifier inactiveModel = ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.create(block, inactiveTextures, this.modelOutput);
        MultiVariant inactive = BlockModelGenerators.plainVariant(inactiveModel);
        MultiVariant active = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_active", activeTextures, this.modelOutput));
        MultiVariant ejectingReward = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_ejecting_reward", ejectingRewardTextures, this.modelOutput));
        MultiVariant ominousInactive = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_inactive_ominous", ominousInactiveTextures, this.modelOutput));
        MultiVariant ominousActive = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_active_ominous", ominousActiveTextures, this.modelOutput));
        MultiVariant ominousEjectingReward = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP_INNER_FACES.createWithSuffix(block, "_ejecting_reward_ominous", ominousEjectingRewardTextures, this.modelOutput));
        this.registerSimpleItemModel(block, inactiveModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BlockStateProperties.TRIAL_SPAWNER_STATE, BlockStateProperties.OMINOUS).generate((state, ominous) -> switch (state) {
            default -> throw new MatchException(null, null);
            case TrialSpawnerState.INACTIVE, TrialSpawnerState.COOLDOWN -> {
                if (ominous.booleanValue()) {
                    yield ominousInactive;
                }
                yield inactive;
            }
            case TrialSpawnerState.WAITING_FOR_PLAYERS, TrialSpawnerState.ACTIVE, TrialSpawnerState.WAITING_FOR_REWARD_EJECTION -> {
                if (ominous.booleanValue()) {
                    yield ominousActive;
                }
                yield active;
            }
            case TrialSpawnerState.EJECTING_REWARD -> ominous != false ? ominousEjectingReward : ejectingReward;
        })));
    }

    private void createVault() {
        Block block = Blocks.VAULT;
        TextureMapping inactiveTextures = TextureMapping.vault(block, "_front_off", "_side_off", "_top", "_bottom");
        TextureMapping activeTextures = TextureMapping.vault(block, "_front_on", "_side_on", "_top", "_bottom");
        TextureMapping unlockingTextures = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top", "_bottom");
        TextureMapping ejectingRewardTextures = TextureMapping.vault(block, "_front_ejecting", "_side_on", "_top_ejecting", "_bottom");
        Identifier inactiveModel = ModelTemplates.VAULT.create(block, inactiveTextures, this.modelOutput);
        MultiVariant inactive = BlockModelGenerators.plainVariant(inactiveModel);
        MultiVariant active = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_active", activeTextures, this.modelOutput));
        MultiVariant unlocking = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_unlocking", unlockingTextures, this.modelOutput));
        MultiVariant ejectingReward = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward", ejectingRewardTextures, this.modelOutput));
        TextureMapping inactiveTexturesOminous = TextureMapping.vault(block, "_front_off_ominous", "_side_off_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping activeTexturesOminous = TextureMapping.vault(block, "_front_on_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping unlockingTexturesOminous = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ominous", "_bottom_ominous");
        TextureMapping ejectingRewardTexturesOminous = TextureMapping.vault(block, "_front_ejecting_ominous", "_side_on_ominous", "_top_ejecting_ominous", "_bottom_ominous");
        MultiVariant inactiveOminous = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ominous", inactiveTexturesOminous, this.modelOutput));
        MultiVariant activeOminous = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_active_ominous", activeTexturesOminous, this.modelOutput));
        MultiVariant unlockingOminous = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_unlocking_ominous", unlockingTexturesOminous, this.modelOutput));
        MultiVariant ejectingRewardOminous = BlockModelGenerators.plainVariant(ModelTemplates.VAULT.createWithSuffix(block, "_ejecting_reward_ominous", ejectingRewardTexturesOminous, this.modelOutput));
        this.registerSimpleItemModel(block, inactiveModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(VaultBlock.STATE, VaultBlock.OMINOUS).generate((state, ominous) -> switch (state) {
            default -> throw new MatchException(null, null);
            case VaultState.INACTIVE -> {
                if (ominous.booleanValue()) {
                    yield inactiveOminous;
                }
                yield inactive;
            }
            case VaultState.ACTIVE -> {
                if (ominous.booleanValue()) {
                    yield activeOminous;
                }
                yield active;
            }
            case VaultState.UNLOCKING -> {
                if (ominous.booleanValue()) {
                    yield unlockingOminous;
                }
                yield unlocking;
            }
            case VaultState.EJECTING -> ominous != false ? ejectingRewardOminous : ejectingReward;
        })).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createSculkSensor() {
        Identifier inactiveModel = ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_inactive");
        MultiVariant inactive = BlockModelGenerators.plainVariant(inactiveModel);
        MultiVariant active = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SCULK_SENSOR, "_active"));
        this.registerSimpleItemModel(Blocks.SCULK_SENSOR, inactiveModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SCULK_SENSOR).with(PropertyDispatch.initial(BlockStateProperties.SCULK_SENSOR_PHASE).generate(phase -> phase == SculkSensorPhase.ACTIVE || phase == SculkSensorPhase.COOLDOWN ? active : inactive)));
    }

    private void createCalibratedSculkSensor() {
        Identifier inactiveModel = ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_inactive");
        MultiVariant inactive = BlockModelGenerators.plainVariant(inactiveModel);
        MultiVariant active = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.CALIBRATED_SCULK_SENSOR, "_active"));
        this.registerSimpleItemModel(Blocks.CALIBRATED_SCULK_SENSOR, inactiveModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CALIBRATED_SCULK_SENSOR).with(PropertyDispatch.initial(BlockStateProperties.SCULK_SENSOR_PHASE).generate(phase -> phase == SculkSensorPhase.ACTIVE || phase == SculkSensorPhase.COOLDOWN ? active : inactive)).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createSculkShrieker() {
        Identifier sculkShriekerModel = ModelTemplates.SCULK_SHRIEKER.create(Blocks.SCULK_SHRIEKER, TextureMapping.sculkShrieker(false), this.modelOutput);
        MultiVariant sculkShrieker = BlockModelGenerators.plainVariant(sculkShriekerModel);
        MultiVariant sculkShriekerCanSummon = BlockModelGenerators.plainVariant(ModelTemplates.SCULK_SHRIEKER.createWithSuffix(Blocks.SCULK_SHRIEKER, "_can_summon", TextureMapping.sculkShrieker(true), this.modelOutput));
        this.registerSimpleItemModel(Blocks.SCULK_SHRIEKER, sculkShriekerModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SCULK_SHRIEKER).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.CAN_SUMMON, sculkShriekerCanSummon, sculkShrieker)));
    }

    private void createScaffolding() {
        Identifier stableModel = ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_stable");
        MultiVariant stable = BlockModelGenerators.plainVariant(stableModel);
        MultiVariant unstable = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SCAFFOLDING, "_unstable"));
        this.registerSimpleItemModel(Blocks.SCAFFOLDING, stableModel);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SCAFFOLDING).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.BOTTOM, unstable, stable)));
    }

    private void createCaveVines() {
        MultiVariant offHead = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES, "", ModelTemplates.CROSS, TextureMapping::cross));
        MultiVariant onHead = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES, "_lit", ModelTemplates.CROSS, TextureMapping::cross));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CAVE_VINES).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.BERRIES, onHead, offHead)));
        MultiVariant offBody = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "", ModelTemplates.CROSS, TextureMapping::cross));
        MultiVariant onBody = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.CAVE_VINES_PLANT, "_lit", ModelTemplates.CROSS, TextureMapping::cross));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.CAVE_VINES_PLANT).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.BERRIES, onBody, offBody)));
    }

    private void createRedstoneLamp() {
        MultiVariant off = BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(Blocks.REDSTONE_LAMP, this.modelOutput));
        MultiVariant on = BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.REDSTONE_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.REDSTONE_LAMP).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, on, off)));
    }

    private void createNormalTorch(Block ground, Block wall) {
        TextureMapping textures = TextureMapping.torch(ground);
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(ground, BlockModelGenerators.plainVariant(ModelTemplates.TORCH.create(ground, textures, this.modelOutput))));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(wall, BlockModelGenerators.plainVariant(ModelTemplates.WALL_TORCH.create(wall, textures, this.modelOutput))).with(ROTATION_TORCH));
        this.registerSimpleFlatItemModel(ground);
    }

    private void createRedstoneTorch() {
        TextureMapping onTextures = TextureMapping.torch(Blocks.REDSTONE_TORCH);
        TextureMapping offTextures = TextureMapping.torch(TextureMapping.getBlockTexture(Blocks.REDSTONE_TORCH, "_off"));
        MultiVariant groundModelOn = BlockModelGenerators.plainVariant(ModelTemplates.REDSTONE_TORCH.create(Blocks.REDSTONE_TORCH, onTextures, this.modelOutput));
        MultiVariant groundModelOff = BlockModelGenerators.plainVariant(ModelTemplates.TORCH_UNLIT.createWithSuffix(Blocks.REDSTONE_TORCH, "_off", offTextures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.REDSTONE_TORCH).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, groundModelOn, groundModelOff)));
        MultiVariant wallModelOn = BlockModelGenerators.plainVariant(ModelTemplates.REDSTONE_WALL_TORCH.create(Blocks.REDSTONE_WALL_TORCH, onTextures, this.modelOutput));
        MultiVariant wallModelOff = BlockModelGenerators.plainVariant(ModelTemplates.WALL_TORCH_UNLIT.createWithSuffix(Blocks.REDSTONE_WALL_TORCH, "_off", offTextures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.REDSTONE_WALL_TORCH).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, wallModelOn, wallModelOff)).with(ROTATION_TORCH));
        this.registerSimpleFlatItemModel(Blocks.REDSTONE_TORCH);
    }

    private void createRepeater() {
        this.registerSimpleFlatItemModel(Items.REPEATER);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.REPEATER).with(PropertyDispatch.initial(BlockStateProperties.DELAY, BlockStateProperties.LOCKED, BlockStateProperties.POWERED).generate((Function3<Integer, Boolean, Boolean, MultiVariant>)((Function3)(delay, locked, powered) -> {
            StringBuilder suffix = new StringBuilder();
            suffix.append('_').append(delay).append("tick");
            if (powered.booleanValue()) {
                suffix.append("_on");
            }
            if (locked.booleanValue()) {
                suffix.append("_locked");
            }
            return BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.REPEATER, suffix.toString()));
        }))).with(ROTATION_HORIZONTAL_FACING_ALT));
    }

    private void createSeaPickle() {
        this.registerSimpleFlatItemModel(Items.SEA_PICKLE);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SEA_PICKLE).with(PropertyDispatch.initial(BlockStateProperties.PICKLES, BlockStateProperties.WATERLOGGED).select(1, false, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("dead_sea_pickle")))).select(2, false, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("two_dead_sea_pickles")))).select(3, false, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("three_dead_sea_pickles")))).select(4, false, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("four_dead_sea_pickles")))).select(1, true, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("sea_pickle")))).select(2, true, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("two_sea_pickles")))).select(3, true, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("three_sea_pickles")))).select(4, true, BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(ModelLocationUtils.decorateBlockModelLocation("four_sea_pickles"))))));
    }

    private void createSnowBlocks() {
        TextureMapping textures = TextureMapping.cube(Blocks.SNOW);
        MultiVariant snowModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(Blocks.SNOW_BLOCK, textures, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SNOW).with(PropertyDispatch.initial(BlockStateProperties.LAYERS).generate(level -> level < 8 ? BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height" + level * 2)) : snowModel)));
        this.registerSimpleItemModel(Blocks.SNOW, ModelLocationUtils.getModelLocation(Blocks.SNOW, "_height2"));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.SNOW_BLOCK, snowModel));
    }

    private void createStonecutter() {
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.STONECUTTER, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.STONECUTTER))).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createStructureBlock() {
        Identifier inventory = TexturedModel.CUBE.create(Blocks.STRUCTURE_BLOCK, this.modelOutput);
        this.registerSimpleItemModel(Blocks.STRUCTURE_BLOCK, inventory);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.STRUCTURE_BLOCK).with(PropertyDispatch.initial(BlockStateProperties.STRUCTUREBLOCK_MODE).generate(model -> BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.STRUCTURE_BLOCK, "_" + model.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube)))));
    }

    private void createTestBlock() {
        HashMap<TestBlockMode, Identifier> variantIds = new HashMap<TestBlockMode, Identifier>();
        for (TestBlockMode mode2 : TestBlockMode.values()) {
            variantIds.put(mode2, this.createSuffixedVariant(Blocks.TEST_BLOCK, "_" + mode2.getSerializedName(), ModelTemplates.CUBE_ALL, TextureMapping::cube));
        }
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.TEST_BLOCK).with(PropertyDispatch.initial(BlockStateProperties.TEST_BLOCK_MODE).generate(mode -> BlockModelGenerators.plainVariant((Identifier)variantIds.get(mode)))));
        this.itemModelOutput.accept(Items.TEST_BLOCK, ItemModelUtils.selectBlockItemProperty(TestBlock.MODE, ItemModelUtils.plainModel((Identifier)variantIds.get(TestBlockMode.START)), Map.of(TestBlockMode.FAIL, ItemModelUtils.plainModel((Identifier)variantIds.get(TestBlockMode.FAIL)), TestBlockMode.LOG, ItemModelUtils.plainModel((Identifier)variantIds.get(TestBlockMode.LOG)), TestBlockMode.ACCEPT, ItemModelUtils.plainModel((Identifier)variantIds.get(TestBlockMode.ACCEPT)))));
    }

    private void createSweetBerryBush() {
        this.registerSimpleFlatItemModel(Items.SWEET_BERRIES);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SWEET_BERRY_BUSH).with(PropertyDispatch.initial(BlockStateProperties.AGE_3).generate(age -> BlockModelGenerators.plainVariant(this.createSuffixedVariant(Blocks.SWEET_BERRY_BUSH, "_stage" + age, ModelTemplates.CROSS, TextureMapping::cross)))));
    }

    private void createTripwire() {
        this.registerSimpleFlatItemModel(Items.STRING);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.TRIPWIRE).with(PropertyDispatch.initial(BlockStateProperties.ATTACHED, BlockStateProperties.EAST, BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.WEST).select(false, false, false, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))).select(false, true, false, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_90)).select(false, false, true, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n"))).select(false, false, false, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_180)).select(false, false, false, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_n")).with(Y_ROT_270)).select(false, true, true, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne"))).select(false, true, false, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_90)).select(false, false, false, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_180)).select(false, false, true, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ne")).with(Y_ROT_270)).select(false, false, true, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns"))).select(false, true, false, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_ns")).with(Y_ROT_90)).select(false, true, true, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse"))).select(false, true, false, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_90)).select(false, false, true, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_180)).select(false, true, true, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nse")).with(Y_ROT_270)).select(false, true, true, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_nsew"))).select(true, false, false, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))).select(true, false, true, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n"))).select(true, false, false, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_180)).select(true, true, false, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_90)).select(true, false, false, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_n")).with(Y_ROT_270)).select(true, true, true, false, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne"))).select(true, true, false, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_90)).select(true, false, false, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_180)).select(true, false, true, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ne")).with(Y_ROT_270)).select(true, false, true, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns"))).select(true, true, false, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_ns")).with(Y_ROT_90)).select(true, true, true, true, false, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse"))).select(true, true, false, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_90)).select(true, false, true, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_180)).select(true, true, true, false, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nse")).with(Y_ROT_270)).select(true, true, true, true, true, BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE, "_attached_nsew")))));
    }

    private void createTripwireHook() {
        this.registerSimpleFlatItemModel(Blocks.TRIPWIRE_HOOK);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.TRIPWIRE_HOOK).with(PropertyDispatch.initial(BlockStateProperties.ATTACHED, BlockStateProperties.POWERED).generate((attached, powered) -> BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.TRIPWIRE_HOOK, (attached != false ? "_attached" : "") + (powered != false ? "_on" : ""))))).with(ROTATION_HORIZONTAL_FACING));
    }

    private Variant createTurtleEggModel(int count, String hatchProgress, TextureMapping texture) {
        return switch (count) {
            case 1 -> BlockModelGenerators.plainModel(ModelTemplates.TURTLE_EGG.create(ModelLocationUtils.decorateBlockModelLocation(hatchProgress + "turtle_egg"), texture, this.modelOutput));
            case 2 -> BlockModelGenerators.plainModel(ModelTemplates.TWO_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("two_" + hatchProgress + "turtle_eggs"), texture, this.modelOutput));
            case 3 -> BlockModelGenerators.plainModel(ModelTemplates.THREE_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("three_" + hatchProgress + "turtle_eggs"), texture, this.modelOutput));
            case 4 -> BlockModelGenerators.plainModel(ModelTemplates.FOUR_TURTLE_EGGS.create(ModelLocationUtils.decorateBlockModelLocation("four_" + hatchProgress + "turtle_eggs"), texture, this.modelOutput));
            default -> throw new UnsupportedOperationException();
        };
    }

    private Variant createTurtleEggModel(int eggs, int hatch) {
        return switch (hatch) {
            case 0 -> this.createTurtleEggModel(eggs, "", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG)));
            case 1 -> this.createTurtleEggModel(eggs, "slightly_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_slightly_cracked")));
            case 2 -> this.createTurtleEggModel(eggs, "very_cracked_", TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.TURTLE_EGG, "_very_cracked")));
            default -> throw new UnsupportedOperationException();
        };
    }

    private void createTurtleEgg() {
        this.registerSimpleFlatItemModel(Items.TURTLE_EGG);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.TURTLE_EGG).with(PropertyDispatch.initial(BlockStateProperties.EGGS, BlockStateProperties.HATCH).generate((eggs, hatch) -> BlockModelGenerators.createRotatedVariants(this.createTurtleEggModel((int)eggs, (int)hatch)))));
    }

    private void createDriedGhastBlock() {
        Identifier driedGhast = ModelLocationUtils.getModelLocation(Blocks.DRIED_GHAST, "_hydration_0");
        this.registerSimpleItemModel(Blocks.DRIED_GHAST, driedGhast);
        Function<Integer, Identifier> createModel = stage -> {
            String suffix = switch (stage) {
                case 1 -> "_hydration_1";
                case 2 -> "_hydration_2";
                case 3 -> "_hydration_3";
                default -> "_hydration_0";
            };
            TextureMapping texture = TextureMapping.driedGhast(suffix);
            return ModelTemplates.DRIED_GHAST.createWithSuffix(Blocks.DRIED_GHAST, suffix, texture, this.modelOutput);
        };
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.DRIED_GHAST).with(PropertyDispatch.initial(DriedGhastBlock.HYDRATION_LEVEL).generate(stage -> BlockModelGenerators.plainVariant((Identifier)createModel.apply((Integer)stage)))).with(ROTATION_HORIZONTAL_FACING));
    }

    private void createSnifferEgg() {
        this.registerSimpleFlatItemModel(Items.SNIFFER_EGG);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SNIFFER_EGG).with(PropertyDispatch.initial(SnifferEggBlock.HATCH).generate(stage -> {
            String suffix = switch (stage) {
                case 1 -> "_slightly_cracked";
                case 2 -> "_very_cracked";
                default -> "_not_cracked";
            };
            TextureMapping texture = TextureMapping.snifferEgg(suffix);
            return BlockModelGenerators.plainVariant(ModelTemplates.SNIFFER_EGG.createWithSuffix(Blocks.SNIFFER_EGG, suffix, texture, this.modelOutput));
        })));
    }

    private void createMultiface(Block block) {
        this.registerSimpleFlatItemModel(block);
        this.createMultifaceBlockStates(block);
    }

    private void createMultiface(Block block, Item item) {
        this.registerSimpleFlatItemModel(item);
        this.createMultifaceBlockStates(block);
    }

    private static <T extends Property<?>> Map<T, VariantMutator> selectMultifaceProperties(StateHolder<?, ?> holder, Function<Direction, T> converter) {
        ImmutableMap.Builder result = ImmutableMap.builderWithExpectedSize((int)MULTIFACE_GENERATOR.size());
        MULTIFACE_GENERATOR.forEach((direction, mutator) -> {
            Property property = (Property)converter.apply((Direction)direction);
            if (holder.hasProperty(property)) {
                result.put((Object)property, mutator);
            }
        });
        return result.build();
    }

    private void createMultifaceBlockStates(Block block) {
        Map<Property, VariantMutator> directionProperties = BlockModelGenerators.selectMultifaceProperties(block.defaultBlockState(), MultifaceBlock::getFaceProperty);
        ConditionBuilder noFaces = BlockModelGenerators.condition();
        directionProperties.forEach((property, mutator) -> noFaces.term(property, false));
        MultiVariant model = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block));
        MultiPartGenerator generator = MultiPartGenerator.multiPart(block);
        directionProperties.forEach((property, mutator) -> {
            generator.with(BlockModelGenerators.condition().term(property, true), model.with((VariantMutator)mutator));
            generator.with(noFaces, model.with((VariantMutator)mutator));
        });
        this.blockStateOutput.accept(generator);
    }

    private void createMossyCarpet(Block block) {
        Map<Property, VariantMutator> directionProperties = BlockModelGenerators.selectMultifaceProperties(block.defaultBlockState(), MossyCarpetBlock::getPropertyForFace);
        ConditionBuilder noFaces = BlockModelGenerators.condition().term(MossyCarpetBlock.BASE, false);
        directionProperties.forEach((property, mutator) -> noFaces.term(property, WallSide.NONE));
        MultiVariant modelCarpet = BlockModelGenerators.plainVariant(TexturedModel.CARPET.create(block, this.modelOutput));
        MultiVariant modelSideTall = BlockModelGenerators.plainVariant(TexturedModel.MOSSY_CARPET_SIDE.get(block).updateTextures(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side_tall"))).createWithSuffix(block, "_side_tall", this.modelOutput));
        MultiVariant modelSideSmall = BlockModelGenerators.plainVariant(TexturedModel.MOSSY_CARPET_SIDE.get(block).updateTextures(m -> m.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side_small"))).createWithSuffix(block, "_side_small", this.modelOutput));
        MultiPartGenerator generator = MultiPartGenerator.multiPart(block);
        generator.with(BlockModelGenerators.condition().term(MossyCarpetBlock.BASE, true), modelCarpet);
        generator.with(noFaces, modelCarpet);
        directionProperties.forEach((property, mutator) -> {
            generator.with(BlockModelGenerators.condition().term(property, WallSide.TALL), modelSideTall.with((VariantMutator)mutator));
            generator.with(BlockModelGenerators.condition().term(property, WallSide.LOW), modelSideSmall.with((VariantMutator)mutator));
            generator.with(noFaces, modelSideTall.with((VariantMutator)mutator));
        });
        this.blockStateOutput.accept(generator);
    }

    private void createHangingMoss(Block block) {
        this.registerSimpleFlatItemModel(block);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(HangingMossBlock.TIP).generate(isTip -> {
            String suffix = isTip != false ? "_tip" : "";
            TextureMapping texture = TextureMapping.cross(TextureMapping.getBlockTexture(block, suffix));
            return BlockModelGenerators.plainVariant(PlantType.NOT_TINTED.getCross().createWithSuffix(block, suffix, texture, this.modelOutput));
        })));
    }

    private void createSculkCatalyst() {
        Material bottom = TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_bottom");
        TextureMapping defaultTextureMap = new TextureMapping().put(TextureSlot.BOTTOM, bottom).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side"));
        TextureMapping bloomTextureMap = new TextureMapping().put(TextureSlot.BOTTOM, bottom).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_top_bloom")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_CATALYST, "_side_bloom"));
        Identifier defaultModel = ModelTemplates.CUBE_BOTTOM_TOP.create(Blocks.SCULK_CATALYST, defaultTextureMap, this.modelOutput);
        MultiVariant defaultVariant = BlockModelGenerators.plainVariant(defaultModel);
        MultiVariant bloom = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.SCULK_CATALYST, "_bloom", bloomTextureMap, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.SCULK_CATALYST).with(PropertyDispatch.initial(BlockStateProperties.BLOOM).generate(pulse -> pulse != false ? bloom : defaultVariant)));
        this.registerSimpleItemModel(Blocks.SCULK_CATALYST, defaultModel);
    }

    private void createShelf(Block block, Block particle) {
        TextureMapping mapping = new TextureMapping().put(TextureSlot.ALL, TextureMapping.getBlockTexture(block)).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(particle));
        MultiPartGenerator generator = MultiPartGenerator.multiPart(block);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_BODY, null, null);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_UNPOWERED, false, null);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_UNCONNECTED, true, SideChainPart.UNCONNECTED);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_LEFT, true, SideChainPart.LEFT);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_CENTER, true, SideChainPart.CENTER);
        this.addShelfPart(block, mapping, generator, ModelTemplates.SHELF_RIGHT, true, SideChainPart.RIGHT);
        this.blockStateOutput.accept(generator);
        this.registerSimpleItemModel(block, ModelTemplates.SHELF_INVENTORY.create(block, mapping, this.modelOutput));
    }

    private void addShelfPart(Block block, TextureMapping mapping, MultiPartGenerator generator, ModelTemplate template, @Nullable Boolean isPowered, @Nullable SideChainPart sideChainPart) {
        MultiVariant variant = BlockModelGenerators.plainVariant(template.create(block, mapping, this.modelOutput));
        BlockModelGenerators.forEachHorizontalDirection((direction, rotation) -> generator.with(BlockModelGenerators.shelfCondition(direction, isPowered, sideChainPart), variant.with((VariantMutator)rotation)));
    }

    private static void forEachHorizontalDirection(BiConsumer<Direction, VariantMutator> consumer) {
        List.of(Pair.of((Object)Direction.NORTH, (Object)NOP), Pair.of((Object)Direction.EAST, (Object)Y_ROT_90), Pair.of((Object)Direction.SOUTH, (Object)Y_ROT_180), Pair.of((Object)Direction.WEST, (Object)Y_ROT_270)).forEach(pair -> {
            Direction direction = (Direction)pair.getFirst();
            VariantMutator rotation = (VariantMutator)pair.getSecond();
            consumer.accept(direction, rotation);
        });
    }

    private static Condition shelfCondition(Direction direction, @Nullable Boolean isPowered, @Nullable SideChainPart sideChainPart) {
        ConditionBuilder facing = BlockModelGenerators.condition(BlockStateProperties.HORIZONTAL_FACING, (Enum)direction, (Enum[])new Direction[0]);
        if (isPowered == null) {
            return facing.build();
        }
        ConditionBuilder powered = BlockModelGenerators.condition(BlockStateProperties.POWERED, isPowered);
        return sideChainPart != null ? BlockModelGenerators.and(facing, powered, BlockModelGenerators.condition(BlockStateProperties.SIDE_CHAIN_PART, (Enum)sideChainPart, (Enum[])new SideChainPart[0])) : BlockModelGenerators.and(facing, powered);
    }

    private void createChiseledBookshelf() {
        Block block = Blocks.CHISELED_BOOKSHELF;
        MultiVariant body = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block));
        MultiPartGenerator multiPartGenerator = MultiPartGenerator.multiPart(block);
        BlockModelGenerators.forEachHorizontalDirection((direction, rotation) -> {
            Condition facingCondition = BlockModelGenerators.condition().term(BlockStateProperties.HORIZONTAL_FACING, direction).build();
            multiPartGenerator.with(facingCondition, body.with((VariantMutator)rotation).with(UV_LOCK));
            this.addSlotStateAndRotationVariants(multiPartGenerator, facingCondition, (VariantMutator)rotation);
        });
        this.blockStateOutput.accept(multiPartGenerator);
        this.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block, "_inventory"));
        CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.clear();
    }

    private void addSlotStateAndRotationVariants(MultiPartGenerator multiPartGenerator, Condition facingCondition, VariantMutator mutator) {
        List.of(Pair.of((Object)ChiseledBookShelfBlock.SLOT_0_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_LEFT), Pair.of((Object)ChiseledBookShelfBlock.SLOT_1_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_MID), Pair.of((Object)ChiseledBookShelfBlock.SLOT_2_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_RIGHT), Pair.of((Object)ChiseledBookShelfBlock.SLOT_3_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT), Pair.of((Object)ChiseledBookShelfBlock.SLOT_4_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_MID), Pair.of((Object)ChiseledBookShelfBlock.SLOT_5_OCCUPIED, (Object)ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT)).forEach(pair -> {
            BooleanProperty stateProperty = (BooleanProperty)pair.getFirst();
            ModelTemplate modelTemplate = (ModelTemplate)pair.getSecond();
            this.addBookSlotModel(multiPartGenerator, facingCondition, mutator, stateProperty, modelTemplate, true);
            this.addBookSlotModel(multiPartGenerator, facingCondition, mutator, stateProperty, modelTemplate, false);
        });
    }

    private void addBookSlotModel(MultiPartGenerator multiPartGenerator, Condition facingCondition, VariantMutator mutator, BooleanProperty stateProperty, ModelTemplate template, boolean isSlotOccupied) {
        String suffix = isSlotOccupied ? "_occupied" : "_empty";
        TextureMapping mapping = new TextureMapping().put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(Blocks.CHISELED_BOOKSHELF, suffix));
        BookSlotModelCacheKey cacheKey = new BookSlotModelCacheKey(template, suffix);
        MultiVariant model = BlockModelGenerators.plainVariant(CHISELED_BOOKSHELF_SLOT_MODEL_CACHE.computeIfAbsent(cacheKey, key -> template.createWithSuffix(Blocks.CHISELED_BOOKSHELF, suffix, mapping, this.modelOutput)));
        multiPartGenerator.with(new CombinedCondition(CombinedCondition.Operation.AND, List.of(facingCondition, BlockModelGenerators.condition().term(stateProperty, isSlotOccupied).build())), model.with(mutator));
    }

    private void createMagmaBlock() {
        Material texture = new Material(Identifier.withDefaultNamespace("block/magma"));
        MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_ALL.create(Blocks.MAGMA_BLOCK, TextureMapping.cube(texture), this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(Blocks.MAGMA_BLOCK, model));
    }

    private void createShulkerBox(Block block, @Nullable DyeColor color) {
        this.createParticleOnlyBlock(block);
        Item item = block.asItem();
        Identifier baseModel = ModelTemplates.SHULKER_BOX_INVENTORY.create(item, TextureMapping.particle(block), this.modelOutput);
        Transformation transformation = ShulkerBoxRenderer.modelTransform(Direction.UP);
        ItemModel.Unbaked itemModel = color != null ? ItemModelUtils.specialModel(baseModel, transformation, new ShulkerBoxSpecialRenderer.Unbaked(color)) : ItemModelUtils.specialModel(baseModel, transformation, new ShulkerBoxSpecialRenderer.Unbaked());
        this.itemModelOutput.accept(item, itemModel);
    }

    private void createGrowingPlant(Block kelp, Block kelpPlant, PlantType type) {
        this.createCrossBlock(kelp, type);
        this.createCrossBlock(kelpPlant, type);
    }

    private void createInfestedStone() {
        Identifier normalModel = ModelLocationUtils.getModelLocation(Blocks.STONE);
        Variant normal = BlockModelGenerators.plainModel(normalModel);
        Variant mirrored = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.STONE, "_mirrored"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.INFESTED_STONE, BlockModelGenerators.createRotatedVariants(normal, mirrored)));
        this.registerSimpleItemModel(Blocks.INFESTED_STONE, normalModel);
    }

    private void createInfestedDeepslate() {
        Identifier normalModel = ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE);
        Variant normal = BlockModelGenerators.plainModel(normalModel);
        Variant mirrored = BlockModelGenerators.plainModel(ModelLocationUtils.getModelLocation(Blocks.DEEPSLATE, "_mirrored"));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.INFESTED_DEEPSLATE, BlockModelGenerators.createRotatedVariants(normal, mirrored)).with(BlockModelGenerators.createRotatedPillar()));
        this.registerSimpleItemModel(Blocks.INFESTED_DEEPSLATE, normalModel);
    }

    private void createNetherRoots(Block roots, Block pottedRoots) {
        this.createCrossBlockWithDefaultItem(roots, PlantType.NOT_TINTED);
        TextureMapping textures = TextureMapping.plant(TextureMapping.getBlockTexture(roots, "_pot"));
        MultiVariant model = BlockModelGenerators.plainVariant(PlantType.NOT_TINTED.getCrossPot().create(pottedRoots, textures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(pottedRoots, model));
    }

    private void createRespawnAnchor() {
        Material bottom = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_bottom");
        Material topOff = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top_off");
        Material topOn = TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_top");
        Identifier[] chargeLevelModels = new Identifier[5];
        for (int i2 = 0; i2 < 5; ++i2) {
            TextureMapping mapping = new TextureMapping().put(TextureSlot.BOTTOM, bottom).put(TextureSlot.TOP, i2 == 0 ? topOff : topOn).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.RESPAWN_ANCHOR, "_side" + i2));
            chargeLevelModels[i2] = ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(Blocks.RESPAWN_ANCHOR, "_" + i2, mapping, this.modelOutput);
        }
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.RESPAWN_ANCHOR).with(PropertyDispatch.initial(BlockStateProperties.RESPAWN_ANCHOR_CHARGES).generate(i -> BlockModelGenerators.plainVariant(chargeLevelModels[i]))));
        this.registerSimpleItemModel(Blocks.RESPAWN_ANCHOR, chargeLevelModels[0]);
    }

    private static VariantMutator applyRotation(FrontAndTop orientation) {
        return switch (orientation) {
            default -> throw new MatchException(null, null);
            case FrontAndTop.DOWN_NORTH -> X_ROT_90;
            case FrontAndTop.DOWN_SOUTH -> X_ROT_90.then(Y_ROT_180);
            case FrontAndTop.DOWN_WEST -> X_ROT_90.then(Y_ROT_270);
            case FrontAndTop.DOWN_EAST -> X_ROT_90.then(Y_ROT_90);
            case FrontAndTop.UP_NORTH -> X_ROT_270.then(Y_ROT_180);
            case FrontAndTop.UP_SOUTH -> X_ROT_270;
            case FrontAndTop.UP_WEST -> X_ROT_270.then(Y_ROT_90);
            case FrontAndTop.UP_EAST -> X_ROT_270.then(Y_ROT_270);
            case FrontAndTop.NORTH_UP -> NOP;
            case FrontAndTop.SOUTH_UP -> Y_ROT_180;
            case FrontAndTop.WEST_UP -> Y_ROT_270;
            case FrontAndTop.EAST_UP -> Y_ROT_90;
        };
    }

    private void createJigsaw() {
        Material front = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_top");
        Material back = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_bottom");
        Material side = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_side");
        Material lock = TextureMapping.getBlockTexture(Blocks.JIGSAW, "_lock");
        TextureMapping mapping = new TextureMapping().put(TextureSlot.DOWN, side).put(TextureSlot.WEST, side).put(TextureSlot.EAST, side).put(TextureSlot.PARTICLE, front).put(TextureSlot.NORTH, front).put(TextureSlot.SOUTH, back).put(TextureSlot.UP, lock);
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.JIGSAW, BlockModelGenerators.plainVariant(ModelTemplates.CUBE_DIRECTIONAL.create(Blocks.JIGSAW, mapping, this.modelOutput))).with(PropertyDispatch.modify(BlockStateProperties.ORIENTATION).generate(BlockModelGenerators::applyRotation)));
    }

    private void createPetrifiedOakSlab() {
        Block fullBlock = Blocks.OAK_PLANKS;
        MultiVariant fullBlockModel = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(fullBlock));
        TextureMapping fullBlockTextures = TextureMapping.cube(fullBlock);
        Block petrifiedSlab = Blocks.PETRIFIED_OAK_SLAB;
        MultiVariant petrifiedSlabBottom = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_BOTTOM.create(petrifiedSlab, fullBlockTextures, this.modelOutput));
        MultiVariant petrifiedSlabTop = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_TOP.create(petrifiedSlab, fullBlockTextures, this.modelOutput));
        this.blockStateOutput.accept(BlockModelGenerators.createSlab(petrifiedSlab, petrifiedSlabBottom, petrifiedSlabTop, fullBlockModel));
    }

    private void createHead(Block standAlone, Block wall, SkullBlock.Type skullType, Identifier itemBase) {
        MultiVariant blockModel = BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("skull"));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(standAlone, blockModel));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(wall, blockModel));
        if (skullType == SkullBlock.Types.PLAYER) {
            this.itemModelOutput.accept(standAlone.asItem(), ItemModelUtils.specialModel(itemBase, SKULL_TRANSFORM, new PlayerHeadSpecialRenderer.Unbaked()));
        } else {
            this.itemModelOutput.accept(standAlone.asItem(), ItemModelUtils.specialModel(itemBase, SKULL_TRANSFORM, new SkullSpecialRenderer.Unbaked(skullType)));
        }
    }

    private void createHeads() {
        Identifier defaultHeadItemBase = ModelLocationUtils.decorateItemModelLocation("template_skull");
        this.createHead(Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, SkullBlock.Types.CREEPER, defaultHeadItemBase);
        this.createHead(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, SkullBlock.Types.PLAYER, defaultHeadItemBase);
        this.createHead(Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, SkullBlock.Types.ZOMBIE, defaultHeadItemBase);
        this.createHead(Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL, SkullBlock.Types.SKELETON, defaultHeadItemBase);
        this.createHead(Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, SkullBlock.Types.WITHER_SKELETON, defaultHeadItemBase);
        this.createHead(Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD, SkullBlock.Types.PIGLIN, defaultHeadItemBase);
        this.createHead(Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD, SkullBlock.Types.DRAGON, ModelLocationUtils.getModelLocation(Items.DRAGON_HEAD));
    }

    private void createCopperGolemStatues() {
        WeatheringCopper.WeatherState.forEach(state -> {
            this.createCopperGolemStatue(Blocks.COPPER_GOLEM_STATUE.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_BLOCK.weathering().pick((WeatheringCopper.WeatherState)state), (WeatheringCopper.WeatherState)state);
            this.copyModel(Blocks.COPPER_GOLEM_STATUE.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_GOLEM_STATUE.waxed().pick((WeatheringCopper.WeatherState)state));
        });
    }

    private void createCopperGolemStatue(Block block, Block particle, WeatheringCopper.WeatherState state) {
        MultiVariant blockModel = BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(TextureMapping.getBlockTexture(particle)), this.modelOutput));
        Identifier itemBase = ModelLocationUtils.decorateItemModelLocation("template_copper_golem_statue");
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, blockModel));
        this.itemModelOutput.accept(block.asItem(), ItemModelUtils.selectBlockItemProperty(new Transformation((Vector3fc)new Vector3f(0.5f, 1.5f, 0.5f), null, (Vector3fc)new Vector3f(1.0f, -1.0f, -1.0f), null), CopperGolemStatueBlock.POSE, ItemModelUtils.specialModel(itemBase, new CopperGolemStatueSpecialRenderer.Unbaked(state, CopperGolemStatueBlock.Pose.STANDING)), Map.of(CopperGolemStatueBlock.Pose.SITTING, ItemModelUtils.specialModel(itemBase, new CopperGolemStatueSpecialRenderer.Unbaked(state, CopperGolemStatueBlock.Pose.SITTING)), CopperGolemStatueBlock.Pose.STAR, ItemModelUtils.specialModel(itemBase, new CopperGolemStatueSpecialRenderer.Unbaked(state, CopperGolemStatueBlock.Pose.STAR)), CopperGolemStatueBlock.Pose.RUNNING, ItemModelUtils.specialModel(itemBase, new CopperGolemStatueSpecialRenderer.Unbaked(state, CopperGolemStatueBlock.Pose.RUNNING)))));
    }

    private void createBanner(DyeColor baseColor) {
        Block standAlone = Blocks.BANNER.pick(baseColor);
        Block wall = Blocks.WALL_BANNER.pick(baseColor);
        MultiVariant blockModel = BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("banner"));
        Identifier itemModel = ModelLocationUtils.decorateItemModelLocation("template_banner");
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(standAlone, blockModel));
        this.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(wall, blockModel));
        Item item = standAlone.asItem();
        this.itemModelOutput.accept(item, ItemModelUtils.specialModel(itemModel, BannerRenderer.TRANSFORMATIONS.freeTransformations(0), new BannerSpecialRenderer.Unbaked(baseColor, BannerBlock.AttachmentType.GROUND)));
    }

    private void createChest(Block block, Block particle, Identifier texture, boolean hasGiftVariant) {
        this.createParticleOnlyBlock(block, particle);
        Item chestItem = block.asItem();
        Identifier itemModelBase = ModelTemplates.CHEST_INVENTORY.create(chestItem, TextureMapping.particle(particle), this.modelOutput);
        ItemModel.Unbaked plainModel = ItemModelUtils.specialModel(itemModelBase, new ChestSpecialRenderer.Unbaked(texture));
        if (hasGiftVariant) {
            ItemModel.Unbaked giftModel = ItemModelUtils.specialModel(itemModelBase, new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.CHRISTMAS.single()));
            this.itemModelOutput.accept(chestItem, ItemModelUtils.isXmas(giftModel, plainModel));
        } else {
            this.itemModelOutput.accept(chestItem, plainModel);
        }
    }

    private void createChest(Block block, Block particle, MultiblockChestResources<Identifier> textures, boolean hasGiftVariant) {
        this.createChest(block, particle, textures.single(), hasGiftVariant);
    }

    private void createChests() {
        this.createChest(Blocks.CHEST, Blocks.OAK_PLANKS, ChestSpecialRenderer.REGULAR, true);
        this.createChest(Blocks.TRAPPED_CHEST, Blocks.OAK_PLANKS, ChestSpecialRenderer.TRAPPED, true);
        this.createChest(Blocks.ENDER_CHEST, Blocks.OBSIDIAN, ChestSpecialRenderer.ENDER_CHEST, false);
    }

    private void createCopperChests() {
        WeatheringCopper.WeatherState.forEach(state -> {
            this.createChest(Blocks.COPPER_CHEST.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_BLOCK.weathering().pick((WeatheringCopper.WeatherState)state), ChestSpecialRenderer.COPPER.pick((WeatheringCopper.WeatherState)state), false);
            this.copyModel(Blocks.COPPER_CHEST.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_CHEST.waxed().pick((WeatheringCopper.WeatherState)state));
        });
    }

    private void createBed(DyeColor dyeColor) {
        Block bed = Blocks.BED.pick(dyeColor);
        Identifier head = ModelTemplates.BED_HEAD.createWithSuffix(bed, "_" + String.valueOf(BedPart.HEAD), TextureMapping.bed(bed, BedPart.HEAD), this.modelOutput);
        Identifier foot = ModelTemplates.BED_FOOT.createWithSuffix(bed, "_" + String.valueOf(BedPart.FOOT), TextureMapping.bed(bed, BedPart.FOOT), this.modelOutput);
        MultiVariant blockModelHead = BlockModelGenerators.plainVariant(head);
        MultiVariant blockModelFoot = BlockModelGenerators.plainVariant(foot);
        this.blockStateOutput.accept(BlockModelGenerators.createBed(bed, blockModelHead, blockModelFoot));
        Transformation footTransformation = new Transformation((Vector3fc)new Vector3f(0.0f, 0.0f, 1.0f), null, null, null);
        ItemModel.Unbaked itemModelHead = ItemModelUtils.plainModel(head);
        ItemModel.Unbaked itemModelFoot = ItemModelUtils.plainModel(foot, footTransformation);
        this.itemModelOutput.accept(bed.asItem(), ItemModelUtils.composite(itemModelHead, itemModelFoot));
    }

    private void generateSimpleSpecialItemModel(Block block, Optional<Transformation> transformation, SpecialModelRenderer.Unbaked<?> specialModel) {
        Item item = block.asItem();
        Identifier harcodedModelBase = ModelLocationUtils.getModelLocation(item);
        this.itemModelOutput.accept(item, ItemModelUtils.specialModel(harcodedModelBase, transformation, specialModel));
    }

    public void run() {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateModel).forEach(blockFamily -> this.family(blockFamily.getBaseBlock()).generateFor((BlockFamily)blockFamily));
        WeatheringCopper.WeatherState.forEach(state -> this.family(Blocks.CUT_COPPER.weathering().pick((WeatheringCopper.WeatherState)state)).generateFor(BlockFamilies.CUT_COPPER.weathering().pick((WeatheringCopper.WeatherState)state)).donateModelTo(Blocks.CUT_COPPER.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.CUT_COPPER.waxed().pick((WeatheringCopper.WeatherState)state)).donateModelTo(Blocks.CHISELED_COPPER.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.CHISELED_COPPER.waxed().pick((WeatheringCopper.WeatherState)state)).generateFor(BlockFamilies.CUT_COPPER.waxed().pick((WeatheringCopper.WeatherState)state)));
        Blocks.COPPER_BULB.zipUnwaxedWaxed((unwaxed, waxed) -> {
            this.createCopperBulb((Block)unwaxed);
            this.copyCopperBulbModel((Block)unwaxed, (Block)waxed);
        });
        this.createNonTemplateModelBlock(Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.CAVE_AIR, Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.VOID_AIR, Blocks.AIR);
        this.createNonTemplateModelBlock(Blocks.BEACON);
        this.createNonTemplateModelBlock(Blocks.CACTUS);
        this.createNonTemplateModelBlock(Blocks.BUBBLE_COLUMN, Blocks.WATER);
        this.createNonTemplateModelBlock(Blocks.DRAGON_EGG);
        this.createNonTemplateModelBlock(Blocks.DRIED_KELP_BLOCK);
        this.createNonTemplateModelBlock(Blocks.ENCHANTING_TABLE);
        this.createNonTemplateModelBlock(Blocks.FLOWER_POT);
        this.registerSimpleFlatItemModel(Items.FLOWER_POT);
        this.createNonTemplateModelBlock(Blocks.HONEY_BLOCK);
        this.createNonTemplateModelBlock(Blocks.WATER);
        this.createNonTemplateModelBlock(Blocks.LAVA);
        this.createNonTemplateModelBlock(Blocks.SLIME_BLOCK);
        this.registerSimpleFlatItemModel(Items.IRON_CHAIN);
        Items.COPPER_CHAIN.zipUnwaxedWaxed(this::createCopperChainItem);
        ColorCollection.zipApply(Blocks.DYED_CANDLE, Blocks.DYED_CANDLE_CAKE, this::createCandleAndCandleCake);
        this.createCandleAndCandleCake(Blocks.CANDLE, Blocks.CANDLE_CAKE);
        this.createNonTemplateModelBlock(Blocks.POTTED_BAMBOO);
        this.createNonTemplateModelBlock(Blocks.POTTED_CACTUS);
        this.createNonTemplateModelBlock(Blocks.POWDER_SNOW);
        this.createNonTemplateModelBlock(Blocks.SPORE_BLOSSOM);
        this.createAzalea(Blocks.AZALEA);
        this.createAzalea(Blocks.FLOWERING_AZALEA);
        this.createPottedAzalea(Blocks.POTTED_AZALEA);
        this.createPottedAzalea(Blocks.POTTED_FLOWERING_AZALEA);
        this.createCaveVines();
        this.createFullAndCarpetBlocks(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET);
        this.createMossyCarpet(Blocks.PALE_MOSS_CARPET);
        this.createHangingMoss(Blocks.PALE_HANGING_MOSS);
        this.createTrivialCube(Blocks.PALE_MOSS_BLOCK);
        this.createFlowerBed(Blocks.PINK_PETALS);
        this.createFlowerBed(Blocks.WILDFLOWERS);
        this.createLeafLitter(Blocks.LEAF_LITTER);
        this.createCrossBlock(Blocks.FIREFLY_BUSH, PlantType.EMISSIVE_NOT_TINTED);
        this.registerSimpleFlatItemModel(Items.FIREFLY_BUSH);
        this.createAirLikeBlock(Blocks.BARRIER, Items.BARRIER);
        this.registerSimpleFlatItemModel(Items.BARRIER);
        this.createLightBlock();
        this.createAirLikeBlock(Blocks.STRUCTURE_VOID, Items.STRUCTURE_VOID);
        this.registerSimpleFlatItemModel(Items.STRUCTURE_VOID);
        this.createAirLikeBlock(Blocks.MOVING_PISTON, TextureMapping.getBlockTexture(Blocks.PISTON, "_side"));
        this.createTrivialCube(Blocks.COAL_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_COAL_ORE);
        this.createTrivialCube(Blocks.COAL_BLOCK);
        this.createTrivialCube(Blocks.DIAMOND_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_DIAMOND_ORE);
        this.createTrivialCube(Blocks.DIAMOND_BLOCK);
        this.createTrivialCube(Blocks.EMERALD_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_EMERALD_ORE);
        this.createTrivialCube(Blocks.EMERALD_BLOCK);
        this.createTrivialCube(Blocks.GOLD_ORE);
        this.createTrivialCube(Blocks.NETHER_GOLD_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_GOLD_ORE);
        this.createTrivialCube(Blocks.GOLD_BLOCK);
        this.createTrivialCube(Blocks.IRON_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_IRON_ORE);
        this.createTrivialCube(Blocks.IRON_BLOCK);
        this.createTrivialBlock(Blocks.ANCIENT_DEBRIS, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.NETHERITE_BLOCK);
        this.createTrivialCube(Blocks.LAPIS_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_LAPIS_ORE);
        this.createTrivialCube(Blocks.LAPIS_BLOCK);
        this.createTrivialCube(Blocks.RESIN_BLOCK);
        this.createTrivialCube(Blocks.NETHER_QUARTZ_ORE);
        this.createTrivialCube(Blocks.REDSTONE_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_REDSTONE_ORE);
        this.createTrivialCube(Blocks.REDSTONE_BLOCK);
        this.createTrivialCube(Blocks.GILDED_BLACKSTONE);
        this.createTrivialCube(Blocks.BLUE_ICE);
        this.createTrivialCube(Blocks.CLAY);
        this.createTrivialCube(Blocks.COARSE_DIRT);
        this.createTrivialCube(Blocks.CRYING_OBSIDIAN);
        this.createTrivialCube(Blocks.GLOWSTONE);
        this.createTrivialCube(Blocks.GRAVEL);
        this.createTrivialCube(Blocks.HONEYCOMB_BLOCK);
        this.createTrivialCube(Blocks.ICE);
        this.createTrivialBlock(Blocks.JUKEBOX, TexturedModel.CUBE_TOP);
        this.createTrivialBlock(Blocks.LODESTONE, TexturedModel.COLUMN);
        this.createTrivialBlock(Blocks.MELON, TexturedModel.COLUMN);
        this.createNonTemplateModelBlock(Blocks.MANGROVE_ROOTS);
        this.createNonTemplateModelBlock(Blocks.POTTED_MANGROVE_PROPAGULE);
        this.createTrivialCube(Blocks.NETHER_WART_BLOCK);
        this.createTrivialCube(Blocks.NOTE_BLOCK);
        this.createTrivialCube(Blocks.PACKED_ICE);
        this.createTrivialCube(Blocks.OBSIDIAN);
        this.createTrivialCube(Blocks.SEA_LANTERN);
        this.createTrivialCube(Blocks.SHROOMLIGHT);
        this.createTrivialCube(Blocks.SOUL_SAND);
        this.createTrivialCube(Blocks.SOUL_SOIL);
        this.createTrivialBlock(Blocks.SPAWNER, TexturedModel.CUBE_INNER_FACES);
        this.createCreakingHeart(Blocks.CREAKING_HEART);
        this.createTrivialCube(Blocks.SPONGE);
        this.createTrivialBlock(Blocks.SEAGRASS, TexturedModel.SEAGRASS);
        this.registerSimpleFlatItemModel(Items.SEAGRASS);
        this.createTrivialBlock(Blocks.TNT, TexturedModel.CUBE_TOP_BOTTOM);
        this.createTrivialBlock(Blocks.TARGET, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.WARPED_WART_BLOCK);
        this.createTrivialCube(Blocks.WET_SPONGE);
        this.createTrivialCube(Blocks.AMETHYST_BLOCK);
        this.createTrivialCube(Blocks.BUDDING_AMETHYST);
        this.createTrivialCube(Blocks.CALCITE);
        this.createTrivialCube(Blocks.DRIPSTONE_BLOCK);
        this.createTrivialCube(Blocks.RAW_IRON_BLOCK);
        this.createTrivialCube(Blocks.RAW_COPPER_BLOCK);
        this.createTrivialCube(Blocks.RAW_GOLD_BLOCK);
        this.createRotatedMirroredVariantBlock(Blocks.SCULK);
        this.createNonTemplateModelBlock(Blocks.HEAVY_CORE);
        this.createPetrifiedOakSlab();
        this.createTrivialCube(Blocks.COPPER_ORE);
        this.createTrivialCube(Blocks.DEEPSLATE_COPPER_ORE);
        Blocks.COPPER_BLOCK.zipUnwaxedWaxed((unwaxed, block) -> this.createTrivialCube((Block)unwaxed));
        Blocks.COPPER_BLOCK.zipUnwaxedWaxed(this::copyModel);
        Blocks.COPPER_DOOR.zipUnwaxedWaxed((unwaxed, block) -> this.createDoor((Block)unwaxed));
        Blocks.COPPER_DOOR.zipUnwaxedWaxed(this::copyDoorModel);
        Blocks.COPPER_TRAPDOOR.zipUnwaxedWaxed((unwaxed, block) -> this.createTrapdoor((Block)unwaxed));
        Blocks.COPPER_TRAPDOOR.zipUnwaxedWaxed(this::copyTrapdoorModel);
        Blocks.COPPER_GRATE.zipUnwaxedWaxed((unwaxed, block) -> this.createTrivialCube((Block)unwaxed));
        Blocks.COPPER_GRATE.zipUnwaxedWaxed(this::copyModel);
        Blocks.LIGHTNING_ROD.zipUnwaxedWaxed(this::createLightningRod);
        this.createWeightedPressurePlate(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.GOLD_BLOCK);
        this.createWeightedPressurePlate(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.IRON_BLOCK);
        this.createShelf(Blocks.ACACIA_SHELF, Blocks.STRIPPED_ACACIA_LOG);
        this.createShelf(Blocks.BAMBOO_SHELF, Blocks.STRIPPED_BAMBOO_BLOCK);
        this.createShelf(Blocks.BIRCH_SHELF, Blocks.STRIPPED_BIRCH_LOG);
        this.createShelf(Blocks.CHERRY_SHELF, Blocks.STRIPPED_CHERRY_LOG);
        this.createShelf(Blocks.CRIMSON_SHELF, Blocks.STRIPPED_CRIMSON_STEM);
        this.createShelf(Blocks.DARK_OAK_SHELF, Blocks.STRIPPED_DARK_OAK_LOG);
        this.createShelf(Blocks.JUNGLE_SHELF, Blocks.STRIPPED_JUNGLE_LOG);
        this.createShelf(Blocks.MANGROVE_SHELF, Blocks.STRIPPED_MANGROVE_LOG);
        this.createShelf(Blocks.OAK_SHELF, Blocks.STRIPPED_OAK_LOG);
        this.createShelf(Blocks.PALE_OAK_SHELF, Blocks.STRIPPED_PALE_OAK_LOG);
        this.createShelf(Blocks.SPRUCE_SHELF, Blocks.STRIPPED_SPRUCE_LOG);
        this.createShelf(Blocks.WARPED_SHELF, Blocks.STRIPPED_WARPED_STEM);
        this.createAmethystClusters();
        this.createBookshelf();
        this.createChiseledBookshelf();
        this.createBrewingStand();
        this.createCakeBlock();
        this.createCampfires(Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
        this.createCartographyTable();
        this.createCauldrons();
        this.createChorusFlower();
        this.createChorusPlant();
        this.createComposter();
        this.createDaylightDetector();
        this.createEndPortalFrame();
        this.createRotatableColumn(Blocks.END_ROD);
        this.createFarmland();
        this.createFire();
        this.createSoulFire();
        this.createFrostedIce();
        this.createGrassBlocks();
        this.createCocoa();
        this.createDirtPath();
        this.createGrindstone();
        this.createHopper();
        this.createBarsAndItem(Blocks.IRON_BARS);
        Blocks.COPPER_BARS.zipUnwaxedWaxed(this::createBarsAndItem);
        this.createLever();
        this.createLilyPad();
        this.createNetherPortalBlock();
        this.createNetherrack();
        this.createObserver();
        this.createPistons();
        this.createPistonHeads();
        this.createScaffolding();
        this.createRedstoneTorch();
        this.createRedstoneLamp();
        this.createRepeater();
        this.createSeaPickle();
        this.createSmithingTable();
        this.createSnowBlocks();
        this.createStonecutter();
        this.createStructureBlock();
        this.createSweetBerryBush();
        this.createTestBlock();
        this.createTrivialCube(Blocks.TEST_INSTANCE_BLOCK);
        this.createTripwire();
        this.createTripwireHook();
        this.createTurtleEgg();
        this.createSnifferEgg();
        this.createDriedGhastBlock();
        this.createVine();
        this.createMultiface(Blocks.GLOW_LICHEN);
        this.createMultiface(Blocks.SCULK_VEIN);
        this.createMultiface(Blocks.RESIN_CLUMP, Items.RESIN_CLUMP);
        this.createMagmaBlock();
        this.createJigsaw();
        this.createSculkSensor();
        this.createCalibratedSculkSensor();
        this.createSculkShrieker();
        this.createFrogspawnBlock();
        this.createMangrovePropagule();
        this.createMuddyMangroveRoots();
        this.createTrialSpawner();
        this.createVault();
        this.createNonTemplateHorizontalBlock(Blocks.LADDER);
        this.registerSimpleFlatItemModel(Blocks.LADDER);
        this.createNonTemplateHorizontalBlock(Blocks.LECTERN);
        this.createBigDripLeafBlock();
        this.createNonTemplateHorizontalBlock(Blocks.BIG_DRIPLEAF_STEM);
        this.createNormalTorch(Blocks.TORCH, Blocks.WALL_TORCH);
        this.createNormalTorch(Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH);
        this.createNormalTorch(Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH);
        this.createCraftingTableLike(Blocks.CRAFTING_TABLE, Blocks.OAK_PLANKS, TextureMapping::craftingTable);
        this.createCraftingTableLike(Blocks.FLETCHING_TABLE, Blocks.BIRCH_PLANKS, TextureMapping::fletchingTable);
        this.createNyliumBlock(Blocks.CRIMSON_NYLIUM);
        this.createNyliumBlock(Blocks.WARPED_NYLIUM);
        this.createDispenserBlock(Blocks.DISPENSER);
        this.createDispenserBlock(Blocks.DROPPER);
        this.createCrafterBlock();
        this.createLantern(Blocks.LANTERN);
        this.createLantern(Blocks.SOUL_LANTERN);
        Blocks.COPPER_LANTERN.zipUnwaxedWaxed(this::createCopperLantern);
        this.createAxisAlignedPillarBlockCustomModel(Blocks.IRON_CHAIN, BlockModelGenerators.plainVariant(TexturedModel.CHAIN.create(Blocks.IRON_CHAIN, this.modelOutput)));
        Blocks.COPPER_CHAIN.zipUnwaxedWaxed(this::createCopperChain);
        this.createAxisAlignedPillarBlock(Blocks.BASALT, TexturedModel.COLUMN);
        this.createAxisAlignedPillarBlock(Blocks.POLISHED_BASALT, TexturedModel.COLUMN);
        this.createTrivialCube(Blocks.SMOOTH_BASALT);
        this.createAxisAlignedPillarBlock(Blocks.BONE_BLOCK, TexturedModel.COLUMN);
        this.createRotatedVariantBlock(Blocks.DIRT);
        this.createRotatedVariantBlock(Blocks.ROOTED_DIRT);
        this.createRotatedVariantBlock(Blocks.SAND);
        this.createBrushableBlock(Blocks.SUSPICIOUS_SAND);
        this.createBrushableBlock(Blocks.SUSPICIOUS_GRAVEL);
        this.createRotatedVariantBlock(Blocks.RED_SAND);
        this.createRotatedMirroredVariantBlock(Blocks.BEDROCK);
        this.createTrivialBlock(Blocks.REINFORCED_DEEPSLATE, TexturedModel.CUBE_TOP_BOTTOM);
        this.createRotatedPillarWithHorizontalVariant(Blocks.HAY_BLOCK, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        this.createRotatedPillarWithHorizontalVariant(Blocks.OCHRE_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        this.createRotatedPillarWithHorizontalVariant(Blocks.VERDANT_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        this.createRotatedPillarWithHorizontalVariant(Blocks.PEARLESCENT_FROGLIGHT, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        this.createHorizontallyRotatedBlock(Blocks.LOOM, TexturedModel.ORIENTABLE);
        this.createPumpkins();
        this.createBeeNest(Blocks.BEE_NEST, TextureMapping::orientableCube);
        this.createBeeNest(Blocks.BEEHIVE, TextureMapping::orientableCubeSameEnds);
        this.createCropBlock(Blocks.BEETROOTS, BlockStateProperties.AGE_3, 0, 1, 2, 3);
        this.createCropBlock(Blocks.CARROTS, BlockStateProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
        this.createCropBlock(Blocks.NETHER_WART, BlockStateProperties.AGE_3, 0, 1, 1, 2);
        this.createCropBlock(Blocks.POTATOES, BlockStateProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
        this.createCropBlock(Blocks.WHEAT, BlockStateProperties.AGE_7, 0, 1, 2, 3, 4, 5, 6, 7);
        this.createCrossBlock(Blocks.TORCHFLOWER_CROP, PlantType.NOT_TINTED, BlockStateProperties.AGE_1, 0, 1);
        this.createPitcherCrop();
        this.createPitcherPlant();
        DyeColor.VALUES.forEach(this::createBanner);
        DyeColor.VALUES.forEach(this::createBed);
        this.createHeads();
        this.createChests();
        this.createCopperChests();
        this.createShulkerBox(Blocks.SHULKER_BOX, null);
        ColorCollection.zipApply(ColorCollection.VALUES, Blocks.DYED_SHULKER_BOX, (color, box) -> this.createShulkerBox((Block)box, (DyeColor)color));
        this.createCopperGolemStatues();
        this.createParticleOnlyBlock(Blocks.CONDUIT);
        this.generateSimpleSpecialItemModel(Blocks.CONDUIT, Optional.of(ConduitRenderer.DEFAULT_TRANSFORMATION), new ConduitSpecialRenderer.Unbaked());
        this.createParticleOnlyBlock(Blocks.DECORATED_POT, Blocks.TERRACOTTA);
        this.generateSimpleSpecialItemModel(Blocks.DECORATED_POT, Optional.empty(), new DecoratedPotSpecialRenderer.Unbaked());
        this.createParticleOnlyBlock(Blocks.END_PORTAL, Blocks.OBSIDIAN);
        this.createParticleOnlyBlock(Blocks.END_GATEWAY, Blocks.OBSIDIAN);
        this.createTrivialCube(Blocks.AZALEA_LEAVES);
        this.createTrivialCube(Blocks.FLOWERING_AZALEA_LEAVES);
        Blocks.CONCRETE.forEach(this::createTrivialCube);
        this.createColoredBlockWithRandomRotations(TexturedModel.CUBE, Blocks.CONCRETE_POWDER.asList());
        this.createTrivialCube(Blocks.POTENT_SULFUR);
        this.createTrivialCube(Blocks.TERRACOTTA);
        Blocks.DYED_TERRACOTTA.forEach(this::createTrivialCube);
        this.createTrivialCube(Blocks.TINTED_GLASS);
        this.createGlassBlocks(Blocks.GLASS, Blocks.GLASS_PANE);
        ColorCollection.zipApply(Blocks.STAINED_GLASS, Blocks.STAINED_GLASS_PANE, this::createGlassBlocks);
        this.createColoredBlockWithStateRotations(TexturedModel.GLAZED_TERRACOTTA, Blocks.GLAZED_TERRACOTTA.asList());
        ColorCollection.zipApply(Blocks.WOOL, Blocks.CARPET, this::createFullAndCarpetBlocks);
        this.createTrivialCube(Blocks.MUD);
        this.createTrivialCube(Blocks.PACKED_MUD);
        this.createPlant(Blocks.FERN, Blocks.POTTED_FERN, PlantType.TINTED);
        this.createItemWithGrassTint(Blocks.FERN);
        this.createPlantWithDefaultItem(Blocks.DANDELION, Blocks.POTTED_DANDELION, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.GOLDEN_DANDELION, Blocks.POTTED_GOLDEN_DANDELION, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.POPPY, Blocks.POTTED_POPPY, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.OPEN_EYEBLOSSOM, Blocks.POTTED_OPEN_EYEBLOSSOM, PlantType.EMISSIVE_NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.CLOSED_EYEBLOSSOM, Blocks.POTTED_CLOSED_EYEBLOSSOM, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.BLUE_ORCHID, Blocks.POTTED_BLUE_ORCHID, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.ALLIUM, Blocks.POTTED_ALLIUM, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.AZURE_BLUET, Blocks.POTTED_AZURE_BLUET, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.RED_TULIP, Blocks.POTTED_RED_TULIP, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.ORANGE_TULIP, Blocks.POTTED_ORANGE_TULIP, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.WHITE_TULIP, Blocks.POTTED_WHITE_TULIP, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.PINK_TULIP, Blocks.POTTED_PINK_TULIP, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.OXEYE_DAISY, Blocks.POTTED_OXEYE_DAISY, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.CORNFLOWER, Blocks.POTTED_CORNFLOWER, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.RED_MUSHROOM, Blocks.POTTED_RED_MUSHROOM, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.BROWN_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.DEAD_BUSH, Blocks.POTTED_DEAD_BUSH, PlantType.NOT_TINTED);
        this.createPlantWithDefaultItem(Blocks.TORCHFLOWER, Blocks.POTTED_TORCHFLOWER, PlantType.NOT_TINTED);
        this.createSpeleothem(Blocks.POINTED_DRIPSTONE);
        this.createSpeleothem(Blocks.SULFUR_SPIKE);
        this.createMushroomBlock(Blocks.BROWN_MUSHROOM_BLOCK);
        this.createMushroomBlock(Blocks.RED_MUSHROOM_BLOCK);
        this.createMushroomBlock(Blocks.MUSHROOM_STEM);
        this.createCrossBlock(Blocks.SHORT_GRASS, PlantType.TINTED);
        this.createItemWithGrassTint(Blocks.SHORT_GRASS);
        this.createCrossBlockWithDefaultItem(Blocks.SHORT_DRY_GRASS, PlantType.NOT_TINTED);
        this.createCrossBlockWithDefaultItem(Blocks.TALL_DRY_GRASS, PlantType.NOT_TINTED);
        this.createCrossBlock(Blocks.BUSH, PlantType.TINTED);
        this.createItemWithGrassTint(Blocks.BUSH);
        this.createCrossBlock(Blocks.SUGAR_CANE, PlantType.TINTED);
        this.registerSimpleFlatItemModel(Items.SUGAR_CANE);
        this.createGrowingPlant(Blocks.KELP, Blocks.KELP_PLANT, PlantType.NOT_TINTED);
        this.registerSimpleFlatItemModel(Items.KELP);
        this.createCrossBlock(Blocks.HANGING_ROOTS, PlantType.NOT_TINTED);
        this.createGrowingPlant(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, PlantType.NOT_TINTED);
        this.createGrowingPlant(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, PlantType.NOT_TINTED);
        this.registerSimpleFlatItemModel(Blocks.WEEPING_VINES, "_plant");
        this.registerSimpleFlatItemModel(Blocks.TWISTING_VINES, "_plant");
        this.createCrossBlockWithDefaultItem(Blocks.BAMBOO_SAPLING, PlantType.TINTED, TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.BAMBOO, "_stage0")));
        this.createBamboo();
        this.createCrossBlockWithDefaultItem(Blocks.CACTUS_FLOWER, PlantType.NOT_TINTED);
        this.createCrossBlockWithDefaultItem(Blocks.COBWEB, PlantType.NOT_TINTED);
        this.createDoublePlantWithDefaultItem(Blocks.LILAC, PlantType.NOT_TINTED);
        this.createDoublePlantWithDefaultItem(Blocks.ROSE_BUSH, PlantType.NOT_TINTED);
        this.createDoublePlantWithDefaultItem(Blocks.PEONY, PlantType.NOT_TINTED);
        this.createTintedDoublePlant(Blocks.TALL_GRASS);
        this.createTintedDoublePlant(Blocks.LARGE_FERN);
        this.createSunflower();
        this.createTallSeagrass();
        this.createSmallDripleaf();
        this.createCoral(Blocks.TUBE_CORAL, Blocks.DEAD_TUBE_CORAL, Blocks.TUBE_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK, Blocks.TUBE_CORAL_FAN, Blocks.DEAD_TUBE_CORAL_FAN, Blocks.TUBE_CORAL_WALL_FAN, Blocks.DEAD_TUBE_CORAL_WALL_FAN);
        this.createCoral(Blocks.BRAIN_CORAL, Blocks.DEAD_BRAIN_CORAL, Blocks.BRAIN_CORAL_BLOCK, Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.BRAIN_CORAL_FAN, Blocks.DEAD_BRAIN_CORAL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.DEAD_BRAIN_CORAL_WALL_FAN);
        this.createCoral(Blocks.BUBBLE_CORAL, Blocks.DEAD_BUBBLE_CORAL, Blocks.BUBBLE_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK, Blocks.BUBBLE_CORAL_FAN, Blocks.DEAD_BUBBLE_CORAL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.DEAD_BUBBLE_CORAL_WALL_FAN);
        this.createCoral(Blocks.FIRE_CORAL, Blocks.DEAD_FIRE_CORAL, Blocks.FIRE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.FIRE_CORAL_FAN, Blocks.DEAD_FIRE_CORAL_FAN, Blocks.FIRE_CORAL_WALL_FAN, Blocks.DEAD_FIRE_CORAL_WALL_FAN);
        this.createCoral(Blocks.HORN_CORAL, Blocks.DEAD_HORN_CORAL, Blocks.HORN_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.HORN_CORAL_FAN, Blocks.DEAD_HORN_CORAL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.DEAD_HORN_CORAL_WALL_FAN);
        this.createStems(Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM);
        this.createStems(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
        this.woodProvider(Blocks.MANGROVE_LOG).logWithHorizontal(Blocks.MANGROVE_LOG).wood(Blocks.MANGROVE_WOOD);
        this.woodProvider(Blocks.STRIPPED_MANGROVE_LOG).logWithHorizontal(Blocks.STRIPPED_MANGROVE_LOG).wood(Blocks.STRIPPED_MANGROVE_WOOD);
        this.createTintedLeaves(Blocks.MANGROVE_LEAVES, TexturedModel.LEAVES, -7158200);
        this.woodProvider(Blocks.ACACIA_LOG).logWithHorizontal(Blocks.ACACIA_LOG).wood(Blocks.ACACIA_WOOD);
        this.woodProvider(Blocks.STRIPPED_ACACIA_LOG).logWithHorizontal(Blocks.STRIPPED_ACACIA_LOG).wood(Blocks.STRIPPED_ACACIA_WOOD);
        this.createPlantWithDefaultItem(Blocks.ACACIA_SAPLING, Blocks.POTTED_ACACIA_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.ACACIA_LEAVES, TexturedModel.LEAVES, -12012264);
        this.woodProvider(Blocks.CHERRY_LOG).logUVLocked(Blocks.CHERRY_LOG).wood(Blocks.CHERRY_WOOD);
        this.woodProvider(Blocks.STRIPPED_CHERRY_LOG).logUVLocked(Blocks.STRIPPED_CHERRY_LOG).wood(Blocks.STRIPPED_CHERRY_WOOD);
        this.createPlantWithDefaultItem(Blocks.CHERRY_SAPLING, Blocks.POTTED_CHERRY_SAPLING, PlantType.NOT_TINTED);
        this.createTrivialBlock(Blocks.CHERRY_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.BIRCH_LOG).logWithHorizontal(Blocks.BIRCH_LOG).wood(Blocks.BIRCH_WOOD);
        this.woodProvider(Blocks.STRIPPED_BIRCH_LOG).logWithHorizontal(Blocks.STRIPPED_BIRCH_LOG).wood(Blocks.STRIPPED_BIRCH_WOOD);
        this.createPlantWithDefaultItem(Blocks.BIRCH_SAPLING, Blocks.POTTED_BIRCH_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.BIRCH_LEAVES, TexturedModel.LEAVES, -8345771);
        this.woodProvider(Blocks.OAK_LOG).logWithHorizontal(Blocks.OAK_LOG).wood(Blocks.OAK_WOOD);
        this.woodProvider(Blocks.STRIPPED_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_OAK_LOG).wood(Blocks.STRIPPED_OAK_WOOD);
        this.createPlantWithDefaultItem(Blocks.OAK_SAPLING, Blocks.POTTED_OAK_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.OAK_LEAVES, TexturedModel.LEAVES, -12012264);
        this.woodProvider(Blocks.SPRUCE_LOG).logWithHorizontal(Blocks.SPRUCE_LOG).wood(Blocks.SPRUCE_WOOD);
        this.woodProvider(Blocks.STRIPPED_SPRUCE_LOG).logWithHorizontal(Blocks.STRIPPED_SPRUCE_LOG).wood(Blocks.STRIPPED_SPRUCE_WOOD);
        this.createPlantWithDefaultItem(Blocks.SPRUCE_SAPLING, Blocks.POTTED_SPRUCE_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.SPRUCE_LEAVES, TexturedModel.LEAVES, -10380959);
        this.woodProvider(Blocks.DARK_OAK_LOG).logWithHorizontal(Blocks.DARK_OAK_LOG).wood(Blocks.DARK_OAK_WOOD);
        this.woodProvider(Blocks.STRIPPED_DARK_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_DARK_OAK_LOG).wood(Blocks.STRIPPED_DARK_OAK_WOOD);
        this.createPlantWithDefaultItem(Blocks.DARK_OAK_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.DARK_OAK_LEAVES, TexturedModel.LEAVES, -12012264);
        this.woodProvider(Blocks.PALE_OAK_LOG).logWithHorizontal(Blocks.PALE_OAK_LOG).wood(Blocks.PALE_OAK_WOOD);
        this.woodProvider(Blocks.STRIPPED_PALE_OAK_LOG).logWithHorizontal(Blocks.STRIPPED_PALE_OAK_LOG).wood(Blocks.STRIPPED_PALE_OAK_WOOD);
        this.createPlantWithDefaultItem(Blocks.PALE_OAK_SAPLING, Blocks.POTTED_PALE_OAK_SAPLING, PlantType.NOT_TINTED);
        this.createTrivialBlock(Blocks.PALE_OAK_LEAVES, TexturedModel.LEAVES);
        this.woodProvider(Blocks.JUNGLE_LOG).logWithHorizontal(Blocks.JUNGLE_LOG).wood(Blocks.JUNGLE_WOOD);
        this.woodProvider(Blocks.STRIPPED_JUNGLE_LOG).logWithHorizontal(Blocks.STRIPPED_JUNGLE_LOG).wood(Blocks.STRIPPED_JUNGLE_WOOD);
        this.createPlantWithDefaultItem(Blocks.JUNGLE_SAPLING, Blocks.POTTED_JUNGLE_SAPLING, PlantType.NOT_TINTED);
        this.createTintedLeaves(Blocks.JUNGLE_LEAVES, TexturedModel.LEAVES, -12012264);
        this.woodProvider(Blocks.CRIMSON_STEM).log(Blocks.CRIMSON_STEM).wood(Blocks.CRIMSON_HYPHAE);
        this.woodProvider(Blocks.STRIPPED_CRIMSON_STEM).log(Blocks.STRIPPED_CRIMSON_STEM).wood(Blocks.STRIPPED_CRIMSON_HYPHAE);
        this.createPlantWithDefaultItem(Blocks.CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_FUNGUS, PlantType.NOT_TINTED);
        this.createNetherRoots(Blocks.CRIMSON_ROOTS, Blocks.POTTED_CRIMSON_ROOTS);
        this.woodProvider(Blocks.WARPED_STEM).log(Blocks.WARPED_STEM).wood(Blocks.WARPED_HYPHAE);
        this.woodProvider(Blocks.STRIPPED_WARPED_STEM).log(Blocks.STRIPPED_WARPED_STEM).wood(Blocks.STRIPPED_WARPED_HYPHAE);
        this.createPlantWithDefaultItem(Blocks.WARPED_FUNGUS, Blocks.POTTED_WARPED_FUNGUS, PlantType.NOT_TINTED);
        this.createNetherRoots(Blocks.WARPED_ROOTS, Blocks.POTTED_WARPED_ROOTS);
        this.woodProvider(Blocks.BAMBOO_BLOCK).logUVLocked(Blocks.BAMBOO_BLOCK);
        this.woodProvider(Blocks.STRIPPED_BAMBOO_BLOCK).logUVLocked(Blocks.STRIPPED_BAMBOO_BLOCK);
        this.createCrossBlock(Blocks.NETHER_SPROUTS, PlantType.NOT_TINTED);
        this.registerSimpleFlatItemModel(Items.NETHER_SPROUTS);
        this.createDoor(Blocks.IRON_DOOR);
        this.createTrapdoor(Blocks.IRON_TRAPDOOR);
        this.createSmoothStoneSlab();
        this.createPassiveRail(Blocks.RAIL);
        this.createActiveRail(Blocks.POWERED_RAIL);
        this.createActiveRail(Blocks.DETECTOR_RAIL);
        this.createActiveRail(Blocks.ACTIVATOR_RAIL);
        this.createComparator();
        this.createCommandBlock(Blocks.COMMAND_BLOCK);
        this.createCommandBlock(Blocks.REPEATING_COMMAND_BLOCK);
        this.createCommandBlock(Blocks.CHAIN_COMMAND_BLOCK);
        this.createAnvil(Blocks.ANVIL);
        this.createAnvil(Blocks.CHIPPED_ANVIL);
        this.createAnvil(Blocks.DAMAGED_ANVIL);
        this.createBarrel();
        this.createBell();
        this.createFurnace(Blocks.FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        this.createFurnace(Blocks.BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
        this.createFurnace(Blocks.SMOKER, TexturedModel.ORIENTABLE);
        this.createRedstoneWire();
        this.createRespawnAnchor();
        this.createSculkCatalyst();
        this.copyModel(Blocks.CHISELED_STONE_BRICKS, Blocks.INFESTED_CHISELED_STONE_BRICKS);
        this.copyModel(Blocks.COBBLESTONE, Blocks.INFESTED_COBBLESTONE);
        this.copyModel(Blocks.CRACKED_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS);
        this.copyModel(Blocks.MOSSY_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS);
        this.createInfestedStone();
        this.copyModel(Blocks.STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS);
        this.createInfestedDeepslate();
    }

    private void createLightBlock() {
        ItemModel.Unbaked base = ItemModelUtils.plainModel(this.createFlatItemModel(Items.LIGHT));
        HashMap<Integer, ItemModel.Unbaked> overrides = new HashMap<Integer, ItemModel.Unbaked>(16);
        PropertyDispatch.C1<MultiVariant, Integer> light = PropertyDispatch.initial(BlockStateProperties.LEVEL);
        for (int i = 0; i <= 15; ++i) {
            String suffix = String.format(Locale.ROOT, "_%02d", i);
            Material texture = TextureMapping.getItemTexture(Items.LIGHT, suffix);
            light.select(i, BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.createWithSuffix(Blocks.LIGHT, suffix, TextureMapping.particle(texture), this.modelOutput)));
            ItemModel.Unbaked overrideItem = ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(Items.LIGHT, suffix), TextureMapping.layer0(texture), this.modelOutput));
            overrides.put(i, overrideItem);
        }
        this.itemModelOutput.accept(Items.LIGHT, ItemModelUtils.selectBlockItemProperty(LightBlock.LEVEL, base, overrides));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(Blocks.LIGHT).with(light));
    }

    private void createCopperChainItem(Item unwaxed, Item waxed) {
        Identifier model = this.createFlatItemModel(unwaxed);
        this.registerSimpleItemModel(unwaxed, model);
        this.registerSimpleItemModel(waxed, model);
    }

    private void createCandleAndCandleCake(Block candleBlock, Block candleCakeBlock) {
        this.registerSimpleFlatItemModel(candleBlock.asItem());
        TextureMapping candleTexture = TextureMapping.cube(TextureMapping.getBlockTexture(candleBlock));
        TextureMapping candleLitTexture = TextureMapping.cube(TextureMapping.getBlockTexture(candleBlock, "_lit"));
        MultiVariant oneCandle = BlockModelGenerators.plainVariant(ModelTemplates.CANDLE.createWithSuffix(candleBlock, "_one_candle", candleTexture, this.modelOutput));
        MultiVariant twoCandles = BlockModelGenerators.plainVariant(ModelTemplates.TWO_CANDLES.createWithSuffix(candleBlock, "_two_candles", candleTexture, this.modelOutput));
        MultiVariant threeCandles = BlockModelGenerators.plainVariant(ModelTemplates.THREE_CANDLES.createWithSuffix(candleBlock, "_three_candles", candleTexture, this.modelOutput));
        MultiVariant fourCandles = BlockModelGenerators.plainVariant(ModelTemplates.FOUR_CANDLES.createWithSuffix(candleBlock, "_four_candles", candleTexture, this.modelOutput));
        MultiVariant oneCandleLit = BlockModelGenerators.plainVariant(ModelTemplates.CANDLE.createWithSuffix(candleBlock, "_one_candle_lit", candleLitTexture, this.modelOutput));
        MultiVariant twoCandlesLit = BlockModelGenerators.plainVariant(ModelTemplates.TWO_CANDLES.createWithSuffix(candleBlock, "_two_candles_lit", candleLitTexture, this.modelOutput));
        MultiVariant threeCandlesLit = BlockModelGenerators.plainVariant(ModelTemplates.THREE_CANDLES.createWithSuffix(candleBlock, "_three_candles_lit", candleLitTexture, this.modelOutput));
        MultiVariant fourCandlesLit = BlockModelGenerators.plainVariant(ModelTemplates.FOUR_CANDLES.createWithSuffix(candleBlock, "_four_candles_lit", candleLitTexture, this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(candleBlock).with(PropertyDispatch.initial(BlockStateProperties.CANDLES, BlockStateProperties.LIT).select(1, false, oneCandle).select(2, false, twoCandles).select(3, false, threeCandles).select(4, false, fourCandles).select(1, true, oneCandleLit).select(2, true, twoCandlesLit).select(3, true, threeCandlesLit).select(4, true, fourCandlesLit)));
        MultiVariant candleCake = BlockModelGenerators.plainVariant(ModelTemplates.CANDLE_CAKE.create(candleCakeBlock, TextureMapping.candleCake(candleBlock, false), this.modelOutput));
        MultiVariant litCandleCake = BlockModelGenerators.plainVariant(ModelTemplates.CANDLE_CAKE.createWithSuffix(candleCakeBlock, "_lit", TextureMapping.candleCake(candleBlock, true), this.modelOutput));
        this.blockStateOutput.accept(MultiVariantGenerator.dispatch(candleCakeBlock).with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, litCandleCake, candleCake)));
    }

    private /* synthetic */ MultiVariant lambda$createCropBlock$0(int[] stages, Int2ObjectMap models, Block block, Integer i) {
        int stage = stages[i];
        return BlockModelGenerators.plainVariant((Identifier)models.computeIfAbsent(stage, s -> this.createSuffixedVariant(block, "_stage" + s, ModelTemplates.CROP, TextureMapping::crop)));
    }

    @Environment(value=EnvType.CLIENT)
    private class BlockFamilyProvider {
        private final TextureMapping mapping;
        private final Map<ModelTemplate, Identifier> models;
        private @Nullable BlockFamily family;
        private @Nullable Variant fullBlock;
        private final Set<Block> skipGeneratingModelsFor;
        final /* synthetic */ BlockModelGenerators this$0;

        public BlockFamilyProvider(BlockModelGenerators blockModelGenerators, TextureMapping mapping) {
            BlockModelGenerators blockModelGenerators2 = blockModelGenerators;
            Objects.requireNonNull(blockModelGenerators2);
            this.this$0 = blockModelGenerators2;
            this.models = new HashMap<ModelTemplate, Identifier>();
            this.skipGeneratingModelsFor = new HashSet<Block>();
            this.mapping = mapping;
        }

        public BlockFamilyProvider fullBlock(Block block, ModelTemplate template) {
            this.fullBlock = BlockModelGenerators.plainModel(template.create(block, this.mapping, this.this$0.modelOutput));
            if (FULL_BLOCK_MODEL_CUSTOM_GENERATORS.containsKey(block)) {
                this.this$0.blockStateOutput.accept(FULL_BLOCK_MODEL_CUSTOM_GENERATORS.get(block).create(block, this.fullBlock, this.mapping, this.this$0.modelOutput));
            } else {
                this.this$0.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.variant(this.fullBlock)));
            }
            return this;
        }

        public BlockFamilyProvider donateModelTo(Block donor, Block copyTo) {
            Identifier donorModelLocation = ModelLocationUtils.getModelLocation(donor);
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(copyTo, BlockModelGenerators.plainVariant(donorModelLocation)));
            this.this$0.itemModelOutput.copy(donor.asItem(), copyTo.asItem());
            this.skipGeneratingModelsFor.add(copyTo);
            return this;
        }

        public BlockFamilyProvider button(Block block) {
            MultiVariant normal = BlockModelGenerators.plainVariant(ModelTemplates.BUTTON.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant pressed = BlockModelGenerators.plainVariant(ModelTemplates.BUTTON_PRESSED.create(block, this.mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createButton(block, normal, pressed));
            Identifier inventory = ModelTemplates.BUTTON_INVENTORY.create(block, this.mapping, this.this$0.modelOutput);
            this.this$0.registerSimpleItemModel(block, inventory);
            return this;
        }

        public BlockFamilyProvider wall(Block block) {
            MultiVariant post = BlockModelGenerators.plainVariant(ModelTemplates.WALL_POST.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant low = BlockModelGenerators.plainVariant(ModelTemplates.WALL_LOW_SIDE.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant high = BlockModelGenerators.plainVariant(ModelTemplates.WALL_TALL_SIDE.create(block, this.mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createWall(block, post, low, high));
            Identifier inventory = ModelTemplates.WALL_INVENTORY.create(block, this.mapping, this.this$0.modelOutput);
            this.this$0.registerSimpleItemModel(block, inventory);
            return this;
        }

        public BlockFamilyProvider customFence(Block block) {
            TextureMapping mapping = TextureMapping.customParticle(block);
            MultiVariant post = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_POST.create(block, mapping, this.this$0.modelOutput));
            MultiVariant north = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_SIDE_NORTH.create(block, mapping, this.this$0.modelOutput));
            MultiVariant east = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_SIDE_EAST.create(block, mapping, this.this$0.modelOutput));
            MultiVariant south = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_SIDE_SOUTH.create(block, mapping, this.this$0.modelOutput));
            MultiVariant west = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_SIDE_WEST.create(block, mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createCustomFence(block, post, north, east, south, west));
            Identifier inventory = ModelTemplates.CUSTOM_FENCE_INVENTORY.create(block, mapping, this.this$0.modelOutput);
            this.this$0.registerSimpleItemModel(block, inventory);
            return this;
        }

        public BlockFamilyProvider fence(Block block) {
            MultiVariant post = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_POST.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant side = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_SIDE.create(block, this.mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createFence(block, post, side));
            Identifier inventory = ModelTemplates.FENCE_INVENTORY.create(block, this.mapping, this.this$0.modelOutput);
            this.this$0.registerSimpleItemModel(block, inventory);
            return this;
        }

        public BlockFamilyProvider customFenceGate(Block block) {
            TextureMapping mapping = TextureMapping.customParticle(block);
            MultiVariant open = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_GATE_OPEN.create(block, mapping, this.this$0.modelOutput));
            MultiVariant closed = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_GATE_CLOSED.create(block, mapping, this.this$0.modelOutput));
            MultiVariant openWall = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_GATE_WALL_OPEN.create(block, mapping, this.this$0.modelOutput));
            MultiVariant closedWall = BlockModelGenerators.plainVariant(ModelTemplates.CUSTOM_FENCE_GATE_WALL_CLOSED.create(block, mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createFenceGate(block, open, closed, openWall, closedWall, false));
            return this;
        }

        public BlockFamilyProvider fenceGate(Block block) {
            MultiVariant open = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_GATE_OPEN.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant closed = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_GATE_CLOSED.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant openWall = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_GATE_WALL_OPEN.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant closedWall = BlockModelGenerators.plainVariant(ModelTemplates.FENCE_GATE_WALL_CLOSED.create(block, this.mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createFenceGate(block, open, closed, openWall, closedWall, true));
            return this;
        }

        public BlockFamilyProvider pressurePlate(Block block) {
            MultiVariant off = BlockModelGenerators.plainVariant(ModelTemplates.PRESSURE_PLATE_UP.create(block, this.mapping, this.this$0.modelOutput));
            MultiVariant on = BlockModelGenerators.plainVariant(ModelTemplates.PRESSURE_PLATE_DOWN.create(block, this.mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(block, off, on));
            return this;
        }

        public BlockFamilyProvider sign(Block sign) {
            if (this.family == null) {
                throw new IllegalStateException("Family not defined");
            }
            TextureMapping mapping = new TextureMapping().put(TextureSlot.ALL, TextureMapping.getBlockTexture(sign)).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(this.family.getBaseBlock()));
            MultiVariant standingRot0 = BlockModelGenerators.plainVariant(ModelTemplates.SIGN_ROT_0.create(ModelLocationUtils.getModelLocation(sign, "_rot_0"), mapping, this.this$0.modelOutput));
            MultiVariant standingRot1 = BlockModelGenerators.plainVariant(ModelTemplates.SIGN_ROT_1.create(ModelLocationUtils.getModelLocation(sign, "_rot_1"), mapping, this.this$0.modelOutput));
            MultiVariant standingRot2 = BlockModelGenerators.plainVariant(ModelTemplates.SIGN_ROT_2.create(ModelLocationUtils.getModelLocation(sign, "_rot_2"), mapping, this.this$0.modelOutput));
            MultiVariant standingRot3 = BlockModelGenerators.plainVariant(ModelTemplates.SIGN_ROT_3.create(ModelLocationUtils.getModelLocation(sign, "_rot_3"), mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createSign(sign, standingRot0, standingRot1, standingRot2, standingRot3));
            Block wallSign = this.family.getVariants().get((Object)BlockFamily.Variant.WALL_SIGN);
            MultiVariant wallModel = BlockModelGenerators.plainVariant(ModelTemplates.WALL_SIGN.create(wallSign, mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(MultiVariantGenerator.dispatch(wallSign, wallModel).with(ROTATION_HORIZONTAL_FACING_ALT));
            this.this$0.registerSimpleFlatItemModel(sign.asItem());
            return this;
        }

        public BlockFamilyProvider customHangingSign(Block hangingSign) {
            return this.hangingSign(hangingSign, this.family.getBaseBlock(), BlockFamily.Variant.CUSTOM_WALL_HANGING_SIGN);
        }

        public BlockFamilyProvider hangingSign(Block hangingSign) {
            return this.hangingSign(hangingSign, this.family.get(BlockFamily.Variant.STRIPPED_LOG), BlockFamily.Variant.WALL_HANGING_SIGN);
        }

        private BlockFamilyProvider hangingSign(Block hangingSign, Block particleBlock, BlockFamily.Variant wallVarient) {
            TextureMapping mapping = new TextureMapping().put(TextureSlot.ALL, TextureMapping.getBlockTexture(hangingSign)).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(particleBlock));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createHangingSign(hangingSign, BlockModelGenerators.plainVariant(ModelTemplates.HANGING_SIGN_ROT_0.create(ModelLocationUtils.getModelLocation(hangingSign, "_rot_0"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.HANGING_SIGN_ROT_1.create(ModelLocationUtils.getModelLocation(hangingSign, "_rot_1"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.HANGING_SIGN_ROT_2.create(ModelLocationUtils.getModelLocation(hangingSign, "_rot_2"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.HANGING_SIGN_ROT_3.create(ModelLocationUtils.getModelLocation(hangingSign, "_rot_3"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.ATTACHED_HANGING_SIGN_ROT_0.create(ModelLocationUtils.getModelLocation(hangingSign, "_attached_rot_0"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.ATTACHED_HANGING_SIGN_ROT_1.create(ModelLocationUtils.getModelLocation(hangingSign, "_attached_rot_1"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.ATTACHED_HANGING_SIGN_ROT_2.create(ModelLocationUtils.getModelLocation(hangingSign, "_attached_rot_2"), mapping, this.this$0.modelOutput)), BlockModelGenerators.plainVariant(ModelTemplates.ATTACHED_HANGING_SIGN_ROT_3.create(ModelLocationUtils.getModelLocation(hangingSign, "_attached_rot_3"), mapping, this.this$0.modelOutput))));
            Block wallSign = this.family.get(wallVarient);
            MultiVariant wallModel = BlockModelGenerators.plainVariant(ModelTemplates.WALL_HANGING_SIGN.create(wallSign, mapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(MultiVariantGenerator.dispatch(wallSign, wallModel).with(ROTATION_HORIZONTAL_FACING_ALT));
            this.this$0.registerSimpleFlatItemModel(hangingSign.asItem());
            return this;
        }

        public BlockFamilyProvider slab(Block slab) {
            if (this.fullBlock == null) {
                throw new IllegalStateException("Full block not generated yet");
            }
            Identifier bottom = this.getOrCreateModel(ModelTemplates.SLAB_BOTTOM, slab);
            MultiVariant top = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.SLAB_TOP, slab));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createSlab(slab, BlockModelGenerators.plainVariant(bottom), top, BlockModelGenerators.variant(this.fullBlock)));
            this.this$0.registerSimpleItemModel(slab, bottom);
            return this;
        }

        public BlockFamilyProvider stairs(Block stairs) {
            MultiVariant inner = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.STAIRS_INNER, stairs));
            Identifier straight = this.getOrCreateModel(ModelTemplates.STAIRS_STRAIGHT, stairs);
            MultiVariant outer = BlockModelGenerators.plainVariant(this.getOrCreateModel(ModelTemplates.STAIRS_OUTER, stairs));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createStairs(stairs, inner, BlockModelGenerators.plainVariant(straight), outer));
            this.this$0.registerSimpleItemModel(stairs, straight);
            return this;
        }

        private BlockFamilyProvider fullBlockVariant(Block variant) {
            TexturedModel model = TEXTURED_MODELS.getOrDefault(variant, TexturedModel.CUBE.get(variant));
            MultiVariant variantModel = BlockModelGenerators.plainVariant(model.create(variant, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(variant, variantModel));
            return this;
        }

        private BlockFamilyProvider pillar(Block variant) {
            MultiVariant verticalModel = BlockModelGenerators.plainVariant(TexturedModel.COLUMN.create(variant, this.this$0.modelOutput));
            MultiVariant horizontalModel = BlockModelGenerators.plainVariant(TexturedModel.COLUMN_HORIZONTAL.create(variant, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(variant, verticalModel, horizontalModel));
            return this;
        }

        private BlockFamilyProvider door(Block door) {
            this.this$0.createDoor(door);
            return this;
        }

        private void trapdoor(Block result) {
            if (NON_ORIENTABLE_TRAPDOOR.contains(result)) {
                this.this$0.createTrapdoor(result);
            } else {
                this.this$0.createOrientableTrapdoor(result);
            }
        }

        private Identifier getOrCreateModel(ModelTemplate modelTemplate, Block block) {
            return this.models.computeIfAbsent(modelTemplate, template -> template.create(block, this.mapping, this.this$0.modelOutput));
        }

        public BlockFamilyProvider generateFor(BlockFamily family) {
            this.family = family;
            family.getVariants().forEach((variant, result) -> {
                boolean modelAlreadyRegisteredAsAnotherFamilyBase = BlockFamilies.getAllFamilies().anyMatch(b -> b.getBaseBlock() == result);
                if (this.skipGeneratingModelsFor.contains(result) || modelAlreadyRegisteredAsAnotherFamilyBase) {
                    return;
                }
                BiConsumer<BlockFamilyProvider, Block> consumer = SHAPE_CONSUMERS.get(variant);
                if (consumer != null) {
                    consumer.accept(this, (Block)result);
                }
            });
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class WoodProvider {
        private final TextureMapping logMapping;
        final /* synthetic */ BlockModelGenerators this$0;

        public WoodProvider(BlockModelGenerators blockModelGenerators, TextureMapping logMapping) {
            BlockModelGenerators blockModelGenerators2 = blockModelGenerators;
            Objects.requireNonNull(blockModelGenerators2);
            this.this$0 = blockModelGenerators2;
            this.logMapping = logMapping;
        }

        public WoodProvider wood(Block block) {
            TextureMapping woodMapping = this.logMapping.copyAndUpdate(TextureSlot.END, this.logMapping.get(TextureSlot.SIDE));
            Identifier model = ModelTemplates.CUBE_COLUMN.create(block, woodMapping, this.this$0.modelOutput);
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block, BlockModelGenerators.plainVariant(model)));
            this.this$0.registerSimpleItemModel(block, model);
            return this;
        }

        public WoodProvider log(Block block) {
            Identifier model = ModelTemplates.CUBE_COLUMN.create(block, this.logMapping, this.this$0.modelOutput);
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createAxisAlignedPillarBlock(block, BlockModelGenerators.plainVariant(model)));
            this.this$0.registerSimpleItemModel(block, model);
            return this;
        }

        public WoodProvider logWithHorizontal(Block block) {
            Identifier model = ModelTemplates.CUBE_COLUMN.create(block, this.logMapping, this.this$0.modelOutput);
            MultiVariant horizontalModel = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN_HORIZONTAL.create(block, this.logMapping, this.this$0.modelOutput));
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(block, BlockModelGenerators.plainVariant(model), horizontalModel));
            this.this$0.registerSimpleItemModel(block, model);
            return this;
        }

        public WoodProvider logUVLocked(Block block) {
            this.this$0.blockStateOutput.accept(BlockModelGenerators.createPillarBlockUVLocked(block, this.logMapping, this.this$0.modelOutput));
            this.this$0.registerSimpleItemModel(block, ModelTemplates.CUBE_COLUMN.create(block, this.logMapping, this.this$0.modelOutput));
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum PlantType {
        TINTED(ModelTemplates.TINTED_CROSS, ModelTemplates.TINTED_FLOWER_POT_CROSS, false),
        NOT_TINTED(ModelTemplates.CROSS, ModelTemplates.FLOWER_POT_CROSS, false),
        EMISSIVE_NOT_TINTED(ModelTemplates.CROSS_EMISSIVE, ModelTemplates.FLOWER_POT_CROSS_EMISSIVE, true);

        private final ModelTemplate blockTemplate;
        private final ModelTemplate flowerPotTemplate;
        private final boolean isEmissive;

        private PlantType(ModelTemplate blockTemplate, ModelTemplate flowerPotTemplate, boolean isEmissive) {
            this.blockTemplate = blockTemplate;
            this.flowerPotTemplate = flowerPotTemplate;
            this.isEmissive = isEmissive;
        }

        public ModelTemplate getCross() {
            return this.blockTemplate;
        }

        public ModelTemplate getCrossPot() {
            return this.flowerPotTemplate;
        }

        public Identifier createItemModel(BlockModelGenerators generator, Block block) {
            Item blockItem = block.asItem();
            if (this.isEmissive) {
                return generator.createFlatItemModelWithBlockTextureAndOverlay(blockItem, block, "_emissive");
            }
            return generator.createFlatItemModelWithBlockTexture(blockItem, block);
        }

        public TextureMapping getTextureMapping(Block block) {
            return this.isEmissive ? TextureMapping.crossEmissive(block) : TextureMapping.cross(block);
        }

        public TextureMapping getPlantTextureMapping(Block standAlone) {
            return this.isEmissive ? TextureMapping.plantEmissive(standAlone) : TextureMapping.plant(standAlone);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record BookSlotModelCacheKey(ModelTemplate template, String modelSuffix) {
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface BlockStateGeneratorSupplier {
        public BlockModelDefinitionGenerator create(Block var1, Variant var2, TextureMapping var3, BiConsumer<Identifier, ModelInstance> var4);
    }
}

