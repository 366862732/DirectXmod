/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntitySpawnerItemVariantComponentFix
extends DataFix {
    public EntitySpawnerItemVariantComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public final TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idFinder = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder componentsFinder = itemStackType.findField("components");
        return this.fixTypeEverywhereTyped("ItemStack bucket_entity_data variants to separate components", itemStackType, input -> {
            String id;
            return switch (id = input.getOptional(idFinder).map(Pair::getSecond).orElse("")) {
                case "minecraft:salmon_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
                case "minecraft:axolotl_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
                case "minecraft:tropical_fish_bucket" -> input.updateTyped(componentsFinder, EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
                case "minecraft:painting" -> input.updateTyped(componentsFinder, components -> Util.writeAndReadTypedOrThrow(components, components.getType(), EntitySpawnerItemVariantComponentFix::fixPainting));
                default -> input;
            };
        });
    }

    private static String getBaseColor(int packedVariant) {
        return ExtraDataFixUtils.dyeColorIdToName(packedVariant >> 16 & 0xFF);
    }

    private static String getPatternColor(int packedVariant) {
        return ExtraDataFixUtils.dyeColorIdToName(packedVariant >> 24 & 0xFF);
    }

    private static String getPattern(int packedVariant) {
        return switch (packedVariant & 0xFFFF) {
            default -> "kob";
            case 256 -> "sunstreak";
            case 512 -> "snooper";
            case 768 -> "dasher";
            case 1024 -> "brinely";
            case 1280 -> "spotty";
            case 1 -> "flopper";
            case 257 -> "stripey";
            case 513 -> "glitter";
            case 769 -> "blockfish";
            case 1025 -> "betty";
            case 1281 -> "clayfish";
        };
    }

    private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> remainder, Dynamic<T> bucketData) {
        Optional oldVariant = bucketData.get("BucketVariantTag").asNumber().result();
        if (oldVariant.isEmpty()) {
            return remainder;
        }
        int packedVariant = ((Number)oldVariant.get()).intValue();
        String pattern = EntitySpawnerItemVariantComponentFix.getPattern(packedVariant);
        String baseColor = EntitySpawnerItemVariantComponentFix.getBaseColor(packedVariant);
        String patternColor = EntitySpawnerItemVariantComponentFix.getPatternColor(packedVariant);
        return remainder.update("minecraft:bucket_entity_data", b -> b.remove("BucketVariantTag")).set("minecraft:tropical_fish/pattern", remainder.createString(pattern)).set("minecraft:tropical_fish/base_color", remainder.createString(baseColor)).set("minecraft:tropical_fish/pattern_color", remainder.createString(patternColor));
    }

    private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> remainder, Dynamic<T> bucketData) {
        Optional oldVariant = bucketData.get("Variant").asNumber().result();
        if (oldVariant.isEmpty()) {
            return remainder;
        }
        String newVariant = switch (((Number)oldVariant.get()).intValue()) {
            default -> "lucy";
            case 1 -> "wild";
            case 2 -> "gold";
            case 3 -> "cyan";
            case 4 -> "blue";
        };
        return remainder.update("minecraft:bucket_entity_data", b -> b.remove("Variant")).set("minecraft:axolotl/variant", remainder.createString(newVariant));
    }

    private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> remainder, Dynamic<T> bucketData) {
        Optional type = bucketData.get("type").result();
        if (type.isEmpty()) {
            return remainder;
        }
        return remainder.update("minecraft:bucket_entity_data", b -> b.remove("type")).set("minecraft:salmon/size", (Dynamic)type.get());
    }

    private static <T> Dynamic<T> fixPainting(Dynamic<T> components) {
        Optional entityData = components.get("minecraft:entity_data").result();
        if (entityData.isEmpty()) {
            return components;
        }
        if (((Dynamic)entityData.get()).get("id").asString().result().filter(id -> id.equals("minecraft:painting")).isEmpty()) {
            return components;
        }
        Optional result = ((Dynamic)entityData.get()).get("variant").result();
        Dynamic entityDataRemainder = ((Dynamic)entityData.get()).remove("variant");
        components = entityDataRemainder.remove("id").equals((Object)entityDataRemainder.emptyMap()) ? components.remove("minecraft:entity_data") : components.set("minecraft:entity_data", entityDataRemainder);
        if (result.isPresent()) {
            components = components.set("minecraft:painting/variant", (Dynamic)result.get());
        }
        return components;
    }

    @FunctionalInterface
    private static interface Fixer
    extends Function<Typed<?>, Typed<?>> {
        @Override
        default public Typed<?> apply(Typed<?> components) {
            return components.update(DSL.remainderFinder(), this::fixRemainder);
        }

        default public <T> Dynamic<T> fixRemainder(Dynamic<T> remainder) {
            return remainder.get("minecraft:bucket_entity_data").result().map(bucketData -> this.fixRemainder(remainder, (Dynamic)bucketData)).orElse(remainder);
        }

        public <T> Dynamic<T> fixRemainder(Dynamic<T> var1, Dynamic<T> var2);
    }
}

