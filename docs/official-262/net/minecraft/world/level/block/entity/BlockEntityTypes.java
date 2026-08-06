/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypeIds;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.slf4j.Logger;

public class BlockEntityTypes {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BlockEntityType<FurnaceBlockEntity> FURNACE = BlockEntityTypes.register(BlockEntityTypeIds.FURNACE, FurnaceBlockEntity::new, Blocks.FURNACE);
    public static final BlockEntityType<ChestBlockEntity> CHEST = BlockEntityTypes.register(BlockEntityTypeIds.CHEST, ChestBlockEntity::new, Util.copyAndAdd(Blocks.COPPER_CHEST.asList(), Blocks.CHEST));
    public static final BlockEntityType<TrappedChestBlockEntity> TRAPPED_CHEST = BlockEntityTypes.register(BlockEntityTypeIds.TRAPPED_CHEST, TrappedChestBlockEntity::new, Blocks.TRAPPED_CHEST);
    public static final BlockEntityType<EnderChestBlockEntity> ENDER_CHEST = BlockEntityTypes.register(BlockEntityTypeIds.ENDER_CHEST, EnderChestBlockEntity::new, Blocks.ENDER_CHEST);
    public static final BlockEntityType<JukeboxBlockEntity> JUKEBOX = BlockEntityTypes.register(BlockEntityTypeIds.JUKEBOX, JukeboxBlockEntity::new, Blocks.JUKEBOX);
    public static final BlockEntityType<DispenserBlockEntity> DISPENSER = BlockEntityTypes.register(BlockEntityTypeIds.DISPENSER, DispenserBlockEntity::new, Blocks.DISPENSER);
    public static final BlockEntityType<DropperBlockEntity> DROPPER = BlockEntityTypes.register(BlockEntityTypeIds.DROPPER, DropperBlockEntity::new, Blocks.DROPPER);
    public static final BlockEntityType<SignBlockEntity> SIGN = BlockEntityTypes.register(BlockEntityTypeIds.SIGN, SignBlockEntity::new, Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.ACACIA_SIGN, Blocks.CHERRY_SIGN, Blocks.JUNGLE_SIGN, Blocks.DARK_OAK_SIGN, Blocks.PALE_OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.CHERRY_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.PALE_OAK_WALL_SIGN, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN, Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.BAMBOO_SIGN, Blocks.BAMBOO_WALL_SIGN);
    public static final BlockEntityType<HangingSignBlockEntity> HANGING_SIGN = BlockEntityTypes.register(BlockEntityTypeIds.HANGING_SIGN, HangingSignBlockEntity::new, Blocks.OAK_HANGING_SIGN, Blocks.SPRUCE_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.ACACIA_HANGING_SIGN, Blocks.CHERRY_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN, Blocks.DARK_OAK_HANGING_SIGN, Blocks.PALE_OAK_HANGING_SIGN, Blocks.CRIMSON_HANGING_SIGN, Blocks.WARPED_HANGING_SIGN, Blocks.MANGROVE_HANGING_SIGN, Blocks.BAMBOO_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN, Blocks.ACACIA_WALL_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN, Blocks.PALE_OAK_WALL_HANGING_SIGN, Blocks.CRIMSON_WALL_HANGING_SIGN, Blocks.WARPED_WALL_HANGING_SIGN, Blocks.MANGROVE_WALL_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN);
    public static final BlockEntityType<SpawnerBlockEntity> MOB_SPAWNER = BlockEntityTypes.register(BlockEntityTypeIds.MOB_SPAWNER, SpawnerBlockEntity::new, Blocks.SPAWNER);
    public static final BlockEntityType<CreakingHeartBlockEntity> CREAKING_HEART = BlockEntityTypes.register(BlockEntityTypeIds.CREAKING_HEART, CreakingHeartBlockEntity::new, Blocks.CREAKING_HEART);
    public static final BlockEntityType<PistonMovingBlockEntity> PISTON = BlockEntityTypes.register(BlockEntityTypeIds.PISTON, PistonMovingBlockEntity::new, Blocks.MOVING_PISTON);
    public static final BlockEntityType<BrewingStandBlockEntity> BREWING_STAND = BlockEntityTypes.register(BlockEntityTypeIds.BREWING_STAND, BrewingStandBlockEntity::new, Blocks.BREWING_STAND);
    public static final BlockEntityType<EnchantingTableBlockEntity> ENCHANTING_TABLE = BlockEntityTypes.register(BlockEntityTypeIds.ENCHANTING_TABLE, EnchantingTableBlockEntity::new, Blocks.ENCHANTING_TABLE);
    public static final BlockEntityType<TheEndPortalBlockEntity> END_PORTAL = BlockEntityTypes.register(BlockEntityTypeIds.END_PORTAL, TheEndPortalBlockEntity::new, Blocks.END_PORTAL);
    public static final BlockEntityType<BeaconBlockEntity> BEACON = BlockEntityTypes.register(BlockEntityTypeIds.BEACON, BeaconBlockEntity::new, Blocks.BEACON);
    public static final BlockEntityType<SkullBlockEntity> SKULL = BlockEntityTypes.register(BlockEntityTypeIds.SKULL, SkullBlockEntity::new, Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD, Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL, Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD);
    public static final BlockEntityType<DaylightDetectorBlockEntity> DAYLIGHT_DETECTOR = BlockEntityTypes.register(BlockEntityTypeIds.DAYLIGHT_DETECTOR, DaylightDetectorBlockEntity::new, Blocks.DAYLIGHT_DETECTOR);
    public static final BlockEntityType<HopperBlockEntity> HOPPER = BlockEntityTypes.register(BlockEntityTypeIds.HOPPER, HopperBlockEntity::new, Blocks.HOPPER);
    public static final BlockEntityType<ComparatorBlockEntity> COMPARATOR = BlockEntityTypes.register(BlockEntityTypeIds.COMPARATOR, ComparatorBlockEntity::new, Blocks.COMPARATOR);
    public static final BlockEntityType<BannerBlockEntity> BANNER = BlockEntityTypes.register(BlockEntityTypeIds.BANNER, BannerBlockEntity::new, Util.join(Blocks.BANNER.asList(), Blocks.WALL_BANNER.asList()));
    public static final BlockEntityType<StructureBlockEntity> STRUCTURE_BLOCK = BlockEntityTypes.register(BlockEntityTypeIds.STRUCTURE_BLOCK, StructureBlockEntity::new, Blocks.STRUCTURE_BLOCK);
    public static final BlockEntityType<TheEndGatewayBlockEntity> END_GATEWAY = BlockEntityTypes.register(BlockEntityTypeIds.END_GATEWAY, TheEndGatewayBlockEntity::new, Blocks.END_GATEWAY);
    public static final BlockEntityType<CommandBlockEntity> COMMAND_BLOCK = BlockEntityTypes.register(BlockEntityTypeIds.COMMAND_BLOCK, CommandBlockEntity::new, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK);
    public static final BlockEntityType<ShulkerBoxBlockEntity> SHULKER_BOX = BlockEntityTypes.register(BlockEntityTypeIds.SHULKER_BOX, ShulkerBoxBlockEntity::new, Util.copyAndAdd(Blocks.DYED_SHULKER_BOX.asList(), Blocks.SHULKER_BOX));
    public static final BlockEntityType<ConduitBlockEntity> CONDUIT = BlockEntityTypes.register(BlockEntityTypeIds.CONDUIT, ConduitBlockEntity::new, Blocks.CONDUIT);
    public static final BlockEntityType<BarrelBlockEntity> BARREL = BlockEntityTypes.register(BlockEntityTypeIds.BARREL, BarrelBlockEntity::new, Blocks.BARREL);
    public static final BlockEntityType<SmokerBlockEntity> SMOKER = BlockEntityTypes.register(BlockEntityTypeIds.SMOKER, SmokerBlockEntity::new, Blocks.SMOKER);
    public static final BlockEntityType<BlastFurnaceBlockEntity> BLAST_FURNACE = BlockEntityTypes.register(BlockEntityTypeIds.BLAST_FURNACE, BlastFurnaceBlockEntity::new, Blocks.BLAST_FURNACE);
    public static final BlockEntityType<LecternBlockEntity> LECTERN = BlockEntityTypes.register(BlockEntityTypeIds.LECTERN, LecternBlockEntity::new, Blocks.LECTERN);
    public static final BlockEntityType<BellBlockEntity> BELL = BlockEntityTypes.register(BlockEntityTypeIds.BELL, BellBlockEntity::new, Blocks.BELL);
    public static final BlockEntityType<JigsawBlockEntity> JIGSAW = BlockEntityTypes.register(BlockEntityTypeIds.JIGSAW, JigsawBlockEntity::new, Blocks.JIGSAW);
    public static final BlockEntityType<CampfireBlockEntity> CAMPFIRE = BlockEntityTypes.register(BlockEntityTypeIds.CAMPFIRE, CampfireBlockEntity::new, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
    public static final BlockEntityType<BeehiveBlockEntity> BEEHIVE = BlockEntityTypes.register(BlockEntityTypeIds.BEEHIVE, BeehiveBlockEntity::new, Blocks.BEE_NEST, Blocks.BEEHIVE);
    public static final BlockEntityType<SculkSensorBlockEntity> SCULK_SENSOR = BlockEntityTypes.register(BlockEntityTypeIds.SCULK_SENSOR, SculkSensorBlockEntity::new, Blocks.SCULK_SENSOR);
    public static final BlockEntityType<CalibratedSculkSensorBlockEntity> CALIBRATED_SCULK_SENSOR = BlockEntityTypes.register(BlockEntityTypeIds.CALIBRATED_SCULK_SENSOR, CalibratedSculkSensorBlockEntity::new, Blocks.CALIBRATED_SCULK_SENSOR);
    public static final BlockEntityType<SculkCatalystBlockEntity> SCULK_CATALYST = BlockEntityTypes.register(BlockEntityTypeIds.SCULK_CATALYST, SculkCatalystBlockEntity::new, Blocks.SCULK_CATALYST);
    public static final BlockEntityType<SculkShriekerBlockEntity> SCULK_SHRIEKER = BlockEntityTypes.register(BlockEntityTypeIds.SCULK_SHRIEKER, SculkShriekerBlockEntity::new, Blocks.SCULK_SHRIEKER);
    public static final BlockEntityType<ChiseledBookShelfBlockEntity> CHISELED_BOOKSHELF = BlockEntityTypes.register(BlockEntityTypeIds.CHISELED_BOOKSHELF, ChiseledBookShelfBlockEntity::new, Blocks.CHISELED_BOOKSHELF);
    public static final BlockEntityType<ShelfBlockEntity> SHELF = BlockEntityTypes.register(BlockEntityTypeIds.SHELF, ShelfBlockEntity::new, Blocks.ACACIA_SHELF, Blocks.BAMBOO_SHELF, Blocks.BIRCH_SHELF, Blocks.CHERRY_SHELF, Blocks.CRIMSON_SHELF, Blocks.DARK_OAK_SHELF, Blocks.JUNGLE_SHELF, Blocks.MANGROVE_SHELF, Blocks.OAK_SHELF, Blocks.PALE_OAK_SHELF, Blocks.SPRUCE_SHELF, Blocks.WARPED_SHELF);
    public static final BlockEntityType<BrushableBlockEntity> BRUSHABLE_BLOCK = BlockEntityTypes.register(BlockEntityTypeIds.BRUSHABLE_BLOCK, BrushableBlockEntity::new, Blocks.SUSPICIOUS_SAND, Blocks.SUSPICIOUS_GRAVEL);
    public static final BlockEntityType<DecoratedPotBlockEntity> DECORATED_POT = BlockEntityTypes.register(BlockEntityTypeIds.DECORATED_POT, DecoratedPotBlockEntity::new, Blocks.DECORATED_POT);
    public static final BlockEntityType<CrafterBlockEntity> CRAFTER = BlockEntityTypes.register(BlockEntityTypeIds.CRAFTER, CrafterBlockEntity::new, Blocks.CRAFTER);
    public static final BlockEntityType<TrialSpawnerBlockEntity> TRIAL_SPAWNER = BlockEntityTypes.register(BlockEntityTypeIds.TRIAL_SPAWNER, TrialSpawnerBlockEntity::new, Blocks.TRIAL_SPAWNER);
    public static final BlockEntityType<VaultBlockEntity> VAULT = BlockEntityTypes.register(BlockEntityTypeIds.VAULT, VaultBlockEntity::new, Blocks.VAULT);
    public static final BlockEntityType<TestBlockEntity> TEST_BLOCK = BlockEntityTypes.register(BlockEntityTypeIds.TEST_BLOCK, TestBlockEntity::new, Blocks.TEST_BLOCK);
    public static final BlockEntityType<TestInstanceBlockEntity> TEST_INSTANCE_BLOCK = BlockEntityTypes.register(BlockEntityTypeIds.TEST_INSTANCE_BLOCK, TestInstanceBlockEntity::new, Blocks.TEST_INSTANCE_BLOCK);
    public static final BlockEntityType<CopperGolemStatueBlockEntity> COPPER_GOLEM_STATUE = BlockEntityTypes.register(BlockEntityTypeIds.COPPER_GOLEM_STATUE, CopperGolemStatueBlockEntity::new, Blocks.COPPER_GOLEM_STATUE.asList());
    public static final BlockEntityType<PotentSulfurBlockEntity> POTENT_SULFUR = BlockEntityTypes.register(BlockEntityTypeIds.POTENT_SULFUR, PotentSulfurBlockEntity::new, Blocks.POTENT_SULFUR);
    static final Set<BlockEntityType<?>> OP_ONLY_CUSTOM_DATA = Set.of(COMMAND_BLOCK, LECTERN, SIGN, HANGING_SIGN, MOB_SPAWNER, TRIAL_SPAWNER);

    private static <T extends BlockEntity> BlockEntityType<T> register(ResourceKey<BlockEntityType<?>> key, BlockEntityType.BlockEntitySupplier<? extends T> factory, Block ... validBlocks) {
        Identifier id = key.identifier();
        if (validBlocks.length == 0) {
            LOGGER.warn("Block entity type {} requires at least one valid block to be defined!", (Object)id);
        }
        if (id.getNamespace().equals("minecraft")) {
            Util.fetchChoiceType(References.BLOCK_ENTITY, id.getPath());
        }
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, new BlockEntityType<T>(factory, Set.of(validBlocks)));
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(ResourceKey<BlockEntityType<?>> id, BlockEntityType.BlockEntitySupplier<? extends T> factory, List<Block> validBlocks) {
        return BlockEntityTypes.register(id, factory, validBlocks.toArray(new Block[0]));
    }
}

