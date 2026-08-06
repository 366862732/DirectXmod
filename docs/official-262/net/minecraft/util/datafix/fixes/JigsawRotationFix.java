/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import net.minecraft.util.datafix.fixes.AbstractBlockPropertyFix;

public class JigsawRotationFix
extends AbstractBlockPropertyFix {
    private static final Map<String, String> RENAMES = ImmutableMap.builder().put((Object)"down", (Object)"down_south").put((Object)"up", (Object)"up_north").put((Object)"north", (Object)"north_up").put((Object)"south", (Object)"south_up").put((Object)"west", (Object)"west_up").put((Object)"east", (Object)"east_up").build();

    public JigsawRotationFix(Schema outputSchema) {
        super(outputSchema, "jigsaw_rotation_fix");
    }

    @Override
    protected boolean shouldFix(String blockId) {
        return blockId.equals("minecraft:jigsaw");
    }

    @Override
    protected <T> Dynamic<T> fixProperties(String blockId, Dynamic<T> properties) {
        String facing = properties.get("facing").asString("north");
        return properties.remove("facing").set("orientation", properties.createString(RENAMES.getOrDefault(facing, facing)));
    }
}

