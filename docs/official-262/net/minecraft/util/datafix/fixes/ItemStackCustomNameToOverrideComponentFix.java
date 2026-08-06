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
 *  com.mojang.serialization.OptionalDynamic
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
import com.mojang.serialization.OptionalDynamic;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackCustomNameToOverrideComponentFix
extends DataFix {
    private static final Set<String> MAP_NAMES = Set.of("filled_map.buried_treasure", "filled_map.explorer_jungle", "filled_map.explorer_swamp", "filled_map.mansion", "filled_map.monument", "filled_map.trial_chambers", "filled_map.village_desert", "filled_map.village_plains", "filled_map.village_savanna", "filled_map.village_snowy", "filled_map.village_taiga");

    public ItemStackCustomNameToOverrideComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public final TypeRewriteRule makeRule() {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder idFinder = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder componentsFinder = itemStackType.findField("components");
        return this.fixTypeEverywhereTyped("ItemStack custom_name to item_name component fix", itemStackType, input -> {
            Optional id = input.getOptional(idFinder);
            Optional<String> maybeId = id.map(Pair::getSecond);
            if (maybeId.filter(s -> s.equals("minecraft:white_banner")).isPresent()) {
                return input.updateTyped(componentsFinder, ItemStackCustomNameToOverrideComponentFix::fixBanner);
            }
            if (maybeId.filter(s -> s.equals("minecraft:filled_map")).isPresent()) {
                return input.updateTyped(componentsFinder, ItemStackCustomNameToOverrideComponentFix::fixMap);
            }
            return input;
        });
    }

    private static <T> Typed<T> fixMap(Typed<T> value) {
        return ItemStackCustomNameToOverrideComponentFix.fixCustomName(value, MAP_NAMES::contains);
    }

    private static <T> Typed<T> fixBanner(Typed<T> value) {
        return ItemStackCustomNameToOverrideComponentFix.fixCustomName(value, e -> e.equals("block.minecraft.ominous_banner"));
    }

    private static <T> Typed<T> fixCustomName(Typed<T> typed, Predicate<String> expectedTranslationKey) {
        return Util.writeAndReadTypedOrThrow(typed, typed.getType(), value -> {
            OptionalDynamic customNameTag = value.get("minecraft:custom_name");
            Optional hasCorrectTranslationKey = customNameTag.asString().result().flatMap(LegacyComponentDataFixUtils::extractTranslationString).filter(expectedTranslationKey);
            if (hasCorrectTranslationKey.isPresent()) {
                return value.renameField("minecraft:custom_name", "minecraft:item_name");
            }
            return value;
        });
    }
}

