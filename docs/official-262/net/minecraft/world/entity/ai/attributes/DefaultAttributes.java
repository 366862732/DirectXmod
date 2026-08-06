/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class DefaultAttributes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> SUPPLIERS = ImmutableMap.builder().put(EntityTypes.ALLAY, (Object)Allay.createAttributes().build()).put(EntityTypes.ARMADILLO, (Object)Armadillo.createAttributes().build()).put(EntityTypes.ARMOR_STAND, (Object)ArmorStand.createAttributes().build()).put(EntityTypes.AXOLOTL, (Object)Axolotl.createAttributes().build()).put(EntityTypes.BAT, (Object)Bat.createAttributes().build()).put(EntityTypes.BEE, (Object)Bee.createAttributes().build()).put(EntityTypes.BLAZE, (Object)Blaze.createAttributes().build()).put(EntityTypes.BOGGED, (Object)Bogged.createAttributes().build()).put(EntityTypes.CAT, (Object)Cat.createAttributes().build()).put(EntityTypes.CAMEL, (Object)Camel.createAttributes().build()).put(EntityTypes.CAMEL_HUSK, (Object)Camel.createAttributes().build()).put(EntityTypes.CAVE_SPIDER, (Object)CaveSpider.createCaveSpider().build()).put(EntityTypes.CHICKEN, (Object)Chicken.createAttributes().build()).put(EntityTypes.COD, (Object)AbstractFish.createAttributes().build()).put(EntityTypes.COPPER_GOLEM, (Object)CopperGolem.createAttributes().build()).put(EntityTypes.COW, (Object)Cow.createAttributes().build()).put(EntityTypes.CREAKING, (Object)Creaking.createAttributes().build()).put(EntityTypes.CREEPER, (Object)Creeper.createAttributes().build()).put(EntityTypes.DOLPHIN, (Object)Dolphin.createAttributes().build()).put(EntityTypes.DONKEY, (Object)AbstractChestedHorse.createBaseChestedHorseAttributes().build()).put(EntityTypes.DROWNED, (Object)Drowned.createAttributes().build()).put(EntityTypes.ELDER_GUARDIAN, (Object)ElderGuardian.createAttributes().build()).put(EntityTypes.ENDERMAN, (Object)EnderMan.createAttributes().build()).put(EntityTypes.ENDERMITE, (Object)Endermite.createAttributes().build()).put(EntityTypes.ENDER_DRAGON, (Object)EnderDragon.createAttributes().build()).put(EntityTypes.EVOKER, (Object)Evoker.createAttributes().build()).put(EntityTypes.BREEZE, (Object)Breeze.createAttributes().build()).put(EntityTypes.FOX, (Object)Fox.createAttributes().build()).put(EntityTypes.FROG, (Object)Frog.createAttributes().build()).put(EntityTypes.GHAST, (Object)Ghast.createAttributes().build()).put(EntityTypes.HAPPY_GHAST, (Object)HappyGhast.createAttributes().build()).put(EntityTypes.GIANT, (Object)Giant.createAttributes().build()).put(EntityTypes.GLOW_SQUID, (Object)GlowSquid.createAttributes().build()).put(EntityTypes.GOAT, (Object)Goat.createAttributes().build()).put(EntityTypes.GUARDIAN, (Object)Guardian.createAttributes().build()).put(EntityTypes.HOGLIN, (Object)Hoglin.createAttributes().build()).put(EntityTypes.HORSE, (Object)AbstractHorse.createBaseHorseAttributes().build()).put(EntityTypes.HUSK, (Object)Zombie.createAttributes().build()).put(EntityTypes.ILLUSIONER, (Object)Illusioner.createAttributes().build()).put(EntityTypes.IRON_GOLEM, (Object)IronGolem.createAttributes().build()).put(EntityTypes.LLAMA, (Object)Llama.createAttributes().build()).put(EntityTypes.MAGMA_CUBE, (Object)MagmaCube.createAttributes().build()).put(EntityTypes.SULFUR_CUBE, (Object)SulfurCube.createSulfurCubeAttributes().build()).put(EntityTypes.MANNEQUIN, (Object)LivingEntity.createLivingAttributes().build()).put(EntityTypes.MOOSHROOM, (Object)Cow.createAttributes().build()).put(EntityTypes.MULE, (Object)AbstractChestedHorse.createBaseChestedHorseAttributes().build()).put(EntityTypes.NAUTILUS, (Object)Nautilus.createAttributes().build()).put(EntityTypes.OCELOT, (Object)Ocelot.createAttributes().build()).put(EntityTypes.PANDA, (Object)Panda.createAttributes().build()).put(EntityTypes.PARCHED, (Object)Parched.createAttributes().build()).put(EntityTypes.PARROT, (Object)Parrot.createAttributes().build()).put(EntityTypes.PHANTOM, (Object)Monster.createMonsterAttributes().build()).put(EntityTypes.PIG, (Object)Pig.createAttributes().build()).put(EntityTypes.PIGLIN, (Object)Piglin.createAttributes().build()).put(EntityTypes.PIGLIN_BRUTE, (Object)PiglinBrute.createAttributes().build()).put(EntityTypes.PILLAGER, (Object)Pillager.createAttributes().build()).put(EntityTypes.PLAYER, (Object)Player.createAttributes().build()).put(EntityTypes.POLAR_BEAR, (Object)PolarBear.createAttributes().build()).put(EntityTypes.PUFFERFISH, (Object)AbstractFish.createAttributes().build()).put(EntityTypes.RABBIT, (Object)Rabbit.createAttributes().build()).put(EntityTypes.RAVAGER, (Object)Ravager.createAttributes().build()).put(EntityTypes.SALMON, (Object)AbstractFish.createAttributes().build()).put(EntityTypes.SHEEP, (Object)Sheep.createAttributes().build()).put(EntityTypes.SHULKER, (Object)Shulker.createAttributes().build()).put(EntityTypes.SILVERFISH, (Object)Silverfish.createAttributes().build()).put(EntityTypes.SKELETON, (Object)AbstractSkeleton.createAttributes().build()).put(EntityTypes.SKELETON_HORSE, (Object)SkeletonHorse.createAttributes().build()).put(EntityTypes.SLIME, (Object)Monster.createMonsterAttributes().build()).put(EntityTypes.SNIFFER, (Object)Sniffer.createAttributes().build()).put(EntityTypes.SNOW_GOLEM, (Object)SnowGolem.createAttributes().build()).put(EntityTypes.SPIDER, (Object)Spider.createAttributes().build()).put(EntityTypes.SQUID, (Object)Squid.createAttributes().build()).put(EntityTypes.STRAY, (Object)AbstractSkeleton.createAttributes().build()).put(EntityTypes.STRIDER, (Object)Strider.createAttributes().build()).put(EntityTypes.TADPOLE, (Object)Tadpole.createAttributes().build()).put(EntityTypes.TRADER_LLAMA, (Object)Llama.createAttributes().build()).put(EntityTypes.TROPICAL_FISH, (Object)AbstractFish.createAttributes().build()).put(EntityTypes.TURTLE, (Object)Turtle.createAttributes().build()).put(EntityTypes.VEX, (Object)Vex.createAttributes().build()).put(EntityTypes.VILLAGER, (Object)Villager.createAttributes().build()).put(EntityTypes.VINDICATOR, (Object)Vindicator.createAttributes().build()).put(EntityTypes.WARDEN, (Object)Warden.createAttributes().build()).put(EntityTypes.WANDERING_TRADER, (Object)Mob.createMobAttributes().build()).put(EntityTypes.WITCH, (Object)Witch.createAttributes().build()).put(EntityTypes.WITHER, (Object)WitherBoss.createAttributes().build()).put(EntityTypes.WITHER_SKELETON, (Object)AbstractSkeleton.createAttributes().build()).put(EntityTypes.WOLF, (Object)Wolf.createAttributes().build()).put(EntityTypes.ZOGLIN, (Object)Zoglin.createAttributes().build()).put(EntityTypes.ZOMBIE, (Object)Zombie.createAttributes().build()).put(EntityTypes.ZOMBIE_HORSE, (Object)ZombieHorse.createAttributes().build()).put(EntityTypes.ZOMBIE_NAUTILUS, (Object)ZombieNautilus.createAttributes().build()).put(EntityTypes.ZOMBIE_VILLAGER, (Object)Zombie.createAttributes().build()).put(EntityTypes.ZOMBIFIED_PIGLIN, (Object)ZombifiedPiglin.createAttributes().build()).build();

    public static AttributeSupplier getSupplier(EntityType<? extends LivingEntity> type) {
        return SUPPLIERS.get(type);
    }

    public static boolean hasSupplier(EntityType<?> type) {
        return SUPPLIERS.containsKey(type);
    }

    public static void validate() {
        BuiltInRegistries.ENTITY_TYPE.stream().filter(entityType -> entityType.getCategory() != MobCategory.MISC).filter(entityType -> !DefaultAttributes.hasSupplier(entityType)).map(BuiltInRegistries.ENTITY_TYPE::getKey).forEach(id -> Util.logAndPauseIfInIde("Entity " + String.valueOf(id) + " has no attributes"));
    }
}

