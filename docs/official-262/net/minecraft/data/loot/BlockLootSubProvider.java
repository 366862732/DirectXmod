/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.loot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public abstract class BlockLootSubProvider
implements LootTableSubProvider {
    protected final HolderLookup.Provider registries;
    private final Set<Item> explosionResistant;
    private final FeatureFlagSet enabledFeatures;
    private final Map<ResourceKey<LootTable>, LootTable.Builder> map = new HashMap<ResourceKey<LootTable>, LootTable.Builder>();
    protected static final float[] NORMAL_LEAVES_SAPLING_CHANCES = new float[]{0.05f, 0.0625f, 0.083333336f, 0.1f};
    private static final float[] NORMAL_LEAVES_STICK_CHANCES = new float[]{0.02f, 0.022222223f, 0.025f, 0.033333335f, 0.1f};

    protected LootItemCondition.Builder hasSilkTouch() {
        return MatchTool.toolMatches(ItemPredicate.Builder.item().withComponents(DataComponentMatchers.Builder.components().partial(DataComponentPredicates.ENCHANTMENTS, EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1))))).build()));
    }

    protected LootItemCondition.Builder doesNotHaveSilkTouch() {
        return this.hasSilkTouch().invert();
    }

    protected LootItemCondition.Builder hasShears() {
        return MatchTool.toolMatches(ItemPredicate.Builder.item().of((HolderGetter<Item>)this.registries.lookupOrThrow(Registries.ITEM), Items.SHEARS));
    }

    private LootItemCondition.Builder hasShearsOrSilkTouch() {
        return this.hasShears().or(this.hasSilkTouch());
    }

    private LootItemCondition.Builder doesNotHaveShearsOrSilkTouch() {
        return this.hasShearsOrSilkTouch().invert();
    }

    protected BlockLootSubProvider(Set<Item> explosionResistant, FeatureFlagSet enabledFeatures, HolderLookup.Provider registries) {
        this.explosionResistant = explosionResistant;
        this.enabledFeatures = enabledFeatures;
        this.registries = registries;
    }

    protected <T extends FunctionUserBuilder<T>> T applyExplosionDecay(ItemLike type, FunctionUserBuilder<T> builder) {
        if (!this.explosionResistant.contains(type.asItem())) {
            return builder.apply(ApplyExplosionDecay.explosionDecay());
        }
        return builder.unwrap();
    }

    protected <T extends ConditionUserBuilder<T>> T applyExplosionCondition(ItemLike type, ConditionUserBuilder<T> builder) {
        if (!this.explosionResistant.contains(type.asItem())) {
            return builder.when(ExplosionCondition.survivesExplosion());
        }
        return builder.unwrap();
    }

    public LootTable.Builder createSingleItemTable(ItemLike drop) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(drop, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(drop))));
    }

    private static LootTable.Builder createSelfDropDispatchTable(Block original, LootItemCondition.Builder condition, LootPoolEntryContainer.Builder<?> entry) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(original).when(condition)).otherwise(entry)));
    }

    protected LootTable.Builder createSilkTouchDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return BlockLootSubProvider.createSelfDropDispatchTable(original, this.hasSilkTouch(), entry);
    }

    protected LootTable.Builder createShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return BlockLootSubProvider.createSelfDropDispatchTable(original, this.hasShears(), entry);
    }

    protected LootTable.Builder createSilkTouchOrShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return BlockLootSubProvider.createSelfDropDispatchTable(original, this.hasShearsOrSilkTouch(), entry);
    }

    protected LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop) {
        return this.createSilkTouchDispatchTable(original, (LootPoolEntryContainer.Builder)this.applyExplosionCondition(original, LootItem.lootTableItem(drop)));
    }

    protected LootTable.Builder createSingleItemTable(ItemLike drop, NumberProvider count) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(drop, LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(count)))));
    }

    protected LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop, NumberProvider count) {
        return this.createSilkTouchDispatchTable(original, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(original, LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(count))));
    }

    private LootTable.Builder createSilkTouchOnlyTable(ItemLike drop) {
        return LootTable.lootTable().withPool(LootPool.lootPool().when(this.hasSilkTouch()).setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(drop)));
    }

    private LootTable.Builder createPotFlowerItemTable(ItemLike flower) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(Blocks.FLOWER_POT, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(Blocks.FLOWER_POT)))).withPool(this.applyExplosionCondition(flower, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(flower))));
    }

    protected LootTable.Builder createSlabItemTable(Block slab) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(slab, LootItem.lootTableItem(slab).apply((LootItemFunction.Builder)((Object)SetItemCountFunction.setCount(ConstantValue.exactly(2.0f)).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(slab).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SlabBlock.TYPE, SlabType.DOUBLE))))))));
    }

    protected <T extends Comparable<T> & StringRepresentable> LootTable.Builder createSinglePropConditionTable(Block drop, Property<T> property, T value) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(drop, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)LootItem.lootTableItem(drop).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(drop).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(property, value))))));
    }

    protected LootTable.Builder createNameableBlockEntityTable(Block drop) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(drop, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(drop).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME))))));
    }

    protected LootTable.Builder createShulkerBoxDrop(Block shulkerBox) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(shulkerBox, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(shulkerBox).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME).include(DataComponents.CONTAINER).include(DataComponents.LOCK).include(DataComponents.CONTAINER_LOOT))))));
    }

    protected LootTable.Builder createCopperOreDrops(Block block) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(block, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(Items.RAW_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 5.0f)))).apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    protected LootTable.Builder createLapisOreDrops(Block block) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(block, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(Items.LAPIS_LAZULI).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0f, 9.0f)))).apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    protected LootTable.Builder createRedstoneOreDrops(Block block) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(block, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(Items.REDSTONE).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0f, 5.0f)))).apply(ApplyBonusCount.addUniformBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    protected LootTable.Builder createBannerDrop(Block original) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(original, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(original).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME).include(DataComponents.ITEM_NAME).include(DataComponents.TOOLTIP_DISPLAY).include(DataComponents.BANNER_PATTERNS).include(DataComponents.RARITY))))));
    }

    protected LootTable.Builder createBeeNestDrop(Block original) {
        return LootTable.lootTable().withPool(LootPool.lootPool().when(this.hasSilkTouch()).setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(original).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.BEES))).apply(CopyBlockState.copyState(original).copy(BeehiveBlock.HONEY_LEVEL)))));
    }

    protected LootTable.Builder createBeeHiveDrop(Block original) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(((LootPoolEntryContainer.Builder)((Object)((LootPoolSingletonContainer.Builder)((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(original).when(this.hasSilkTouch())).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.BEES))).apply(CopyBlockState.copyState(original).copy(BeehiveBlock.HONEY_LEVEL)))).otherwise(LootItem.lootTableItem(original))));
    }

    protected LootTable.Builder createCaveVinesDrop(Block original) {
        return LootTable.lootTable().withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.GLOW_BERRIES)).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(original).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CaveVines.BERRIES, true))));
    }

    protected LootTable.Builder createCopperGolemStatueBlock(Block block) {
        return LootTable.lootTable().withPool(this.applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(block).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME))).apply(CopyBlockState.copyState(block).copy(CopperGolemStatueBlock.POSE))))));
    }

    protected LootTable.Builder createOreDrop(Block original, Item drop) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(original, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(original, LootItem.lootTableItem(drop).apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    protected LootTable.Builder createMushroomBlockDrop(Block original, ItemLike drop) {
        return this.createSilkTouchDispatchTable(original, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(original, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(UniformGenerator.between(-6.0f, 2.0f)))).apply(LimitCount.limitCount(IntRange.lowerBound(0)))));
    }

    protected LootTable.Builder createGrassDrops(Block original) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createShearsDispatchTable(original, (LootPoolEntryContainer.Builder)this.applyExplosionDecay(original, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(Items.WHEAT_SEEDS).when(LootItemRandomChanceCondition.randomChance(0.125f))).apply(ApplyBonusCount.addUniformBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE), 2))));
    }

    public LootTable.Builder createStemDrops(Block block, Item drop) {
        return LootTable.lootTable().withPool(this.applyExplosionDecay(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder)LootItem.lootTableItem(drop).apply(StemBlock.AGE.getPossibleValues(), age -> SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, (float)(age + 1) / 15.0f)).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(StemBlock.AGE, age.intValue())))))));
    }

    public LootTable.Builder createAttachedStemDrops(Block block, Item drop) {
        return LootTable.lootTable().withPool(this.applyExplosionDecay(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, 0.53333336f)))))));
    }

    protected LootTable.Builder createShearsOnlyDrop(ItemLike drop) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).when(this.hasShears()).add(LootItem.lootTableItem(drop)));
    }

    protected LootTable.Builder createShearsOrSilkTouchOnlyDrop(ItemLike drop) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).when(this.hasShearsOrSilkTouch()).add(LootItem.lootTableItem(drop)));
    }

    protected LootTable.Builder createMultifaceBlockDrops(Block block, LootItemCondition.Builder condition) {
        return LootTable.lootTable().withPool(LootPool.lootPool().add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, ((LootPoolSingletonContainer.Builder)((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(block).when(condition)).apply(Direction.values(), dir -> SetItemCountFunction.setCount(ConstantValue.exactly(1.0f), true).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MultifaceBlock.getFaceProperty(dir), true))))).apply(SetItemCountFunction.setCount(ConstantValue.exactly(-1.0f), true)))));
    }

    protected LootTable.Builder createMultifaceBlockDrops(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, ((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(block).apply(Direction.values(), dir -> SetItemCountFunction.setCount(ConstantValue.exactly(1.0f), true).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MultifaceBlock.getFaceProperty(dir), true))))).apply(SetItemCountFunction.setCount(ConstantValue.exactly(-1.0f), true)))));
    }

    protected LootTable.Builder createMossyCarpetBlockDrops(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, (FunctionUserBuilder)((Object)LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MossyCarpetBlock.BASE, true)))))));
    }

    protected LootTable.Builder createLeavesDrops(Block original, Block sapling, float ... saplingChances) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchOrShearsDispatchTable(original, (LootPoolEntryContainer.Builder<?>)((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(original, LootItem.lootTableItem(sapling))).when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE), saplingChances))).withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).when(this.doesNotHaveShearsOrSilkTouch()).add((LootPoolEntryContainer.Builder<?>)((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(original, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))).when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES))));
    }

    protected LootTable.Builder createOakLeavesDrops(Block original, Block sapling, float ... saplingChances) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createLeavesDrops(original, sapling, saplingChances).withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).when(this.doesNotHaveShearsOrSilkTouch()).add((LootPoolEntryContainer.Builder<?>)((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(original, LootItem.lootTableItem(Items.APPLE))).when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE), 0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f))));
    }

    protected LootTable.Builder createMangroveLeavesDrops(Block block) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchOrShearsDispatchTable(block, (LootPoolEntryContainer.Builder<?>)((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(Blocks.MANGROVE_LEAVES, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))).when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES)));
    }

    protected LootTable.Builder createCropDrops(Block original, Item cropDrop, Item seedDrop, LootItemCondition.Builder isMaxAge) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.applyExplosionDecay(original, LootTable.lootTable().withPool(LootPool.lootPool().add(((LootPoolSingletonContainer.Builder)LootItem.lootTableItem(cropDrop).when(isMaxAge)).otherwise(LootItem.lootTableItem(seedDrop)))).withPool(LootPool.lootPool().when(isMaxAge).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(seedDrop).apply(ApplyBonusCount.addBonusBinomialDistributionCount(enchantments.getOrThrow(Enchantments.FORTUNE), 0.5714286f, 3))))));
    }

    protected LootTable.Builder createDoublePlantShearsDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().when(this.hasShears()).add((LootPoolEntryContainer.Builder<?>)((Object)LootItem.lootTableItem(block).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0f))))));
    }

    protected LootTable.Builder createDoublePlantWithSeedDrops(Block block, Block drop) {
        HolderGetter blocks = this.registries.lookupOrThrow(Registries.BLOCK);
        AlternativesEntry.Builder dropEntry = ((LootPoolSingletonContainer.Builder)((LootPoolEntryContainer.Builder)((Object)LootItem.lootTableItem(drop).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0f))))).when(this.hasShears())).otherwise((LootPoolEntryContainer.Builder<?>)((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(block, LootItem.lootTableItem(Items.WHEAT_SEEDS))).when(LootItemRandomChanceCondition.randomChance(0.125f)));
        return LootTable.lootTable().withPool(LootPool.lootPool().add(dropEntry).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of((HolderGetter<Block>)blocks, block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))), new BlockPos(0, 1, 0)))).withPool(LootPool.lootPool().add(dropEntry).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))).when(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of((HolderGetter<Block>)blocks, block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))), new BlockPos(0, -1, 0))));
    }

    protected LootTable.Builder createCandleDrops(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, (FunctionUserBuilder)LootItem.lootTableItem(block).apply(List.of(Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4)), count -> SetItemCountFunction.setCount(ConstantValue.exactly(count.intValue())).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CandleBlock.CANDLES, count.intValue())))))));
    }

    public LootTable.Builder createSegmentedBlockDrops(Block block) {
        if (block instanceof SegmentableBlock) {
            SegmentableBlock segmentableBlock = (SegmentableBlock)((Object)block);
            return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add((LootPoolEntryContainer.Builder)this.applyExplosionDecay(block, (FunctionUserBuilder)LootItem.lootTableItem(block).apply(IntStream.rangeClosed(1, 4).boxed().toList(), count -> SetItemCountFunction.setCount(ConstantValue.exactly(count.intValue())).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(segmentableBlock.getSegmentAmountProperty(), count.intValue())))))));
        }
        return BlockLootSubProvider.noDrop();
    }

    protected static LootTable.Builder createCandleCakeDrops(Block candle) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(candle)));
    }

    public static LootTable.Builder noDrop() {
        return LootTable.lootTable();
    }

    protected abstract void generate();

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        this.generate();
        HashSet seen = new HashSet();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!block.isEnabled(this.enabledFeatures)) continue;
            block.getLootTable().ifPresent(lootTable -> {
                if (seen.add(lootTable)) {
                    LootTable.Builder builder = this.map.remove(lootTable);
                    if (builder == null) {
                        throw new IllegalStateException(String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", lootTable.identifier(), BuiltInRegistries.BLOCK.getKey(block)));
                    }
                    output.accept((ResourceKey<LootTable>)lootTable, builder);
                }
            });
        }
        if (!this.map.isEmpty()) {
            throw new IllegalStateException("Created block loot tables for non-blocks: " + String.valueOf(this.map.keySet()));
        }
    }

    protected void addNetherVinesDropTable(Block vineBlock, Block plantBlock) {
        HolderGetter enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        LootTable.Builder builder = this.createSilkTouchOrShearsDispatchTable(vineBlock, (LootPoolEntryContainer.Builder<?>)LootItem.lootTableItem(vineBlock).when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE), 0.33f, 0.55f, 0.77f, 1.0f)));
        this.add(vineBlock, builder);
        this.add(plantBlock, builder);
    }

    protected LootTable.Builder createDoorTable(Block block) {
        return this.createSinglePropConditionTable(block, DoorBlock.HALF, DoubleBlockHalf.LOWER);
    }

    protected void dropPottedContents(Block potted) {
        this.add(potted, (Block block) -> this.createPotFlowerItemTable(((FlowerPotBlock)block).getPotted()));
    }

    protected void otherWhenSilkTouch(Block block, Block other) {
        this.add(block, this.createSilkTouchOnlyTable(other));
    }

    protected void dropOther(Block block, ItemLike drop) {
        this.add(block, this.createSingleItemTable(drop));
    }

    protected void dropWhenSilkTouch(Block block) {
        this.otherWhenSilkTouch(block, block);
    }

    protected void dropSelf(Block block) {
        this.dropOther(block, block);
    }

    protected void add(Block block, Function<Block, LootTable.Builder> builder) {
        this.add(block, builder.apply(block));
    }

    protected void add(Block block, LootTable.Builder builder) {
        this.map.put(block.getLootTable().orElseThrow(() -> new IllegalStateException("Block " + String.valueOf(block) + " does not have loot table")), builder);
    }
}

