/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.Hook$HookFunction
 *  com.mojang.datafixers.types.templates.TypeTemplate
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V99;

public class V705
extends NamespacedSchema {
    private static final Map<String, String> ITEM_TO_ENTITY = ImmutableMap.builder().put((Object)"minecraft:armor_stand", (Object)"minecraft:armor_stand").put((Object)"minecraft:painting", (Object)"minecraft:painting").put((Object)"minecraft:armadillo_spawn_egg", (Object)"minecraft:armadillo").put((Object)"minecraft:allay_spawn_egg", (Object)"minecraft:allay").put((Object)"minecraft:axolotl_spawn_egg", (Object)"minecraft:axolotl").put((Object)"minecraft:bat_spawn_egg", (Object)"minecraft:bat").put((Object)"minecraft:bee_spawn_egg", (Object)"minecraft:bee").put((Object)"minecraft:blaze_spawn_egg", (Object)"minecraft:blaze").put((Object)"minecraft:bogged_spawn_egg", (Object)"minecraft:bogged").put((Object)"minecraft:breeze_spawn_egg", (Object)"minecraft:breeze").put((Object)"minecraft:cat_spawn_egg", (Object)"minecraft:cat").put((Object)"minecraft:camel_spawn_egg", (Object)"minecraft:camel").put((Object)"minecraft:cave_spider_spawn_egg", (Object)"minecraft:cave_spider").put((Object)"minecraft:chicken_spawn_egg", (Object)"minecraft:chicken").put((Object)"minecraft:cod_spawn_egg", (Object)"minecraft:cod").put((Object)"minecraft:cow_spawn_egg", (Object)"minecraft:cow").put((Object)"minecraft:creeper_spawn_egg", (Object)"minecraft:creeper").put((Object)"minecraft:dolphin_spawn_egg", (Object)"minecraft:dolphin").put((Object)"minecraft:donkey_spawn_egg", (Object)"minecraft:donkey").put((Object)"minecraft:drowned_spawn_egg", (Object)"minecraft:drowned").put((Object)"minecraft:elder_guardian_spawn_egg", (Object)"minecraft:elder_guardian").put((Object)"minecraft:ender_dragon_spawn_egg", (Object)"minecraft:ender_dragon").put((Object)"minecraft:enderman_spawn_egg", (Object)"minecraft:enderman").put((Object)"minecraft:endermite_spawn_egg", (Object)"minecraft:endermite").put((Object)"minecraft:evoker_spawn_egg", (Object)"minecraft:evoker").put((Object)"minecraft:fox_spawn_egg", (Object)"minecraft:fox").put((Object)"minecraft:frog_spawn_egg", (Object)"minecraft:frog").put((Object)"minecraft:ghast_spawn_egg", (Object)"minecraft:ghast").put((Object)"minecraft:glow_squid_spawn_egg", (Object)"minecraft:glow_squid").put((Object)"minecraft:goat_spawn_egg", (Object)"minecraft:goat").put((Object)"minecraft:guardian_spawn_egg", (Object)"minecraft:guardian").put((Object)"minecraft:hoglin_spawn_egg", (Object)"minecraft:hoglin").put((Object)"minecraft:horse_spawn_egg", (Object)"minecraft:horse").put((Object)"minecraft:husk_spawn_egg", (Object)"minecraft:husk").put((Object)"minecraft:iron_golem_spawn_egg", (Object)"minecraft:iron_golem").put((Object)"minecraft:llama_spawn_egg", (Object)"minecraft:llama").put((Object)"minecraft:magma_cube_spawn_egg", (Object)"minecraft:magma_cube").put((Object)"minecraft:mooshroom_spawn_egg", (Object)"minecraft:mooshroom").put((Object)"minecraft:mule_spawn_egg", (Object)"minecraft:mule").put((Object)"minecraft:ocelot_spawn_egg", (Object)"minecraft:ocelot").put((Object)"minecraft:panda_spawn_egg", (Object)"minecraft:panda").put((Object)"minecraft:parrot_spawn_egg", (Object)"minecraft:parrot").put((Object)"minecraft:phantom_spawn_egg", (Object)"minecraft:phantom").put((Object)"minecraft:pig_spawn_egg", (Object)"minecraft:pig").put((Object)"minecraft:piglin_spawn_egg", (Object)"minecraft:piglin").put((Object)"minecraft:piglin_brute_spawn_egg", (Object)"minecraft:piglin_brute").put((Object)"minecraft:pillager_spawn_egg", (Object)"minecraft:pillager").put((Object)"minecraft:polar_bear_spawn_egg", (Object)"minecraft:polar_bear").put((Object)"minecraft:pufferfish_spawn_egg", (Object)"minecraft:pufferfish").put((Object)"minecraft:rabbit_spawn_egg", (Object)"minecraft:rabbit").put((Object)"minecraft:ravager_spawn_egg", (Object)"minecraft:ravager").put((Object)"minecraft:salmon_spawn_egg", (Object)"minecraft:salmon").put((Object)"minecraft:sheep_spawn_egg", (Object)"minecraft:sheep").put((Object)"minecraft:shulker_spawn_egg", (Object)"minecraft:shulker").put((Object)"minecraft:silverfish_spawn_egg", (Object)"minecraft:silverfish").put((Object)"minecraft:skeleton_spawn_egg", (Object)"minecraft:skeleton").put((Object)"minecraft:skeleton_horse_spawn_egg", (Object)"minecraft:skeleton_horse").put((Object)"minecraft:slime_spawn_egg", (Object)"minecraft:slime").put((Object)"minecraft:sniffer_spawn_egg", (Object)"minecraft:sniffer").put((Object)"minecraft:snow_golem_spawn_egg", (Object)"minecraft:snow_golem").put((Object)"minecraft:spider_spawn_egg", (Object)"minecraft:spider").put((Object)"minecraft:squid_spawn_egg", (Object)"minecraft:squid").put((Object)"minecraft:stray_spawn_egg", (Object)"minecraft:stray").put((Object)"minecraft:strider_spawn_egg", (Object)"minecraft:strider").put((Object)"minecraft:tadpole_spawn_egg", (Object)"minecraft:tadpole").put((Object)"minecraft:trader_llama_spawn_egg", (Object)"minecraft:trader_llama").put((Object)"minecraft:tropical_fish_spawn_egg", (Object)"minecraft:tropical_fish").put((Object)"minecraft:turtle_spawn_egg", (Object)"minecraft:turtle").put((Object)"minecraft:vex_spawn_egg", (Object)"minecraft:vex").put((Object)"minecraft:villager_spawn_egg", (Object)"minecraft:villager").put((Object)"minecraft:vindicator_spawn_egg", (Object)"minecraft:vindicator").put((Object)"minecraft:wandering_trader_spawn_egg", (Object)"minecraft:wandering_trader").put((Object)"minecraft:warden_spawn_egg", (Object)"minecraft:warden").put((Object)"minecraft:witch_spawn_egg", (Object)"minecraft:witch").put((Object)"minecraft:wither_spawn_egg", (Object)"minecraft:wither").put((Object)"minecraft:wither_skeleton_spawn_egg", (Object)"minecraft:wither_skeleton").put((Object)"minecraft:wolf_spawn_egg", (Object)"minecraft:wolf").put((Object)"minecraft:zoglin_spawn_egg", (Object)"minecraft:zoglin").put((Object)"minecraft:zombie_spawn_egg", (Object)"minecraft:zombie").put((Object)"minecraft:zombie_horse_spawn_egg", (Object)"minecraft:zombie_horse").put((Object)"minecraft:zombie_villager_spawn_egg", (Object)"minecraft:zombie_villager").put((Object)"minecraft:zombified_piglin_spawn_egg", (Object)"minecraft:zombified_piglin").put((Object)"minecraft:item_frame", (Object)"minecraft:item_frame").put((Object)"minecraft:boat", (Object)"minecraft:oak_boat").put((Object)"minecraft:oak_boat", (Object)"minecraft:oak_boat").put((Object)"minecraft:oak_chest_boat", (Object)"minecraft:oak_chest_boat").put((Object)"minecraft:spruce_boat", (Object)"minecraft:spruce_boat").put((Object)"minecraft:spruce_chest_boat", (Object)"minecraft:spruce_chest_boat").put((Object)"minecraft:birch_boat", (Object)"minecraft:birch_boat").put((Object)"minecraft:birch_chest_boat", (Object)"minecraft:birch_chest_boat").put((Object)"minecraft:jungle_boat", (Object)"minecraft:jungle_boat").put((Object)"minecraft:jungle_chest_boat", (Object)"minecraft:jungle_chest_boat").put((Object)"minecraft:acacia_boat", (Object)"minecraft:acacia_boat").put((Object)"minecraft:acacia_chest_boat", (Object)"minecraft:acacia_chest_boat").put((Object)"minecraft:cherry_boat", (Object)"minecraft:cherry_boat").put((Object)"minecraft:cherry_chest_boat", (Object)"minecraft:cherry_chest_boat").put((Object)"minecraft:dark_oak_boat", (Object)"minecraft:dark_oak_boat").put((Object)"minecraft:dark_oak_chest_boat", (Object)"minecraft:dark_oak_chest_boat").put((Object)"minecraft:mangrove_boat", (Object)"minecraft:mangrove_boat").put((Object)"minecraft:mangrove_chest_boat", (Object)"minecraft:mangrove_chest_boat").put((Object)"minecraft:bamboo_raft", (Object)"minecraft:bamboo_raft").put((Object)"minecraft:bamboo_chest_raft", (Object)"minecraft:bamboo_chest_raft").put((Object)"minecraft:minecart", (Object)"minecraft:minecart").put((Object)"minecraft:chest_minecart", (Object)"minecraft:chest_minecart").put((Object)"minecraft:furnace_minecart", (Object)"minecraft:furnace_minecart").put((Object)"minecraft:tnt_minecart", (Object)"minecraft:tnt_minecart").put((Object)"minecraft:hopper_minecart", (Object)"minecraft:hopper_minecart").build();
    protected static final Hook.HookFunction ADD_NAMES = new Hook.HookFunction(){

        public <T> T apply(DynamicOps<T> ops, T value) {
            return V99.addNames(new Dynamic(ops, value), V704.ITEM_TO_BLOCKENTITY, ITEM_TO_ENTITY);
        }
    };

    public V705(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    protected static void registerMob(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.registerSimple(map, name);
    }

    protected static void registerThrowableProjectile(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
    }

    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        HashMap map = Maps.newHashMap();
        schema.register((Map)map, "minecraft:area_effect_cloud", name -> DSL.optionalFields((String)"Particle", (TypeTemplate)References.PARTICLE.in(schema)));
        V705.registerMob(schema, map, "minecraft:armor_stand");
        schema.register((Map)map, "minecraft:arrow", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:bat");
        V705.registerMob(schema, map, "minecraft:blaze");
        schema.registerSimple((Map)map, "minecraft:boat");
        V705.registerMob(schema, map, "minecraft:cave_spider");
        schema.register((Map)map, "minecraft:chest_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        V705.registerMob(schema, map, "minecraft:chicken");
        schema.register((Map)map, "minecraft:commandblock_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"LastOutput", (TypeTemplate)References.TEXT_COMPONENT.in(schema)));
        V705.registerMob(schema, map, "minecraft:cow");
        V705.registerMob(schema, map, "minecraft:creeper");
        schema.register((Map)map, "minecraft:donkey", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:dragon_fireball");
        V705.registerThrowableProjectile(schema, map, "minecraft:egg");
        V705.registerMob(schema, map, "minecraft:elder_guardian");
        schema.registerSimple((Map)map, "minecraft:ender_crystal");
        V705.registerMob(schema, map, "minecraft:ender_dragon");
        schema.register((Map)map, "minecraft:enderman", name -> DSL.optionalFields((String)"carried", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:endermite");
        V705.registerThrowableProjectile(schema, map, "minecraft:ender_pearl");
        schema.registerSimple((Map)map, "minecraft:eye_of_ender_signal");
        schema.register((Map)map, "minecraft:falling_block", name -> DSL.optionalFields((String)"Block", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"TileEntityData", (TypeTemplate)References.BLOCK_ENTITY.in(schema)));
        V705.registerThrowableProjectile(schema, map, "minecraft:fireball");
        schema.register((Map)map, "minecraft:fireworks_rocket", name -> DSL.optionalFields((String)"FireworksItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register((Map)map, "minecraft:furnace_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:ghast");
        V705.registerMob(schema, map, "minecraft:giant");
        V705.registerMob(schema, map, "minecraft:guardian");
        schema.register((Map)map, "minecraft:hopper_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema))));
        schema.register((Map)map, "minecraft:horse", name -> DSL.optionalFields((String)"ArmorItem", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V705.registerMob(schema, map, "minecraft:husk");
        schema.register((Map)map, "minecraft:item", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.register((Map)map, "minecraft:item_frame", name -> DSL.optionalFields((String)"Item", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:leash_knot");
        V705.registerMob(schema, map, "minecraft:magma_cube");
        schema.register((Map)map, "minecraft:minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:mooshroom");
        schema.register((Map)map, "minecraft:mule", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V705.registerMob(schema, map, "minecraft:ocelot");
        schema.registerSimple((Map)map, "minecraft:painting");
        V705.registerMob(schema, map, "minecraft:parrot");
        V705.registerMob(schema, map, "minecraft:pig");
        V705.registerMob(schema, map, "minecraft:polar_bear");
        schema.register((Map)map, "minecraft:potion", name -> DSL.optionalFields((String)"Potion", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:rabbit");
        V705.registerMob(schema, map, "minecraft:sheep");
        V705.registerMob(schema, map, "minecraft:shulker");
        schema.registerSimple((Map)map, "minecraft:shulker_bullet");
        V705.registerMob(schema, map, "minecraft:silverfish");
        V705.registerMob(schema, map, "minecraft:skeleton");
        schema.register((Map)map, "minecraft:skeleton_horse", name -> DSL.optionalFields((String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V705.registerMob(schema, map, "minecraft:slime");
        V705.registerThrowableProjectile(schema, map, "minecraft:small_fireball");
        V705.registerThrowableProjectile(schema, map, "minecraft:snowball");
        V705.registerMob(schema, map, "minecraft:snowman");
        schema.register((Map)map, "minecraft:spawner_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)References.UNTAGGED_SPAWNER.in(schema)));
        schema.register((Map)map, "minecraft:spectral_arrow", name -> DSL.optionalFields((String)"inTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        V705.registerMob(schema, map, "minecraft:spider");
        V705.registerMob(schema, map, "minecraft:squid");
        V705.registerMob(schema, map, "minecraft:stray");
        schema.registerSimple((Map)map, "minecraft:tnt");
        schema.register((Map)map, "minecraft:tnt_minecart", name -> DSL.optionalFields((String)"DisplayTile", (TypeTemplate)References.BLOCK_NAME.in(schema)));
        schema.register((Map)map, "minecraft:villager", name -> DSL.optionalFields((String)"Inventory", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"Offers", (TypeTemplate)DSL.optionalFields((String)"Recipes", (TypeTemplate)DSL.list((TypeTemplate)References.VILLAGER_TRADE.in(schema)))));
        V705.registerMob(schema, map, "minecraft:villager_golem");
        V705.registerMob(schema, map, "minecraft:witch");
        V705.registerMob(schema, map, "minecraft:wither");
        V705.registerMob(schema, map, "minecraft:wither_skeleton");
        V705.registerThrowableProjectile(schema, map, "minecraft:wither_skull");
        V705.registerMob(schema, map, "minecraft:wolf");
        V705.registerThrowableProjectile(schema, map, "minecraft:xp_bottle");
        schema.registerSimple((Map)map, "minecraft:xp_orb");
        V705.registerMob(schema, map, "minecraft:zombie");
        schema.register((Map)map, "minecraft:zombie_horse", name -> DSL.optionalFields((String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        V705.registerMob(schema, map, "minecraft:zombie_pigman");
        schema.register((Map)map, "minecraft:zombie_villager", name -> DSL.optionalFields((String)"Offers", (TypeTemplate)DSL.optionalFields((String)"Recipes", (TypeTemplate)DSL.list((TypeTemplate)References.VILLAGER_TRADE.in(schema)))));
        schema.registerSimple((Map)map, "minecraft:evocation_fangs");
        V705.registerMob(schema, map, "minecraft:evocation_illager");
        V705.registerMob(schema, map, "minecraft:illusion_illager");
        schema.register((Map)map, "minecraft:llama", name -> DSL.optionalFields((String)"Items", (TypeTemplate)DSL.list((TypeTemplate)References.ITEM_STACK.in(schema)), (String)"SaddleItem", (TypeTemplate)References.ITEM_STACK.in(schema), (String)"DecorItem", (TypeTemplate)References.ITEM_STACK.in(schema)));
        schema.registerSimple((Map)map, "minecraft:llama_spit");
        V705.registerMob(schema, map, "minecraft:vex");
        V705.registerMob(schema, map, "minecraft:vindication_illager");
        return map;
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        schema.registerType(true, References.ENTITY, () -> DSL.and((TypeTemplate)References.ENTITY_EQUIPMENT.in(schema), (TypeTemplate)DSL.optionalFields((String)"CustomName", (TypeTemplate)DSL.constType((Type)DSL.string()), (TypeTemplate)DSL.taggedChoiceLazy((String)"id", V705.namespacedString(), (Map)entityTypes))));
        schema.registerType(true, References.ITEM_STACK, () -> DSL.hook((TypeTemplate)DSL.optionalFields((String)"id", (TypeTemplate)References.ITEM_NAME.in(schema), (String)"tag", (TypeTemplate)V99.itemStackTag(schema)), (Hook.HookFunction)ADD_NAMES, (Hook.HookFunction)Hook.HookFunction.IDENTITY));
    }
}

