/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.Hook$HookFunction
 *  com.mojang.datafixers.types.templates.TypeTemplate
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  org.slf4j.Logger
 */
package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.slf4j.Logger;

public class V99
extends Schema {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> ITEM_TO_BLOCKENTITY = (Map)DataFixUtils.make((Object)Maps.newHashMap(), map -> {
        map.put("minecraft:furnace", "Furnace");
        map.put("minecraft:lit_furnace", "Furnace");
        map.put("minecraft:chest", "Chest");
        map.put("minecraft:trapped_chest", "Chest");
        map.put("minecraft:ender_chest", "EnderChest");
        map.put("minecraft:jukebox", "RecordPlayer");
        map.put("minecraft:dispenser", "Trap");
        map.put("minecraft:dropper", "Dropper");
        map.put("minecraft:sign", "Sign");
        map.put("minecraft:mob_spawner", "MobSpawner");
        map.put("minecraft:noteblock", "Music");
        map.put("minecraft:brewing_stand", "Cauldron");
        map.put("minecraft:enhanting_table", "EnchantTable");
        map.put("minecraft:command_block", "CommandBlock");
        map.put("minecraft:beacon", "Beacon");
        map.put("minecraft:skull", "Skull");
        map.put("minecraft:daylight_detector", "DLDetector");
        map.put("minecraft:hopper", "Hopper");
        map.put("minecraft:banner", "Banner");
        map.put("minecraft:flower_pot", "FlowerPot");
        map.put("minecraft:repeating_command_block", "CommandBlock");
        map.put("minecraft:chain_command_block", "CommandBlock");
        map.put("minecraft:standing_sign", "Sign");
        map.put("minecraft:wall_sign", "Sign");
        map.put("minecraft:piston_head", "Piston");
        map.put("minecraft:daylight_detector_inverted", "DLDetector");
        map.put("minecraft:unpowered_comparator", "Comparator");
        map.put("minecraft:powered_comparator", "Comparator");
        map.put("minecraft:wall_banner", "Banner");
        map.put("minecraft:standing_banner", "Banner");
        map.put("minecraft:structure_block", "Structure");
        map.put("minecraft:end_portal", "Airportal");
        map.put("minecraft:end_gateway", "EndGateway");
        map.put("minecraft:shield", "Banner");
    });
    public static final Map<String, String> ITEM_TO_ENTITY = Map.of("minecraft:armor_stand", "ArmorStand", "minecraft:painting", "Painting");
    protected static final Hook.HookFunction ADD_NAMES = new Hook.HookFunction(){

        public <T> T apply(DynamicOps<T> ops, T value) {
            return V99.addNames(new Dynamic(ops, value), ITEM_TO_BLOCKENTITY, ITEM_TO_ENTITY);
        }
    };

    public V99(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerThrowableProjectile(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
    }

    protected static void registerMinecart(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
    }

    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        HashMap map = Maps.newHashMap();
        schema.register((Map)map, "Item", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "XPOrb");
        V99.registerThrowableProjectile(schema, map, "ThrownEgg");
        schema.registerSimple((Map)map, "LeashKnot");
        schema.registerSimple((Map)map, "Painting");
        schema.register((Map)map, "Arrow", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        schema.register((Map)map, "TippedArrow", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        schema.register((Map)map, "SpectralArrow", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V99.registerThrowableProjectile(schema, map, "Snowball");
        V99.registerThrowableProjectile(schema, map, "Fireball");
        V99.registerThrowableProjectile(schema, map, "SmallFireball");
        V99.registerThrowableProjectile(schema, map, "ThrownEnderpearl");
        schema.registerSimple((Map)map, "EyeOfEnderSignal");
        schema.register((Map)map, "ThrownPotion", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Potion", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V99.registerThrowableProjectile(schema, map, "ThrownExpBottle");
        schema.register((Map)map, "ItemFrame", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V99.registerThrowableProjectile(schema, map, "WitherSkull");
        schema.registerSimple((Map)map, "PrimedTnt");
        schema.register((Map)map, "FallingSand", name -> DSL.optionalFields((String)"Block", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"TileEntityData", (TypeTemplate)References.BLOCK_ENTITY.in(schema)));
        schema.register((Map)map, "FireworksRocketEntity", name -> DSL.optionalFields((String)"FireworksItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "Boat");
        schema.register((Map)map, "Minecart", () -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        V99.registerMinecart(schema, map, "MinecartRideable");
        schema.register((Map)map, "MinecartChest", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        V99.registerMinecart(schema, map, "MinecartFurnace");
        V99.registerMinecart(schema, map, "MinecartTNT");
        schema.register((Map)map, "MinecartSpawner", () -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)References.UNTAGGED_SPAWNER.in(schema)));
        schema.register((Map)map, "MinecartHopper", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        schema.register((Map)map, "MinecartCommandBlock", () -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"LastOutput", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerSimple((Map)map, "ArmorStand");
        schema.registerSimple((Map)map, "Creeper");
        schema.registerSimple((Map)map, "Skeleton");
        schema.registerSimple((Map)map, "Spider");
        schema.registerSimple((Map)map, "Giant");
        schema.registerSimple((Map)map, "Zombie");
        schema.registerSimple((Map)map, "Slime");
        schema.registerSimple((Map)map, "Ghast");
        schema.registerSimple((Map)map, "PigZombie");
        schema.register((Map)map, "Enderman", name -> DSL.optionalFields((String)"carried", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        schema.registerSimple((Map)map, "CaveSpider");
        schema.registerSimple((Map)map, "Silverfish");
        schema.registerSimple((Map)map, "Blaze");
        schema.registerSimple((Map)map, "LavaSlime");
        schema.registerSimple((Map)map, "EnderDragon");
        schema.registerSimple((Map)map, "WitherBoss");
        schema.registerSimple((Map)map, "Bat");
        schema.registerSimple((Map)map, "Witch");
        schema.registerSimple((Map)map, "Endermite");
        schema.registerSimple((Map)map, "Guardian");
        schema.registerSimple((Map)map, "Pig");
        schema.registerSimple((Map)map, "Sheep");
        schema.registerSimple((Map)map, "Cow");
        schema.registerSimple((Map)map, "Chicken");
        schema.registerSimple((Map)map, "Squid");
        schema.registerSimple((Map)map, "Wolf");
        schema.registerSimple((Map)map, "MushroomCow");
        schema.registerSimple((Map)map, "SnowMan");
        schema.registerSimple((Map)map, "Ozelot");
        schema.registerSimple((Map)map, "VillagerGolem");
        schema.register((Map)map, "EntityHorse", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"ArmorItem", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "Rabbit");
        schema.register((Map)map, "Villager", name -> DSL.optionalFields((String)"Inventory", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"Offers", (TypeTemplate)DSL.optionalFields((String)"Recipes", (TypeTemplate)DSL.list((TypeTemplate)References.VILLAGER_TRADE.in(schema)))));
        schema.registerSimple((Map)map, "EnderCrystal");
        schema.register((Map)map, "AreaEffectCloud", name -> DSL.optionalFields((String)"Particle", (TypeTemplate)References.PARTICLE.in(schema)));
        schema.registerSimple((Map)map, "ShulkerBullet");
        schema.registerSimple((Map)map, "DragonFireball");
        schema.registerSimple((Map)map, "Shulker");
        return map;
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        HashMap map = Maps.newHashMap();
        V99.registerInventory(schema, map, "Furnace");
        V99.registerInventory(schema, map, "Chest");
        schema.registerSimple((Map)map, "EnderChest");
        schema.register((Map)map, "RecordPlayer", name -> DSL.optionalFields((String)"RecordItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V99.registerInventory(schema, map, "Trap");
        V99.registerInventory(schema, map, "Dropper");
        schema.register((Map)map, "Sign", () -> V99.sign(schema));
        schema.register((Map)map, "MobSpawner", name -> References.UNTAGGED_SPAWNER.in(schema));
        schema.registerSimple((Map)map, "Music");
        schema.registerSimple((Map)map, "Piston");
        V99.registerInventory(schema, map, "Cauldron");
        schema.registerSimple((Map)map, "EnchantTable");
        schema.registerSimple((Map)map, "Airportal");
        schema.register((Map)map, "Control", () -> DSL.optionalFields((String)"LastOutput", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerSimple((Map)map, "Beacon");
        schema.register((Map)map, "Skull", () -> DSL.optionalFields((String)"custom_name", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerSimple((Map)map, "DLDetector");
        V99.registerInventory(schema, map, "Hopper");
        schema.registerSimple((Map)map, "Comparator");
        schema.register((Map)map, "FlowerPot", name -> DSL.optionalFields((String)"Item", (TypeTemplate)DSL.or((TypeTemplate)DSL.constType((Type)DSL.intType()), (TypeTemplate)References.ITEM_NAME.in(schema))));
        schema.register((Map)map, "Banner", () -> DSL.optionalFields((String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerSimple((Map)map, "Structure");
        schema.registerSimple((Map)map, "EndGateway");
        return map;
    }

    public static TypeTemplate sign(Schema schema) {
        return DSL.optionalFields((Pair[])new Pair[]{Pair.of((Object)"Text1", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"Text2", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"Text3", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"Text4", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"FilteredText1", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"FilteredText2", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"FilteredText3", (Object)References.TEXT_COMPONENT.in(schema)), Pair.of((Object)"FilteredText4", (Object)References.TEXT_COMPONENT.in(schema))});
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        schema.registerType(false, References.LEVEL, () -> DSL.optionalFields((String)"CustomBossEvents", (TypeTemplate)DSL.compoundList((TypeTemplate)DSL.optionalFields((String)"Name", (TypeTemplate)References.TEXT_COMPONENT.in(schema))), (TypeTemplate)References.LIGHTWEIGHT_LEVEL.in(schema)));
        schema.registerType(false, References.LIGHTWEIGHT_LEVEL, DSL::remainder);
        schema.registerType(false, References.PLAYER, () -> DSL.optionalFields((String)"Inventory", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"EnderItems", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        schema.registerType(false, References.CHUNK, () -> DSL.fields((String)"Level", (TypeTemplate)DSL.optionalFields((String)"Entities", (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema)), (String)"TileEntities", (TypeTemplate)DSL.list((TypeTemplate)DSL.or((TypeTemplate)References.BLOCK_ENTITY.in(schema), (TypeTemplate)DSL.remainder())), (String)"TileTicks", (TypeTemplate)DSL.list((TypeTemplate)DSL.fields((String)"i", (TypeTemplate)References.BLOCK_NAME.in(schema))))));
        schema.registerType(true, References.BLOCK_ENTITY, () -> DSL.optionalFields((String)"components", (TypeTemplate)References.DATA_COMPONENTS.in(schema), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", (Type)DSL.string(), (Map)blockEntityTypes)));
        schema.registerType(true, References.ENTITY_TREE, () -> DSL.optionalFields((String)"Riding", (TypeTemplate)References.ENTITY_TREE.in(schema), (TypeTemplate)References.ENTITY.in(schema)));
        schema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
        schema.registerType(true, References.ENTITY, () -> DSL.and((TypeTemplate)References.ENTITY_EQUIPMENT.in(schema), (TypeTemplate)DSL.optionalFields((String)"CustomName", (TypeTemplate)DSL.constType((Type)DSL.string()), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", (Type)DSL.string(), (Map)entityTypes))));
        schema.registerType(true, References.ITEM_STACK, () -> DSL.hook((TypeTemplate)DSL.optionalFields((String)"id", (TypeTemplate)DSL.or((TypeTemplate)DSL.constType((Type)DSL.intType()), (TypeTemplate)References.ITEM_NAME.in(schema)), (String)"tag", (TypeTemplate)V99.itemStackTag(schema)), (Hook.HookFunction)ADD_NAMES, (Hook.HookFunction)Hook.HookFunction.IDENTITY));
        schema.registerType(false, References.OPTIONS, DSL::remainder);
        schema.registerType(false, References.BLOCK_NAME, () -> DSL.or((TypeTemplate)DSL.constType((Type)DSL.intType()), (TypeTemplate)DSL.constType(NamespacedSchema.namespacedString())));
        schema.registerType(false, References.ITEM_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
        schema.registerType(false, References.STATS, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_CUSTOM_BOSS_EVENTS, () -> DSL.optionalFields((String)"data", (TypeTemplate)DSL.compoundList((TypeTemplate)DSL.optionalFields((String)"Name", (TypeTemplate)References.TEXT_COMPONENT.in(schema)))));
        schema.registerType(false, References.SAVED_DATA_ENDER_DRAGON_FIGHT, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_GAME_RULES, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_TICKETS, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_MAP_DATA, () -> DSL.optionalFields((String)"data", (TypeTemplate)DSL.optionalFields((String)"banners", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"Name", (TypeTemplate)References.TEXT_COMPONENT.in(schema))))));
        schema.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_SCHEDULED_EVENTS, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_SCOREBOARD, () -> DSL.optionalFields((String)"data", (TypeTemplate)DSL.optionalFields((String)"Objectives", (TypeTemplate)DSL.list((TypeTemplate)References.OBJECTIVE.in(schema)), (String)"Teams", (TypeTemplate)DSL.list((TypeTemplate)References.TEAM.in(schema)), (String)"PlayerScores", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"display", (TypeTemplate)References.TEXT_COMPONENT.in(schema))))));
        schema.registerType(false, References.SAVED_DATA_STOPWATCHES, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_STRUCTURE_FEATURE_INDICES, () -> DSL.optionalFields((String)"data", (TypeTemplate)DSL.optionalFields((String)"Features", (TypeTemplate)DSL.compoundList((TypeTemplate)References.STRUCTURE_FEATURE.in(schema)))));
        schema.registerType(false, References.SAVED_DATA_WANDERING_TRADER, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_WEATHER, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_WORLD_BORDER, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_WORLD_CLOCKS, DSL::remainder);
        schema.registerType(false, References.SAVED_DATA_WORLD_GEN_SETTINGS, () -> DSL.fields((String)"data", (TypeTemplate)References.WORLD_GEN_SETTINGS.in(schema)));
        schema.registerType(false, References.DEBUG_PROFILE, DSL::remainder);
        schema.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
        schema.registerType(false, References.OBJECTIVE, DSL::remainder);
        schema.registerType(false, References.TEAM, () -> DSL.optionalFields((String)"MemberNamePrefix", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"MemberNameSuffix", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"DisplayName", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerType(true, References.UNTAGGED_SPAWNER, DSL::remainder);
        schema.registerType(false, References.POI_CHUNK, DSL::remainder);
        schema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
        schema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields((String)"Entities", (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema))));
        schema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
        schema.registerType(true, References.VILLAGER_TRADE, () -> DSL.optionalFields((String)"buy", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"buyB", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"sell", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerType(true, References.PARTICLE, () -> DSL.constType((Type)DSL.string()));
        schema.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType((Type)DSL.string()));
        schema.registerType(false, References.STRUCTURE, () -> DSL.optionalFields((String)"entities", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"nbt", (TypeTemplate)References.ENTITY_TREE.in(schema))), (String)"blocks", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"nbt", (TypeTemplate)References.BLOCK_ENTITY.in(schema))), (String)"palette", (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_STATE.in(schema))));
        schema.registerType(false, References.BLOCK_STATE, DSL::remainder);
        schema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
        schema.registerType(true, References.ENTITY_EQUIPMENT, () -> DSL.optional((TypeTemplate)DSL.field((String)"Equipment", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)))));
    }

    public static TypeTemplate itemStackTag(Schema schema) {
        return DSL.optionalFields((Pair[])new Pair[]{Pair.of((Object)"EntityTag", (Object)References.ENTITY_TREE.in(schema)), Pair.of((Object)"BlockEntityTag", (Object)References.BLOCK_ENTITY.in(schema)), Pair.of((Object)"CanDestroy", (Object)DSL.list((TypeTemplate)References.BLOCK_NAME.in(schema))), Pair.of((Object)"CanPlaceOn", (Object)DSL.list((TypeTemplate)References.BLOCK_NAME.in(schema))), Pair.of((Object)"Items", (Object)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))), Pair.of((Object)"ChargedProjectiles", (Object)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))), Pair.of((Object)"pages", (Object)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema))), Pair.of((Object)"filtered_pages", (Object)DSL.compoundList((TypeTemplate)References.TEXT_COMPONENT.in(schema))), Pair.of((Object)"display", (Object)DSL.optionalFields((String)"Name", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"Lore", (TypeTemplate)DSL.list((TypeTemplate)References.TEXT_COMPONENT.in(schema))))});
    }

    protected static <T> T addNames(Dynamic<T> input, Map<String, String> itemToBlockEntityMap, Map<String, String> itemToEntityMap) {
        return (T)input.update("tag", itemStackTag -> itemStackTag.update("BlockEntityTag", blockEntity -> {
            String itemId = input.get("id").asString().result().map(NamespacedSchema::ensureNamespaced).orElse("minecraft:air");
            if (!"minecraft:air".equals(itemId)) {
                String expectedId = (String)itemToBlockEntityMap.get(itemId);
                if (expectedId == null) {
                    LOGGER.warn("Unable to resolve BlockEntity for ItemStack: {}", (Object)itemId);
                } else {
                    return blockEntity.set("id", input.createString(expectedId));
                }
            }
            return blockEntity;
        }).update("EntityTag", entity -> {
            if (entity.get("id").result().isPresent()) {
                return entity;
            }
            String itemId = NamespacedSchema.ensureNamespaced(input.get("id").asString(""));
            String expectedId = (String)itemToEntityMap.get(itemId);
            if (expectedId != null) {
                return entity.set("id", input.createString(expectedId));
            }
            return entity;
        })).getValue();
    }
}

