/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.ArmadilloRenderer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.AxolotlRenderer;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.BoggedRenderer;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.CamelHuskRenderer;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.CreakingRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.DolphinRenderer;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerFangsRenderer;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.client.renderer.entity.FrogRenderer;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.GiantMobRenderer;
import net.minecraft.client.renderer.entity.GlowSquidRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.IllusionerRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.LlamaRenderer;
import net.minecraft.client.renderer.entity.LlamaSpitRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.client.renderer.entity.NautilusRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.OcelotRenderer;
import net.minecraft.client.renderer.entity.OminousItemSpawnerRenderer;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.PandaRenderer;
import net.minecraft.client.renderer.entity.ParchedRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.RaftRenderer;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.client.renderer.entity.SpectralArrowRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.client.renderer.entity.StriderRenderer;
import net.minecraft.client.renderer.entity.SulfurCubeRenderer;
import net.minecraft.client.renderer.entity.TadpoleRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.TurtleRenderer;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.client.renderer.entity.ZombieNautilusRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.PlayerModelType;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class EntityRenderers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = new Object2ObjectOpenHashMap();

    private static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> renderer) {
        PROVIDERS.put(type, renderer);
    }

    public static Map<EntityType<?>, EntityRenderer<?, ?>> createEntityRenderers(EntityRendererProvider.Context context) {
        ImmutableMap.Builder result = ImmutableMap.builder();
        PROVIDERS.forEach((type, provider) -> {
            try {
                result.put(type, provider.create(context));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Failed to create model for " + String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)type)), e);
            }
        });
        return result.build();
    }

    public static <T extends Avatar> Map<PlayerModelType, AvatarRenderer<T>> createAvatarRenderers(EntityRendererProvider.Context context) {
        try {
            return Map.of(PlayerModelType.WIDE, new AvatarRenderer(context, false), PlayerModelType.SLIM, new AvatarRenderer(context, true));
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Failed to create avatar models", e);
        }
    }

    public static boolean validateRegistrations() {
        boolean hasAllModels = true;
        for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (entityType == EntityTypes.PLAYER || entityType == EntityTypes.MANNEQUIN || PROVIDERS.containsKey(entityType)) continue;
            LOGGER.warn("No renderer registered for {}", (Object)BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            hasAllModels = false;
        }
        return !hasAllModels;
    }

    static {
        EntityRenderers.register(EntityTypes.ACACIA_BOAT, context -> new BoatRenderer(context, ModelLayers.ACACIA_BOAT));
        EntityRenderers.register(EntityTypes.ACACIA_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.ACACIA_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.ALLAY, AllayRenderer::new);
        EntityRenderers.register(EntityTypes.AREA_EFFECT_CLOUD, NoopRenderer::new);
        EntityRenderers.register(EntityTypes.ARMADILLO, ArmadilloRenderer::new);
        EntityRenderers.register(EntityTypes.ARMOR_STAND, ArmorStandRenderer::new);
        EntityRenderers.register(EntityTypes.ARROW, TippableArrowRenderer::new);
        EntityRenderers.register(EntityTypes.AXOLOTL, AxolotlRenderer::new);
        EntityRenderers.register(EntityTypes.BAMBOO_CHEST_RAFT, context -> new RaftRenderer(context, ModelLayers.BAMBOO_CHEST_RAFT));
        EntityRenderers.register(EntityTypes.BAMBOO_RAFT, context -> new RaftRenderer(context, ModelLayers.BAMBOO_RAFT));
        EntityRenderers.register(EntityTypes.BAT, BatRenderer::new);
        EntityRenderers.register(EntityTypes.BEE, BeeRenderer::new);
        EntityRenderers.register(EntityTypes.BIRCH_BOAT, context -> new BoatRenderer(context, ModelLayers.BIRCH_BOAT));
        EntityRenderers.register(EntityTypes.BIRCH_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.BIRCH_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.BLAZE, BlazeRenderer::new);
        EntityRenderers.register(EntityTypes.BLOCK_DISPLAY, DisplayRenderer.BlockDisplayRenderer::new);
        EntityRenderers.register(EntityTypes.BOGGED, BoggedRenderer::new);
        EntityRenderers.register(EntityTypes.BREEZE, BreezeRenderer::new);
        EntityRenderers.register(EntityTypes.BREEZE_WIND_CHARGE, WindChargeRenderer::new);
        EntityRenderers.register(EntityTypes.CAMEL, CamelRenderer::new);
        EntityRenderers.register(EntityTypes.CAMEL_HUSK, CamelHuskRenderer::new);
        EntityRenderers.register(EntityTypes.CAT, CatRenderer::new);
        EntityRenderers.register(EntityTypes.CAVE_SPIDER, CaveSpiderRenderer::new);
        EntityRenderers.register(EntityTypes.CHERRY_BOAT, context -> new BoatRenderer(context, ModelLayers.CHERRY_BOAT));
        EntityRenderers.register(EntityTypes.CHERRY_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.CHERRY_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.CHEST_MINECART, context -> new MinecartRenderer(context, ModelLayers.CHEST_MINECART));
        EntityRenderers.register(EntityTypes.CHICKEN, ChickenRenderer::new);
        EntityRenderers.register(EntityTypes.COD, CodRenderer::new);
        EntityRenderers.register(EntityTypes.COMMAND_BLOCK_MINECART, context -> new MinecartRenderer(context, ModelLayers.COMMAND_BLOCK_MINECART));
        EntityRenderers.register(EntityTypes.COPPER_GOLEM, CopperGolemRenderer::new);
        EntityRenderers.register(EntityTypes.COW, CowRenderer::new);
        EntityRenderers.register(EntityTypes.CREAKING, CreakingRenderer::new);
        EntityRenderers.register(EntityTypes.CREEPER, CreeperRenderer::new);
        EntityRenderers.register(EntityTypes.DARK_OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.DARK_OAK_BOAT));
        EntityRenderers.register(EntityTypes.DARK_OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.DARK_OAK_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.DOLPHIN, DolphinRenderer::new);
        EntityRenderers.register(EntityTypes.DONKEY, context -> new DonkeyRenderer(context, EquipmentClientInfo.LayerType.DONKEY_SADDLE, ModelLayers.DONKEY_SADDLE, DonkeyRenderer.Type.DONKEY, DonkeyRenderer.Type.DONKEY_BABY));
        EntityRenderers.register(EntityTypes.DRAGON_FIREBALL, DragonFireballRenderer::new);
        EntityRenderers.register(EntityTypes.DROWNED, DrownedRenderer::new);
        EntityRenderers.register(EntityTypes.EGG, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.ELDER_GUARDIAN, ElderGuardianRenderer::new);
        EntityRenderers.register(EntityTypes.ENDERMAN, EndermanRenderer::new);
        EntityRenderers.register(EntityTypes.ENDERMITE, EndermiteRenderer::new);
        EntityRenderers.register(EntityTypes.ENDER_DRAGON, EnderDragonRenderer::new);
        EntityRenderers.register(EntityTypes.ENDER_PEARL, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.END_CRYSTAL, EndCrystalRenderer::new);
        EntityRenderers.register(EntityTypes.EVOKER, EvokerRenderer::new);
        EntityRenderers.register(EntityTypes.EVOKER_FANGS, EvokerFangsRenderer::new);
        EntityRenderers.register(EntityTypes.EXPERIENCE_BOTTLE, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.EXPERIENCE_ORB, ExperienceOrbRenderer::new);
        EntityRenderers.register(EntityTypes.EYE_OF_ENDER, context -> new ThrownItemRenderer(context, 1.0f, true));
        EntityRenderers.register(EntityTypes.FALLING_BLOCK, FallingBlockRenderer::new);
        EntityRenderers.register(EntityTypes.FIREBALL, context -> new ThrownItemRenderer(context, 3.0f, true));
        EntityRenderers.register(EntityTypes.FIREWORK_ROCKET, FireworkEntityRenderer::new);
        EntityRenderers.register(EntityTypes.FISHING_BOBBER, FishingHookRenderer::new);
        EntityRenderers.register(EntityTypes.FOX, FoxRenderer::new);
        EntityRenderers.register(EntityTypes.FROG, FrogRenderer::new);
        EntityRenderers.register(EntityTypes.FURNACE_MINECART, context -> new MinecartRenderer(context, ModelLayers.FURNACE_MINECART));
        EntityRenderers.register(EntityTypes.GHAST, GhastRenderer::new);
        EntityRenderers.register(EntityTypes.HAPPY_GHAST, HappyGhastRenderer::new);
        EntityRenderers.register(EntityTypes.GIANT, context -> new GiantMobRenderer(context, 6.0f));
        EntityRenderers.register(EntityTypes.GLOW_ITEM_FRAME, ItemFrameRenderer::new);
        EntityRenderers.register(EntityTypes.GLOW_SQUID, context -> new GlowSquidRenderer(context, new SquidModel(context.bakeLayer(ModelLayers.GLOW_SQUID)), new SquidModel(context.bakeLayer(ModelLayers.GLOW_SQUID_BABY))));
        EntityRenderers.register(EntityTypes.GOAT, GoatRenderer::new);
        EntityRenderers.register(EntityTypes.GUARDIAN, GuardianRenderer::new);
        EntityRenderers.register(EntityTypes.HOGLIN, HoglinRenderer::new);
        EntityRenderers.register(EntityTypes.HOPPER_MINECART, context -> new MinecartRenderer(context, ModelLayers.HOPPER_MINECART));
        EntityRenderers.register(EntityTypes.HORSE, HorseRenderer::new);
        EntityRenderers.register(EntityTypes.HUSK, HuskRenderer::new);
        EntityRenderers.register(EntityTypes.ILLUSIONER, IllusionerRenderer::new);
        EntityRenderers.register(EntityTypes.INTERACTION, NoopRenderer::new);
        EntityRenderers.register(EntityTypes.IRON_GOLEM, IronGolemRenderer::new);
        EntityRenderers.register(EntityTypes.ITEM, ItemEntityRenderer::new);
        EntityRenderers.register(EntityTypes.ITEM_DISPLAY, DisplayRenderer.ItemDisplayRenderer::new);
        EntityRenderers.register(EntityTypes.ITEM_FRAME, ItemFrameRenderer::new);
        EntityRenderers.register(EntityTypes.JUNGLE_BOAT, context -> new BoatRenderer(context, ModelLayers.JUNGLE_BOAT));
        EntityRenderers.register(EntityTypes.JUNGLE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.JUNGLE_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.LEASH_KNOT, LeashKnotRenderer::new);
        EntityRenderers.register(EntityTypes.LIGHTNING_BOLT, LightningBoltRenderer::new);
        EntityRenderers.register(EntityTypes.LINGERING_POTION, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.LLAMA, context -> new LlamaRenderer(context, ModelLayers.LLAMA, ModelLayers.LLAMA_BABY));
        EntityRenderers.register(EntityTypes.LLAMA_SPIT, LlamaSpitRenderer::new);
        EntityRenderers.register(EntityTypes.MAGMA_CUBE, MagmaCubeRenderer::new);
        EntityRenderers.register(EntityTypes.MANGROVE_BOAT, context -> new BoatRenderer(context, ModelLayers.MANGROVE_BOAT));
        EntityRenderers.register(EntityTypes.MANGROVE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.MANGROVE_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.MARKER, NoopRenderer::new);
        EntityRenderers.register(EntityTypes.MINECART, context -> new MinecartRenderer(context, ModelLayers.MINECART));
        EntityRenderers.register(EntityTypes.MOOSHROOM, MushroomCowRenderer::new);
        EntityRenderers.register(EntityTypes.MULE, context -> new DonkeyRenderer(context, EquipmentClientInfo.LayerType.MULE_SADDLE, ModelLayers.MULE_SADDLE, DonkeyRenderer.Type.MULE, DonkeyRenderer.Type.MULE_BABY));
        EntityRenderers.register(EntityTypes.NAUTILUS, NautilusRenderer::new);
        EntityRenderers.register(EntityTypes.OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.OAK_BOAT));
        EntityRenderers.register(EntityTypes.OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.OAK_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.OCELOT, OcelotRenderer::new);
        EntityRenderers.register(EntityTypes.OMINOUS_ITEM_SPAWNER, OminousItemSpawnerRenderer::new);
        EntityRenderers.register(EntityTypes.PAINTING, PaintingRenderer::new);
        EntityRenderers.register(EntityTypes.PALE_OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.PALE_OAK_BOAT));
        EntityRenderers.register(EntityTypes.PALE_OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.PALE_OAK_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.PANDA, PandaRenderer::new);
        EntityRenderers.register(EntityTypes.PARCHED, ParchedRenderer::new);
        EntityRenderers.register(EntityTypes.PARROT, ParrotRenderer::new);
        EntityRenderers.register(EntityTypes.PHANTOM, PhantomRenderer::new);
        EntityRenderers.register(EntityTypes.PIG, PigRenderer::new);
        EntityRenderers.register(EntityTypes.PIGLIN, context -> new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR));
        EntityRenderers.register(EntityTypes.PIGLIN_BRUTE, context -> new PiglinRenderer(context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BRUTE_ARMOR));
        EntityRenderers.register(EntityTypes.PILLAGER, PillagerRenderer::new);
        EntityRenderers.register(EntityTypes.POLAR_BEAR, PolarBearRenderer::new);
        EntityRenderers.register(EntityTypes.PUFFERFISH, PufferfishRenderer::new);
        EntityRenderers.register(EntityTypes.RABBIT, RabbitRenderer::new);
        EntityRenderers.register(EntityTypes.RAVAGER, RavagerRenderer::new);
        EntityRenderers.register(EntityTypes.SALMON, SalmonRenderer::new);
        EntityRenderers.register(EntityTypes.SHEEP, SheepRenderer::new);
        EntityRenderers.register(EntityTypes.SHULKER, ShulkerRenderer::new);
        EntityRenderers.register(EntityTypes.SHULKER_BULLET, ShulkerBulletRenderer::new);
        EntityRenderers.register(EntityTypes.SILVERFISH, SilverfishRenderer::new);
        EntityRenderers.register(EntityTypes.SKELETON, SkeletonRenderer::new);
        EntityRenderers.register(EntityTypes.SKELETON_HORSE, context -> new UndeadHorseRenderer(context, EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, ModelLayers.SKELETON_HORSE_SADDLE, UndeadHorseRenderer.Type.SKELETON, UndeadHorseRenderer.Type.SKELETON_BABY));
        EntityRenderers.register(EntityTypes.SLIME, SlimeRenderer::new);
        EntityRenderers.register(EntityTypes.SMALL_FIREBALL, context -> new ThrownItemRenderer(context, 0.75f, true));
        EntityRenderers.register(EntityTypes.SNIFFER, SnifferRenderer::new);
        EntityRenderers.register(EntityTypes.SNOWBALL, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.SNOW_GOLEM, SnowGolemRenderer::new);
        EntityRenderers.register(EntityTypes.SPAWNER_MINECART, context -> new MinecartRenderer(context, ModelLayers.SPAWNER_MINECART));
        EntityRenderers.register(EntityTypes.SPECTRAL_ARROW, SpectralArrowRenderer::new);
        EntityRenderers.register(EntityTypes.SPIDER, SpiderRenderer::new);
        EntityRenderers.register(EntityTypes.SPLASH_POTION, ThrownItemRenderer::new);
        EntityRenderers.register(EntityTypes.SPRUCE_BOAT, context -> new BoatRenderer(context, ModelLayers.SPRUCE_BOAT));
        EntityRenderers.register(EntityTypes.SPRUCE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.SPRUCE_CHEST_BOAT));
        EntityRenderers.register(EntityTypes.SQUID, context -> new SquidRenderer(context, new SquidModel(context.bakeLayer(ModelLayers.SQUID)), new SquidModel(context.bakeLayer(ModelLayers.SQUID_BABY))));
        EntityRenderers.register(EntityTypes.STRAY, StrayRenderer::new);
        EntityRenderers.register(EntityTypes.STRIDER, StriderRenderer::new);
        EntityRenderers.register(EntityTypes.SULFUR_CUBE, SulfurCubeRenderer::new);
        EntityRenderers.register(EntityTypes.TADPOLE, TadpoleRenderer::new);
        EntityRenderers.register(EntityTypes.TEXT_DISPLAY, DisplayRenderer.TextDisplayRenderer::new);
        EntityRenderers.register(EntityTypes.TNT, TntRenderer::new);
        EntityRenderers.register(EntityTypes.TNT_MINECART, TntMinecartRenderer::new);
        EntityRenderers.register(EntityTypes.TRADER_LLAMA, context -> new LlamaRenderer(context, ModelLayers.TRADER_LLAMA, ModelLayers.TRADER_LLAMA_BABY));
        EntityRenderers.register(EntityTypes.TRIDENT, ThrownTridentRenderer::new);
        EntityRenderers.register(EntityTypes.TROPICAL_FISH, TropicalFishRenderer::new);
        EntityRenderers.register(EntityTypes.TURTLE, TurtleRenderer::new);
        EntityRenderers.register(EntityTypes.VEX, VexRenderer::new);
        EntityRenderers.register(EntityTypes.VILLAGER, VillagerRenderer::new);
        EntityRenderers.register(EntityTypes.VINDICATOR, VindicatorRenderer::new);
        EntityRenderers.register(EntityTypes.WANDERING_TRADER, WanderingTraderRenderer::new);
        EntityRenderers.register(EntityTypes.WARDEN, WardenRenderer::new);
        EntityRenderers.register(EntityTypes.WIND_CHARGE, WindChargeRenderer::new);
        EntityRenderers.register(EntityTypes.WITCH, WitchRenderer::new);
        EntityRenderers.register(EntityTypes.WITHER, WitherBossRenderer::new);
        EntityRenderers.register(EntityTypes.WITHER_SKELETON, WitherSkeletonRenderer::new);
        EntityRenderers.register(EntityTypes.WITHER_SKULL, WitherSkullRenderer::new);
        EntityRenderers.register(EntityTypes.WOLF, WolfRenderer::new);
        EntityRenderers.register(EntityTypes.ZOGLIN, ZoglinRenderer::new);
        EntityRenderers.register(EntityTypes.ZOMBIE, ZombieRenderer::new);
        EntityRenderers.register(EntityTypes.ZOMBIE_HORSE, context -> new UndeadHorseRenderer(context, EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, ModelLayers.ZOMBIE_HORSE_SADDLE, UndeadHorseRenderer.Type.ZOMBIE, UndeadHorseRenderer.Type.ZOMBIE_BABY));
        EntityRenderers.register(EntityTypes.ZOMBIE_NAUTILUS, ZombieNautilusRenderer::new);
        EntityRenderers.register(EntityTypes.ZOMBIE_VILLAGER, ZombieVillagerRenderer::new);
        EntityRenderers.register(EntityTypes.ZOMBIFIED_PIGLIN, context -> new ZombifiedPiglinRenderer(context, ModelLayers.ZOMBIFIED_PIGLIN, ModelLayers.ZOMBIFIED_PIGLIN_BABY, ModelLayers.ZOMBIFIED_PIGLIN_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_BABY_ARMOR));
    }
}

