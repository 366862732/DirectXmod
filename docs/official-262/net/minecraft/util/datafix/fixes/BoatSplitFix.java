/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BoatSplitFix
extends DataFix {
    public BoatSplitFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    private static boolean isNormalBoat(String id) {
        return id.equals("minecraft:boat");
    }

    private static boolean isChestBoat(String id) {
        return id.equals("minecraft:chest_boat");
    }

    private static boolean isAnyBoat(String id) {
        return BoatSplitFix.isNormalBoat(id) || BoatSplitFix.isChestBoat(id);
    }

    private static String mapVariantToNormalBoat(String id) {
        return switch (id) {
            default -> "minecraft:oak_boat";
            case "spruce" -> "minecraft:spruce_boat";
            case "birch" -> "minecraft:birch_boat";
            case "jungle" -> "minecraft:jungle_boat";
            case "acacia" -> "minecraft:acacia_boat";
            case "cherry" -> "minecraft:cherry_boat";
            case "dark_oak" -> "minecraft:dark_oak_boat";
            case "mangrove" -> "minecraft:mangrove_boat";
            case "bamboo" -> "minecraft:bamboo_raft";
        };
    }

    private static String mapVariantToChestBoat(String id) {
        return switch (id) {
            default -> "minecraft:oak_chest_boat";
            case "spruce" -> "minecraft:spruce_chest_boat";
            case "birch" -> "minecraft:birch_chest_boat";
            case "jungle" -> "minecraft:jungle_chest_boat";
            case "acacia" -> "minecraft:acacia_chest_boat";
            case "cherry" -> "minecraft:cherry_chest_boat";
            case "dark_oak" -> "minecraft:dark_oak_chest_boat";
            case "mangrove" -> "minecraft:mangrove_chest_boat";
            case "bamboo" -> "minecraft:bamboo_chest_raft";
        };
    }

    public TypeRewriteRule makeRule() {
        OpticFinder idF = DSL.fieldFinder((String)"id", NamespacedSchema.namespacedString());
        Type oldType = this.getInputSchema().getType(References.ENTITY);
        Type newType = this.getOutputSchema().getType(References.ENTITY);
        return this.fixTypeEverywhereTyped("BoatSplitFix", oldType, newType, input -> {
            Optional id = input.getOptional(idF);
            if (id.isPresent() && BoatSplitFix.isAnyBoat((String)id.get())) {
                Dynamic tag = (Dynamic)input.getOrCreate(DSL.remainderFinder());
                Optional maybeBoatId = tag.get("Type").asString().result();
                String newId = BoatSplitFix.isChestBoat((String)id.get()) ? maybeBoatId.map(BoatSplitFix::mapVariantToChestBoat).orElse("minecraft:oak_chest_boat") : maybeBoatId.map(BoatSplitFix::mapVariantToNormalBoat).orElse("minecraft:oak_boat");
                return ExtraDataFixUtils.cast(newType, input).update(DSL.remainderFinder(), remainder -> remainder.remove("Type")).set(idF, (Object)newId);
            }
            return ExtraDataFixUtils.cast(newType, input);
        });
    }
}

