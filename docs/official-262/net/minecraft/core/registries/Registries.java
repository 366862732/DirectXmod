/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.core.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatType;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.pig.PigSoundVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.timeline.Timeline;

public class Registries {
    public static final Identifier ROOT_REGISTRY_NAME = Identifier.withDefaultNamespace("root");
    public static final ResourceKey<Registry<Activity>> ACTIVITY = Registries.createRegistryKey("activity");
    public static final ResourceKey<Registry<Attribute>> ATTRIBUTE = Registries.createRegistryKey("attribute");
    public static final ResourceKey<Registry<MapCodec<? extends BiomeSource>>> BIOME_SOURCE = Registries.createRegistryKey("worldgen/biome_source");
    public static final ResourceKey<Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPE = Registries.createRegistryKey("block_entity_type");
    public static final ResourceKey<Registry<BlockPredicateType<?>>> BLOCK_PREDICATE_TYPE = Registries.createRegistryKey("block_predicate_type");
    public static final ResourceKey<Registry<BlockStateProviderType<?>>> BLOCK_STATE_PROVIDER_TYPE = Registries.createRegistryKey("worldgen/block_state_provider_type");
    public static final ResourceKey<Registry<MapCodec<? extends Block>>> BLOCK_TYPE = Registries.createRegistryKey("block_type");
    public static final ResourceKey<Registry<Block>> BLOCK = Registries.createRegistryKey("block");
    public static final ResourceKey<Registry<WorldCarver<?>>> CARVER = Registries.createRegistryKey("worldgen/carver");
    public static final ResourceKey<Registry<MapCodec<? extends ChunkGenerator>>> CHUNK_GENERATOR = Registries.createRegistryKey("worldgen/chunk_generator");
    public static final ResourceKey<Registry<ChunkStatus>> CHUNK_STATUS = Registries.createRegistryKey("chunk_status");
    public static final ResourceKey<Registry<ArgumentTypeInfo<?, ?>>> COMMAND_ARGUMENT_TYPE = Registries.createRegistryKey("command_argument_type");
    public static final ResourceKey<Registry<ConsumeEffect.Type<?>>> CONSUME_EFFECT_TYPE = Registries.createRegistryKey("consume_effect_type");
    public static final ResourceKey<Registry<CreativeModeTab>> CREATIVE_MODE_TAB = Registries.createRegistryKey("creative_mode_tab");
    public static final ResourceKey<Registry<Identifier>> CUSTOM_STAT = Registries.createRegistryKey("custom_stat");
    public static final ResourceKey<Registry<DataComponentPredicate.Type<?>>> DATA_COMPONENT_PREDICATE_TYPE = Registries.createRegistryKey("data_component_predicate_type");
    public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_TYPE = Registries.createRegistryKey("data_component_type");
    public static final ResourceKey<Registry<GameRule<?>>> GAME_RULE = Registries.createRegistryKey("game_rule");
    public static final ResourceKey<Registry<DebugSubscription<?>>> DEBUG_SUBSCRIPTION = Registries.createRegistryKey("debug_subscription");
    public static final ResourceKey<Registry<DecoratedPotPattern>> DECORATED_POT_PATTERN = Registries.createRegistryKey("decorated_pot_pattern");
    public static final ResourceKey<Registry<MapCodec<? extends DensityFunction>>> DENSITY_FUNCTION_TYPE = Registries.createRegistryKey("worldgen/density_function_type");
    public static final ResourceKey<Registry<MapCodec<? extends DialogBody>>> DIALOG_BODY_TYPE = Registries.createRegistryKey("dialog_body_type");
    public static final ResourceKey<Registry<MapCodec<? extends Dialog>>> DIALOG_TYPE = Registries.createRegistryKey("dialog_type");
    public static final ResourceKey<Registry<DataComponentType<?>>> ENCHANTMENT_EFFECT_COMPONENT_TYPE = Registries.createRegistryKey("enchantment_effect_component_type");
    public static final ResourceKey<Registry<MapCodec<? extends EnchantmentEntityEffect>>> ENCHANTMENT_ENTITY_EFFECT_TYPE = Registries.createRegistryKey("enchantment_entity_effect_type");
    public static final ResourceKey<Registry<MapCodec<? extends LevelBasedValue>>> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = Registries.createRegistryKey("enchantment_level_based_value_type");
    public static final ResourceKey<Registry<MapCodec<? extends EnchantmentLocationBasedEffect>>> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = Registries.createRegistryKey("enchantment_location_based_effect_type");
    public static final ResourceKey<Registry<MapCodec<? extends EnchantmentProvider>>> ENCHANTMENT_PROVIDER_TYPE = Registries.createRegistryKey("enchantment_provider_type");
    public static final ResourceKey<Registry<MapCodec<? extends EnchantmentValueEffect>>> ENCHANTMENT_VALUE_EFFECT_TYPE = Registries.createRegistryKey("enchantment_value_effect_type");
    public static final ResourceKey<Registry<Codec<? extends EntitySubPredicate>>> ENTITY_SUB_PREDICATE_TYPE = Registries.createRegistryKey("entity_sub_predicate_type");
    public static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE = Registries.createRegistryKey("entity_type");
    public static final ResourceKey<Registry<EnvironmentAttribute<?>>> ENVIRONMENT_ATTRIBUTE = Registries.createRegistryKey("environment_attribute");
    public static final ResourceKey<Registry<AttributeType<?>>> ATTRIBUTE_TYPE = Registries.createRegistryKey("attribute_type");
    public static final ResourceKey<Registry<FeatureSizeType<?>>> FEATURE_SIZE_TYPE = Registries.createRegistryKey("worldgen/feature_size_type");
    public static final ResourceKey<Registry<Feature<?>>> FEATURE = Registries.createRegistryKey("worldgen/feature");
    public static final ResourceKey<Registry<MapCodec<? extends FloatProvider>>> FLOAT_PROVIDER_TYPE = Registries.createRegistryKey("float_provider_type");
    public static final ResourceKey<Registry<Fluid>> FLUID = Registries.createRegistryKey("fluid");
    public static final ResourceKey<Registry<FoliagePlacerType<?>>> FOLIAGE_PLACER_TYPE = Registries.createRegistryKey("worldgen/foliage_placer_type");
    public static final ResourceKey<Registry<GameEvent>> GAME_EVENT = Registries.createRegistryKey("game_event");
    public static final ResourceKey<Registry<HeightProviderType<?>>> HEIGHT_PROVIDER_TYPE = Registries.createRegistryKey("height_provider_type");
    public static final ResourceKey<Registry<MapCodec<? extends InputControl>>> INPUT_CONTROL_TYPE = Registries.createRegistryKey("input_control_type");
    public static final ResourceKey<Registry<MapCodec<? extends IntProvider>>> INT_PROVIDER_TYPE = Registries.createRegistryKey("int_provider_type");
    public static final ResourceKey<Registry<Item>> ITEM = Registries.createRegistryKey("item");
    public static final ResourceKey<Registry<MapCodec<? extends SlotSource>>> SLOT_SOURCE_TYPE = Registries.createRegistryKey("slot_source_type");
    public static final ResourceKey<Registry<MapCodec<? extends LootItemCondition>>> LOOT_CONDITION_TYPE = Registries.createRegistryKey("loot_condition_type");
    public static final ResourceKey<Registry<MapCodec<? extends LootItemFunction>>> LOOT_FUNCTION_TYPE = Registries.createRegistryKey("loot_function_type");
    public static final ResourceKey<Registry<MapCodec<? extends NbtProvider>>> LOOT_NBT_PROVIDER_TYPE = Registries.createRegistryKey("loot_nbt_provider_type");
    public static final ResourceKey<Registry<MapCodec<? extends NumberProvider>>> LOOT_NUMBER_PROVIDER_TYPE = Registries.createRegistryKey("loot_number_provider_type");
    public static final ResourceKey<Registry<MapCodec<? extends LootPoolEntryContainer>>> LOOT_POOL_ENTRY_TYPE = Registries.createRegistryKey("loot_pool_entry_type");
    public static final ResourceKey<Registry<MapCodec<? extends ScoreboardNameProvider>>> LOOT_SCORE_PROVIDER_TYPE = Registries.createRegistryKey("loot_score_provider_type");
    public static final ResourceKey<Registry<MapDecorationType>> MAP_DECORATION_TYPE = Registries.createRegistryKey("map_decoration_type");
    public static final ResourceKey<Registry<MapCodec<? extends SurfaceRules.ConditionSource>>> MATERIAL_CONDITION = Registries.createRegistryKey("worldgen/material_condition");
    public static final ResourceKey<Registry<MapCodec<? extends SurfaceRules.RuleSource>>> MATERIAL_RULE = Registries.createRegistryKey("worldgen/material_rule");
    public static final ResourceKey<Registry<MemoryModuleType<?>>> MEMORY_MODULE_TYPE = Registries.createRegistryKey("memory_module_type");
    public static final ResourceKey<Registry<MenuType<?>>> MENU = Registries.createRegistryKey("menu");
    public static final ResourceKey<Registry<MobEffect>> MOB_EFFECT = Registries.createRegistryKey("mob_effect");
    public static final ResourceKey<Registry<NumberFormatType<?>>> NUMBER_FORMAT_TYPE = Registries.createRegistryKey("number_format_type");
    public static final ResourceKey<Registry<ParticleType<?>>> PARTICLE_TYPE = Registries.createRegistryKey("particle_type");
    public static final ResourceKey<Registry<PlacementModifierType<?>>> PLACEMENT_MODIFIER_TYPE = Registries.createRegistryKey("worldgen/placement_modifier_type");
    public static final ResourceKey<Registry<PoiType>> POINT_OF_INTEREST_TYPE = Registries.createRegistryKey("point_of_interest_type");
    public static final ResourceKey<Registry<MapCodec<? extends PoolAliasBinding>>> POOL_ALIAS_BINDING = Registries.createRegistryKey("worldgen/pool_alias_binding");
    public static final ResourceKey<Registry<PositionSourceType<?>>> POSITION_SOURCE_TYPE = Registries.createRegistryKey("position_source_type");
    public static final ResourceKey<Registry<PosRuleTestType<?>>> POS_RULE_TEST = Registries.createRegistryKey("pos_rule_test");
    public static final ResourceKey<Registry<Potion>> POTION = Registries.createRegistryKey("potion");
    public static final ResourceKey<Registry<RecipeBookCategory>> RECIPE_BOOK_CATEGORY = Registries.createRegistryKey("recipe_book_category");
    public static final ResourceKey<Registry<RecipeDisplay.Type<?>>> RECIPE_DISPLAY = Registries.createRegistryKey("recipe_display");
    public static final ResourceKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZER = Registries.createRegistryKey("recipe_serializer");
    public static final ResourceKey<Registry<RecipeType<?>>> RECIPE_TYPE = Registries.createRegistryKey("recipe_type");
    public static final ResourceKey<Registry<RootPlacerType<?>>> ROOT_PLACER_TYPE = Registries.createRegistryKey("worldgen/root_placer_type");
    public static final ResourceKey<Registry<RuleBlockEntityModifierType<?>>> RULE_BLOCK_ENTITY_MODIFIER = Registries.createRegistryKey("rule_block_entity_modifier");
    public static final ResourceKey<Registry<RuleTestType<?>>> RULE_TEST = Registries.createRegistryKey("rule_test");
    public static final ResourceKey<Registry<SensorType<?>>> SENSOR_TYPE = Registries.createRegistryKey("sensor_type");
    public static final ResourceKey<Registry<SlotDisplay.Type<?>>> SLOT_DISPLAY = Registries.createRegistryKey("slot_display");
    public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT = Registries.createRegistryKey("sound_event");
    public static final ResourceKey<Registry<MapCodec<? extends SpawnCondition>>> SPAWN_CONDITION_TYPE = Registries.createRegistryKey("spawn_condition_type");
    public static final ResourceKey<Registry<StatType<?>>> STAT_TYPE = Registries.createRegistryKey("stat_type");
    public static final ResourceKey<Registry<StructurePieceType>> STRUCTURE_PIECE = Registries.createRegistryKey("worldgen/structure_piece");
    public static final ResourceKey<Registry<StructurePlacementType<?>>> STRUCTURE_PLACEMENT = Registries.createRegistryKey("worldgen/structure_placement");
    public static final ResourceKey<Registry<StructurePoolElementType<?>>> STRUCTURE_POOL_ELEMENT = Registries.createRegistryKey("worldgen/structure_pool_element");
    public static final ResourceKey<Registry<MapCodec<? extends StructureProcessor>>> STRUCTURE_PROCESSOR = Registries.createRegistryKey("worldgen/structure_processor");
    public static final ResourceKey<Registry<StructureType<?>>> STRUCTURE_TYPE = Registries.createRegistryKey("worldgen/structure_type");
    public static final ResourceKey<Registry<MapCodec<? extends Action>>> DIALOG_ACTION_TYPE = Registries.createRegistryKey("dialog_action_type");
    public static final ResourceKey<Registry<MapCodec<? extends TestEnvironmentDefinition<?>>>> TEST_ENVIRONMENT_DEFINITION_TYPE = Registries.createRegistryKey("test_environment_definition_type");
    public static final ResourceKey<Registry<Consumer<GameTestHelper>>> TEST_FUNCTION = Registries.createRegistryKey("test_function");
    public static final ResourceKey<Registry<MapCodec<? extends GameTestInstance>>> TEST_INSTANCE_TYPE = Registries.createRegistryKey("test_instance_type");
    public static final ResourceKey<Registry<TicketType>> TICKET_TYPE = Registries.createRegistryKey("ticket_type");
    public static final ResourceKey<Registry<TreeDecoratorType<?>>> TREE_DECORATOR_TYPE = Registries.createRegistryKey("worldgen/tree_decorator_type");
    public static final ResourceKey<Registry<TrunkPlacerType<?>>> TRUNK_PLACER_TYPE = Registries.createRegistryKey("worldgen/trunk_placer_type");
    public static final ResourceKey<Registry<VillagerProfession>> VILLAGER_PROFESSION = Registries.createRegistryKey("villager_profession");
    public static final ResourceKey<Registry<VillagerType>> VILLAGER_TYPE = Registries.createRegistryKey("villager_type");
    public static final ResourceKey<Registry<IncomingRpcMethod<?, ?>>> INCOMING_RPC_METHOD = Registries.createRegistryKey("incoming_rpc_methods");
    public static final ResourceKey<Registry<OutgoingRpcMethod<?, ?>>> OUTGOING_RPC_METHOD = Registries.createRegistryKey("outgoing_rpc_methods");
    public static final ResourceKey<Registry<MapCodec<? extends Permission>>> PERMISSION_TYPE = Registries.createRegistryKey("permission_type");
    public static final ResourceKey<Registry<MapCodec<? extends PermissionCheck>>> PERMISSION_CHECK_TYPE = Registries.createRegistryKey("permission_check_type");
    public static final ResourceKey<Registry<BannerPattern>> BANNER_PATTERN = Registries.createRegistryKey("banner_pattern");
    public static final ResourceKey<Registry<Biome>> BIOME = Registries.createRegistryKey("worldgen/biome");
    public static final ResourceKey<Registry<CatSoundVariant>> CAT_SOUND_VARIANT = Registries.createRegistryKey("cat_sound_variant");
    public static final ResourceKey<Registry<CatVariant>> CAT_VARIANT = Registries.createRegistryKey("cat_variant");
    public static final ResourceKey<Registry<ChatType>> CHAT_TYPE = Registries.createRegistryKey("chat_type");
    public static final ResourceKey<Registry<ChickenSoundVariant>> CHICKEN_SOUND_VARIANT = Registries.createRegistryKey("chicken_sound_variant");
    public static final ResourceKey<Registry<ChickenVariant>> CHICKEN_VARIANT = Registries.createRegistryKey("chicken_variant");
    public static final ResourceKey<Registry<ZombieNautilusVariant>> ZOMBIE_NAUTILUS_VARIANT = Registries.createRegistryKey("zombie_nautilus_variant");
    public static final ResourceKey<Registry<ConfiguredWorldCarver<?>>> CONFIGURED_CARVER = Registries.createRegistryKey("worldgen/configured_carver");
    public static final ResourceKey<Registry<ConfiguredFeature<?, ?>>> CONFIGURED_FEATURE = Registries.createRegistryKey("worldgen/configured_feature");
    public static final ResourceKey<Registry<CowSoundVariant>> COW_SOUND_VARIANT = Registries.createRegistryKey("cow_sound_variant");
    public static final ResourceKey<Registry<CowVariant>> COW_VARIANT = Registries.createRegistryKey("cow_variant");
    public static final ResourceKey<Registry<DamageType>> DAMAGE_TYPE = Registries.createRegistryKey("damage_type");
    public static final ResourceKey<Registry<DensityFunction>> DENSITY_FUNCTION = Registries.createRegistryKey("worldgen/density_function");
    public static final ResourceKey<Registry<Dialog>> DIALOG = Registries.createRegistryKey("dialog");
    public static final ResourceKey<Registry<DimensionType>> DIMENSION_TYPE = Registries.createRegistryKey("dimension_type");
    public static final ResourceKey<Registry<EnchantmentProvider>> ENCHANTMENT_PROVIDER = Registries.createRegistryKey("enchantment_provider");
    public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT = Registries.createRegistryKey("enchantment");
    public static final ResourceKey<Registry<FlatLevelGeneratorPreset>> FLAT_LEVEL_GENERATOR_PRESET = Registries.createRegistryKey("worldgen/flat_level_generator_preset");
    public static final ResourceKey<Registry<FrogVariant>> FROG_VARIANT = Registries.createRegistryKey("frog_variant");
    public static final ResourceKey<Registry<Instrument>> INSTRUMENT = Registries.createRegistryKey("instrument");
    public static final ResourceKey<Registry<JukeboxSong>> JUKEBOX_SONG = Registries.createRegistryKey("jukebox_song");
    public static final ResourceKey<Registry<MultiNoiseBiomeSourceParameterList>> MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = Registries.createRegistryKey("worldgen/multi_noise_biome_source_parameter_list");
    public static final ResourceKey<Registry<NoiseGeneratorSettings>> NOISE_SETTINGS = Registries.createRegistryKey("worldgen/noise_settings");
    public static final ResourceKey<Registry<NormalNoise.NoiseParameters>> NOISE = Registries.createRegistryKey("worldgen/noise");
    public static final ResourceKey<Registry<PaintingVariant>> PAINTING_VARIANT = Registries.createRegistryKey("painting_variant");
    public static final ResourceKey<Registry<PigSoundVariant>> PIG_SOUND_VARIANT = Registries.createRegistryKey("pig_sound_variant");
    public static final ResourceKey<Registry<PigVariant>> PIG_VARIANT = Registries.createRegistryKey("pig_variant");
    public static final ResourceKey<Registry<PlacedFeature>> PLACED_FEATURE = Registries.createRegistryKey("worldgen/placed_feature");
    public static final ResourceKey<Registry<StructureProcessorList>> PROCESSOR_LIST = Registries.createRegistryKey("worldgen/processor_list");
    public static final ResourceKey<Registry<StructureSet>> STRUCTURE_SET = Registries.createRegistryKey("worldgen/structure_set");
    public static final ResourceKey<Registry<Structure>> STRUCTURE = Registries.createRegistryKey("worldgen/structure");
    public static final ResourceKey<Registry<SulfurCubeArchetype>> SULFUR_CUBE_ARCHETYPE = Registries.createRegistryKey("sulfur_cube_archetype");
    public static final ResourceKey<Registry<StructureTemplatePool>> TEMPLATE_POOL = Registries.createRegistryKey("worldgen/template_pool");
    public static final ResourceKey<Registry<TestEnvironmentDefinition<?>>> TEST_ENVIRONMENT = Registries.createRegistryKey("test_environment");
    public static final ResourceKey<Registry<GameTestInstance>> TEST_INSTANCE = Registries.createRegistryKey("test_instance");
    public static final ResourceKey<Registry<Timeline>> TIMELINE = Registries.createRegistryKey("timeline");
    public static final ResourceKey<Registry<TradeSet>> TRADE_SET = Registries.createRegistryKey("trade_set");
    public static final ResourceKey<Registry<TrialSpawnerConfig>> TRIAL_SPAWNER_CONFIG = Registries.createRegistryKey("trial_spawner");
    public static final ResourceKey<Registry<CriterionTrigger<?>>> TRIGGER_TYPE = Registries.createRegistryKey("trigger_type");
    public static final ResourceKey<Registry<TrimMaterial>> TRIM_MATERIAL = Registries.createRegistryKey("trim_material");
    public static final ResourceKey<Registry<TrimPattern>> TRIM_PATTERN = Registries.createRegistryKey("trim_pattern");
    public static final ResourceKey<Registry<VillagerTrade>> VILLAGER_TRADE = Registries.createRegistryKey("villager_trade");
    public static final ResourceKey<Registry<WolfVariant>> WOLF_VARIANT = Registries.createRegistryKey("wolf_variant");
    public static final ResourceKey<Registry<WolfSoundVariant>> WOLF_SOUND_VARIANT = Registries.createRegistryKey("wolf_sound_variant");
    public static final ResourceKey<Registry<WorldClock>> WORLD_CLOCK = Registries.createRegistryKey("world_clock");
    public static final ResourceKey<Registry<WorldPreset>> WORLD_PRESET = Registries.createRegistryKey("worldgen/world_preset");
    public static final ResourceKey<Registry<Level>> DIMENSION = Registries.createRegistryKey("dimension");
    public static final ResourceKey<Registry<LevelStem>> LEVEL_STEM = Registries.createRegistryKey("dimension");
    public static final ResourceKey<Registry<LootTable>> LOOT_TABLE = Registries.createRegistryKey("loot_table");
    public static final ResourceKey<Registry<LootItemFunction>> ITEM_MODIFIER = Registries.createRegistryKey("item_modifier");
    public static final ResourceKey<Registry<LootItemCondition>> PREDICATE = Registries.createRegistryKey("predicate");
    public static final ResourceKey<Registry<Advancement>> ADVANCEMENT = Registries.createRegistryKey("advancement");
    public static final ResourceKey<Registry<Recipe<?>>> RECIPE = Registries.createRegistryKey("recipe");

    public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> levelStem) {
        return ResourceKey.create(DIMENSION, levelStem.identifier());
    }

    public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> level) {
        return ResourceKey.create(LEVEL_STEM, level.identifier());
    }

    private static <T> ResourceKey<Registry<T>> createRegistryKey(String name) {
        return ResourceKey.createRegistryKey(Identifier.withDefaultNamespace(name));
    }

    private static String registryDirPath(ResourceKey<? extends Registry<?>> registryKey) {
        return registryKey.identifier().getPath();
    }

    public static String elementsDirPath(ResourceKey<? extends Registry<?>> registryKey) {
        return Registries.registryDirPath(registryKey);
    }

    public static String tagsDirPath(ResourceKey<? extends Registry<?>> registryKey) {
        return "tags/" + Registries.registryDirPath(registryKey);
    }

    public static String componentsDirPath(ResourceKey<? extends Registry<?>> registryKey) {
        return "components/" + Registries.registryDirPath(registryKey);
    }
}

