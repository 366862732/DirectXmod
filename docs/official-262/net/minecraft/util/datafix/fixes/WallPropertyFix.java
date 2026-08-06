/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.util.datafix.fixes.References;

public class WallPropertyFix
extends DataFix {
    private static final Set<String> WALL_BLOCKS = ImmutableSet.of((Object)"minecraft:andesite_wall", (Object)"minecraft:brick_wall", (Object)"minecraft:cobblestone_wall", (Object)"minecraft:diorite_wall", (Object)"minecraft:end_stone_brick_wall", (Object)"minecraft:granite_wall", (Object[])new String[]{"minecraft:mossy_cobblestone_wall", "minecraft:mossy_stone_brick_wall", "minecraft:nether_brick_wall", "minecraft:prismarine_wall", "minecraft:red_nether_brick_wall", "minecraft:red_sandstone_wall", "minecraft:sandstone_wall", "minecraft:stone_brick_wall"});

    public WallPropertyFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("WallPropertyFix", this.getInputSchema().getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), WallPropertyFix::upgradeBlockStateTag));
    }

    private static String mapProperty(String value) {
        return "true".equals(value) ? "low" : "none";
    }

    private static <T> Dynamic<T> fixWallProperty(Dynamic<T> state, String property) {
        return state.update(property, value -> (Dynamic)DataFixUtils.orElse(value.asString().result().map(WallPropertyFix::mapProperty).map(arg_0 -> ((Dynamic)value).createString(arg_0)), (Object)value));
    }

    private static <T> Dynamic<T> upgradeBlockStateTag(Dynamic<T> state) {
        boolean isWall = state.get("Name").asString().result().filter(WALL_BLOCKS::contains).isPresent();
        if (!isWall) {
            return state;
        }
        return state.update("Properties", properties -> {
            Dynamic newState = WallPropertyFix.fixWallProperty(properties, "east");
            newState = WallPropertyFix.fixWallProperty(newState, "west");
            newState = WallPropertyFix.fixWallProperty(newState, "north");
            return WallPropertyFix.fixWallProperty(newState, "south");
        });
    }
}

