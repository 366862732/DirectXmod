/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.Hook$HookFunction
 *  com.mojang.datafixers.types.templates.TypeTemplate
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V1451_6;
import net.minecraft.util.datafix.schemas.V1458;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V99;

public class V1460
extends NamespacedSchema {
    public V1460(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.registerSimple(map, name);
    }

    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> V1458.nameableInventory(schema));
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        HashMap map = Maps.newHashMap();
        schema.register((Map)map, "minecraft:area_effect_cloud", name -> DSL.optionalFields((String)"Particle", (TypeTemplate)References.PARTICLE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:armor_stand");
        schema.register((Map)map, "minecraft:arrow", name -> DSL.optionalFields((String)"inBlockState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:bat");
        V1460.registerMob(schema, map, "minecraft:blaze");
        schema.registerSimple((Map)map, "minecraft:boat");
        V1460.registerMob(schema, map, "minecraft:cave_spider");
        schema.register((Map)map, "minecraft:chest_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        V1460.registerMob(schema, map, "minecraft:chicken");
        schema.register((Map)map, "minecraft:commandblock_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema), (String)"LastOutput", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        V1460.registerMob(schema, map, "minecraft:cow");
        V1460.registerMob(schema, map, "minecraft:creeper");
        schema.register((Map)map, "minecraft:donkey", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:dragon_fireball");
        schema.registerSimple((Map)map, "minecraft:egg");
        V1460.registerMob(schema, map, "minecraft:elder_guardian");
        schema.registerSimple((Map)map, "minecraft:ender_crystal");
        V1460.registerMob(schema, map, "minecraft:ender_dragon");
        schema.register((Map)map, "minecraft:enderman", name -> DSL.optionalFields((String)"carriedBlockState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:endermite");
        schema.registerSimple((Map)map, "minecraft:ender_pearl");
        schema.registerSimple((Map)map, "minecraft:evocation_fangs");
        V1460.registerMob(schema, map, "minecraft:evocation_illager");
        schema.registerSimple((Map)map, "minecraft:eye_of_ender_signal");
        schema.register((Map)map, "minecraft:falling_block", name -> DSL.optionalFields((String)"BlockState", (TypeTemplate)References.BLOCK_STATE.in(schema), (String)"TileEntityData", (TypeTemplate)References.BLOCK_ENTITY.in(schema)));
        schema.registerSimple((Map)map, "minecraft:fireball");
        schema.register((Map)map, "minecraft:fireworks_rocket", name -> DSL.optionalFields((String)"FireworksItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register((Map)map, "minecraft:furnace_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:ghast");
        V1460.registerMob(schema, map, "minecraft:giant");
        V1460.registerMob(schema, map, "minecraft:guardian");
        schema.register((Map)map, "minecraft:hopper_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        schema.register((Map)map, "minecraft:horse", name -> DSL.optionalFields((String)"ArmorItem", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerMob(schema, map, "minecraft:husk");
        V1460.registerMob(schema, map, "minecraft:illusion_illager");
        schema.register((Map)map, "minecraft:item", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register((Map)map, "minecraft:item_frame", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:leash_knot");
        schema.register((Map)map, "minecraft:llama", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"DecorItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:llama_spit");
        V1460.registerMob(schema, map, "minecraft:magma_cube");
        schema.register((Map)map, "minecraft:minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:mooshroom");
        schema.register((Map)map, "minecraft:mule", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerMob(schema, map, "minecraft:ocelot");
        schema.registerSimple((Map)map, "minecraft:painting");
        V1460.registerMob(schema, map, "minecraft:parrot");
        V1460.registerMob(schema, map, "minecraft:pig");
        V1460.registerMob(schema, map, "minecraft:polar_bear");
        schema.register((Map)map, "minecraft:potion", name -> DSL.optionalFields((String)"Potion", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerMob(schema, map, "minecraft:rabbit");
        V1460.registerMob(schema, map, "minecraft:sheep");
        V1460.registerMob(schema, map, "minecraft:shulker");
        schema.registerSimple((Map)map, "minecraft:shulker_bullet");
        V1460.registerMob(schema, map, "minecraft:silverfish");
        V1460.registerMob(schema, map, "minecraft:skeleton");
        schema.register((Map)map, "minecraft:skeleton_horse", name -> DSL.optionalFields((String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerMob(schema, map, "minecraft:slime");
        schema.registerSimple((Map)map, "minecraft:small_fireball");
        schema.registerSimple((Map)map, "minecraft:snowball");
        V1460.registerMob(schema, map, "minecraft:snowman");
        schema.register((Map)map, "minecraft:spawner_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema), (TypeTemplate)References.UNTAGGED_SPAWNER.in(schema)));
        schema.register((Map)map, "minecraft:spectral_arrow", name -> DSL.optionalFields((String)"inBlockState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:spider");
        V1460.registerMob(schema, map, "minecraft:squid");
        V1460.registerMob(schema, map, "minecraft:stray");
        schema.registerSimple((Map)map, "minecraft:tnt");
        schema.register((Map)map, "minecraft:tnt_minecart", name -> DSL.optionalFields((String)"DisplayState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerMob(schema, map, "minecraft:vex");
        schema.register((Map)map, "minecraft:villager", name -> DSL.optionalFields((String)"Inventory", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"Offers", (TypeTemplate)DSL.optionalFields((String)"Recipes", (TypeTemplate)DSL.list((TypeTemplate)References.VILLAGER_TRADE.in(schema)))));
        V1460.registerMob(schema, map, "minecraft:villager_golem");
        V1460.registerMob(schema, map, "minecraft:vindication_illager");
        V1460.registerMob(schema, map, "minecraft:witch");
        V1460.registerMob(schema, map, "minecraft:wither");
        V1460.registerMob(schema, map, "minecraft:wither_skeleton");
        schema.registerSimple((Map)map, "minecraft:wither_skull");
        V1460.registerMob(schema, map, "minecraft:wolf");
        schema.registerSimple((Map)map, "minecraft:xp_bottle");
        schema.registerSimple((Map)map, "minecraft:xp_orb");
        V1460.registerMob(schema, map, "minecraft:zombie");
        schema.register((Map)map, "minecraft:zombie_horse", name -> DSL.optionalFields((String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerMob(schema, map, "minecraft:zombie_pigman");
        schema.register((Map)map, "minecraft:zombie_villager", name -> DSL.optionalFields((String)"Offers", (TypeTemplate)DSL.optionalFields((String)"Recipes", (TypeTemplate)DSL.list((TypeTemplate)References.VILLAGER_TRADE.in(schema)))));
        return map;
    }

    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        HashMap map = Maps.newHashMap();
        V1460.registerInventory(schema, map, "minecraft:furnace");
        V1460.registerInventory(schema, map, "minecraft:chest");
        V1460.registerInventory(schema, map, "minecraft:trapped_chest");
        schema.registerSimple((Map)map, "minecraft:ender_chest");
        schema.register((Map)map, "minecraft:jukebox", name -> DSL.optionalFields((String)"RecordItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V1460.registerInventory(schema, map, "minecraft:dispenser");
        V1460.registerInventory(schema, map, "minecraft:dropper");
        schema.register((Map)map, "minecraft:sign", () -> V99.sign(schema));
        schema.register((Map)map, "minecraft:mob_spawner", name -> References.UNTAGGED_SPAWNER.in(schema));
        schema.register((Map)map, "minecraft:piston", name -> DSL.optionalFields((String)"blockState", (TypeTemplate)References.BLOCK_STATE.in(schema)));
        V1460.registerInventory(schema, map, "minecraft:brewing_stand");
        schema.register((Map)map, "minecraft:enchanting_table", () -> V1458.nameable(schema));
        schema.registerSimple((Map)map, "minecraft:end_portal");
        schema.register((Map)map, "minecraft:beacon", () -> V1458.nameable(schema));
        schema.register((Map)map, "minecraft:skull", () -> DSL.optionalFields((String)"custom_name", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerSimple((Map)map, "minecraft:daylight_detector");
        V1460.registerInventory(schema, map, "minecraft:hopper");
        schema.registerSimple((Map)map, "minecraft:comparator");
        schema.register((Map)map, "minecraft:banner", () -> V1458.nameable(schema));
        schema.registerSimple((Map)map, "minecraft:structure_block");
        schema.registerSimple((Map)map, "minecraft:end_gateway");
        schema.register((Map)map, "minecraft:command_block", () -> DSL.optionalFields((String)"LastOutput", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        V1460.registerInventory(schema, map, "minecraft:shulker_box");
        schema.registerSimple((Map)map, "minecraft:bed");
        return map;
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        schema.registerType(false, References.LEVEL, () -> DSL.optionalFields((String)"CustomBossEvents", (TypeTemplate)DSL.compoundList((TypeTemplate)DSL.optionalFields((String)"Name", (TypeTemplate)References.TEXT_COMPONENT.in(schema))), (TypeTemplate)References.LIGHTWEIGHT_LEVEL.in(schema)));
        schema.registerType(false, References.LIGHTWEIGHT_LEVEL, DSL::remainder);
        schema.registerType(false, References.RECIPE, () -> DSL.constType(V1460.namespacedString()));
        schema.registerType(false, References.PLAYER, () -> DSL.optionalFields((Pair[])new Pair[]{Pair.of((Object)"RootVehicle", (Object)DSL.optionalFields((String)"Entity", (TypeTemplate)References.ENTITY_TREE.in(schema))), Pair.of((Object)"ender_pearls", (Object)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema))), Pair.of((Object)"Inventory", (Object)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))), Pair.of((Object)"EnderItems", (Object)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))), Pair.of((Object)"ShoulderEntityLeft", (Object)References.ENTITY_TREE.in(schema)), Pair.of((Object)"ShoulderEntityRight", (Object)References.ENTITY_TREE.in(schema)), Pair.of((Object)"recipeBook", (Object)DSL.optionalFields((String)"recipes", (TypeTemplate)DSL.list((TypeTemplate)References.RECIPE.in(schema)), (String)"toBeDisplayed", (TypeTemplate)DSL.list((TypeTemplate)References.RECIPE.in(schema))))}));
        schema.registerType(false, References.CHUNK, () -> DSL.fields((String)"Level", (TypeTemplate)DSL.optionalFields((String)"Entities", (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema)), (String)"TileEntities", (TypeTemplate)DSL.list((TypeTemplate)DSL.or((TypeTemplate)References.BLOCK_ENTITY.in(schema), (TypeTemplate)DSL.remainder())), (String)"TileTicks", (TypeTemplate)DSL.list((TypeTemplate)DSL.fields((String)"i", (TypeTemplate)References.BLOCK_NAME.in(schema))), (String)"Sections", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"Palette", (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_STATE.in(schema)))))));
        schema.registerType(true, References.BLOCK_ENTITY, () -> DSL.optionalFields((String)"components", (TypeTemplate)References.DATA_COMPONENTS.in(schema), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", V1460.namespacedString(), (Map)blockEntityTypes)));
        schema.registerType(true, References.ENTITY_TREE, () -> DSL.optionalFields((String)"Passengers", (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema)), (TypeTemplate)References.ENTITY.in(schema)));
        schema.registerType(true, References.ENTITY, () -> DSL.and((TypeTemplate)References.ENTITY_EQUIPMENT.in(schema), (TypeTemplate)DSL.optionalFields((String)"CustomName", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", V1460.namespacedString(), (Map)entityTypes))));
        schema.registerType(true, References.ITEM_STACK, () -> DSL.hook((TypeTemplate)DSL.optionalFields((String)"id", (TypeTemplate)References.ITEM_NAME.in(schema), (String)"tag", (TypeTemplate)V99.itemStackTag(schema)), (Hook.HookFunction)V705.ADD_NAMES, (Hook.HookFunction)Hook.HookFunction.IDENTITY));
        schema.registerType(false, References.HOTBAR, () -> DSL.compoundList((TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        schema.registerType(false, References.OPTIONS, DSL::remainder);
        schema.registerType(false, References.STRUCTURE, () -> DSL.optionalFields((String)"entities", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"nbt", (TypeTemplate)References.ENTITY_TREE.in(schema))), (String)"blocks", (TypeTemplate)DSL.list((TypeTemplate)DSL.optionalFields((String)"nbt", (TypeTemplate)References.BLOCK_ENTITY.in(schema))), (String)"palette", (TypeTemplate)DSL.list((TypeTemplate)References.BLOCK_STATE.in(schema))));
        schema.registerType(false, References.BLOCK_NAME, () -> DSL.constType(V1460.namespacedString()));
        schema.registerType(false, References.ITEM_NAME, () -> DSL.constType(V1460.namespacedString()));
        schema.registerType(false, References.BLOCK_STATE, DSL::remainder);
        schema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
        Supplier<TypeTemplate> itemStats = () -> DSL.compoundList((TypeTemplate)References.ITEM_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()));
        schema.registerType(false, References.STATS, () -> DSL.optionalFields((String)"stats", (TypeTemplate)DSL.optionalFields((Pair[])new Pair[]{Pair.of((Object)"minecraft:mined", (Object)DSL.compoundList((TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:crafted", (Object)((TypeTemplate)itemStats.get())), Pair.of((Object)"minecraft:used", (Object)((TypeTemplate)itemStats.get())), Pair.of((Object)"minecraft:broken", (Object)((TypeTemplate)itemStats.get())), Pair.of((Object)"minecraft:picked_up", (Object)((TypeTemplate)itemStats.get())), Pair.of((Object)"minecraft:dropped", (Object)((TypeTemplate)itemStats.get())), Pair.of((Object)"minecraft:killed", (Object)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:killed_by", (Object)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:custom", (Object)DSL.compoundList((TypeTemplate)DSL.constType(V1460.namespacedString()), (TypeTemplate)DSL.constType((Type)DSL.intType())))})));
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
        Map<String, Supplier<TypeTemplate>> criterionTypes = V1451_6.createCriterionTypes(schema);
        schema.registerType(false, References.OBJECTIVE, () -> DSL.hook((TypeTemplate)DSL.optionalFields((String)"CriteriaType", (TypeTemplate)DSL.taggedChoiceLazy((String)"type", (Type)DSL.string(), (Map)criterionTypes), (String)"DisplayName", (TypeTemplate)References.TEXT_COMPONENT.in(schema)), (Hook.HookFunction)V1451_6.UNPACK_OBJECTIVE_ID, (Hook.HookFunction)V1451_6.REPACK_OBJECTIVE_ID));
        schema.registerType(false, References.TEAM, () -> DSL.optionalFields((String)"MemberNamePrefix", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"MemberNameSuffix", (TypeTemplate)References.TEXT_COMPONENT.in(schema), (String)"DisplayName", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        schema.registerType(true, References.UNTAGGED_SPAWNER, () -> DSL.optionalFields((String)"SpawnPotentials", (TypeTemplate)DSL.list((TypeTemplate)DSL.fields((String)"Entity", (TypeTemplate)References.ENTITY_TREE.in(schema))), (String)"SpawnData", (TypeTemplate)References.ENTITY_TREE.in(schema)));
        schema.registerType(false, References.ADVANCEMENTS, () -> DSL.optionalFields((String)"minecraft:adventure/adventuring_time", (TypeTemplate)DSL.optionalFields((String)"criteria", (TypeTemplate)DSL.compoundList((TypeTemplate)References.BIOME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.string()))), (String)"minecraft:adventure/kill_a_mob", (TypeTemplate)DSL.optionalFields((String)"criteria", (TypeTemplate)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.string()))), (String)"minecraft:adventure/kill_all_mobs", (TypeTemplate)DSL.optionalFields((String)"criteria", (TypeTemplate)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.string()))), (String)"minecraft:husbandry/bred_all_animals", (TypeTemplate)DSL.optionalFields((String)"criteria", (TypeTemplate)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.string())))));
        schema.registerType(false, References.BIOME, () -> DSL.constType(V1460.namespacedString()));
        schema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(V1460.namespacedString()));
        schema.registerType(false, References.POI_CHUNK, DSL::remainder);
        schema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
        schema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields((String)"Entities", (TypeTemplate)DSL.list((TypeTemplate)References.ENTITY_TREE.in(schema))));
        schema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
        schema.registerType(true, References.VILLAGER_TRADE, () -> DSL.optionalFields((String)"buy", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"buyB", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"sell", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerType(true, References.PARTICLE, () -> DSL.constType((Type)DSL.string()));
        schema.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType((Type)DSL.string()));
        schema.registerType(true, References.ENTITY_EQUIPMENT, () -> DSL.and((TypeTemplate)DSL.optional((TypeTemplate)DSL.field((String)"ArmorItems", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)))), (TypeTemplate[])new TypeTemplate[]{DSL.optional((TypeTemplate)DSL.field((String)"HandItems", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)))), DSL.optional((TypeTemplate)DSL.field((String)"body_armor_item", (TypeTemplate)References.ITEM_STACK.in(schema))), DSL.optional((TypeTemplate)DSL.field((String)"saddle", (TypeTemplate)References.ITEM_STACK.in(schema)))}));
    }
}

