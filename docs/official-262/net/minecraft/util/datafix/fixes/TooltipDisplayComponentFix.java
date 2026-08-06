/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;

public class TooltipDisplayComponentFix
extends DataFix {
    private static final List<String> CONVERTED_ADDITIONAL_TOOLTIP_TYPES = List.of("minecraft:banner_patterns", "minecraft:bees", "minecraft:block_entity_data", "minecraft:block_state", "minecraft:bundle_contents", "minecraft:charged_projectiles", "minecraft:container", "minecraft:container_loot", "minecraft:firework_explosion", "minecraft:fireworks", "minecraft:instrument", "minecraft:map_id", "minecraft:painting/variant", "minecraft:pot_decorations", "minecraft:potion_contents", "minecraft:tropical_fish/pattern", "minecraft:written_book_content");

    public TooltipDisplayComponentFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type componentsType = this.getInputSchema().getType(References.DATA_COMPONENTS);
        Type newComponentsType = this.getOutputSchema().getType(References.DATA_COMPONENTS);
        OpticFinder canPlaceOnFinder = componentsType.findField("minecraft:can_place_on");
        OpticFinder canBreakFinder = componentsType.findField("minecraft:can_break");
        Type newCanPlaceOnType = newComponentsType.findFieldType("minecraft:can_place_on");
        Type newCanBreakType = newComponentsType.findFieldType("minecraft:can_break");
        return this.fixTypeEverywhereTyped("TooltipDisplayComponentFix", componentsType, newComponentsType, typed -> TooltipDisplayComponentFix.fix(typed, canPlaceOnFinder, canBreakFinder, newCanPlaceOnType, newCanBreakType));
    }

    private static Typed<?> fix(Typed<?> typed, OpticFinder<?> canPlaceOnFinder, OpticFinder<?> canBreakFinder, Type<?> newCanPlaceOnType, Type<?> newCanBreakType) {
        HashSet<String> hiddenTooltips = new HashSet<String>();
        typed = TooltipDisplayComponentFix.fixAdventureModePredicate(typed, canPlaceOnFinder, newCanPlaceOnType, "minecraft:can_place_on", hiddenTooltips);
        typed = TooltipDisplayComponentFix.fixAdventureModePredicate(typed, canBreakFinder, newCanBreakType, "minecraft:can_break", hiddenTooltips);
        return typed.update(DSL.remainderFinder(), remainder -> {
            remainder = TooltipDisplayComponentFix.fixSimpleComponent(remainder, "minecraft:trim", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixSimpleComponent(remainder, "minecraft:unbreakable", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixComponentAndUnwrap(remainder, "minecraft:dyed_color", "rgb", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixComponentAndUnwrap(remainder, "minecraft:attribute_modifiers", "modifiers", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixComponentAndUnwrap(remainder, "minecraft:enchantments", "levels", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixComponentAndUnwrap(remainder, "minecraft:stored_enchantments", "levels", hiddenTooltips);
            remainder = TooltipDisplayComponentFix.fixComponentAndUnwrap(remainder, "minecraft:jukebox_playable", "song", hiddenTooltips);
            boolean hideTooltip = remainder.get("minecraft:hide_tooltip").result().isPresent();
            remainder = remainder.remove("minecraft:hide_tooltip");
            boolean hideAdditionalTooltip = remainder.get("minecraft:hide_additional_tooltip").result().isPresent();
            remainder = remainder.remove("minecraft:hide_additional_tooltip");
            if (hideAdditionalTooltip) {
                for (String componentId : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                    if (!remainder.get(componentId).result().isPresent()) continue;
                    hiddenTooltips.add(componentId);
                }
            }
            if (hiddenTooltips.isEmpty() && !hideTooltip) {
                return remainder;
            }
            return remainder.set("minecraft:tooltip_display", remainder.createMap(Map.of(remainder.createString("hide_tooltip"), remainder.createBoolean(hideTooltip), remainder.createString("hidden_components"), remainder.createList(hiddenTooltips.stream().map(arg_0 -> ((Dynamic)remainder).createString(arg_0))))));
        });
    }

    private static Dynamic<?> fixSimpleComponent(Dynamic<?> remainder, String componentId, Set<String> hiddenTooltips) {
        return TooltipDisplayComponentFix.fixRemainderComponent(remainder, componentId, hiddenTooltips, UnaryOperator.identity());
    }

    private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> remainder, String componentId, String fieldName, Set<String> hiddenTooltips) {
        return TooltipDisplayComponentFix.fixRemainderComponent(remainder, componentId, hiddenTooltips, component -> (Dynamic)DataFixUtils.orElse((Optional)component.get(fieldName).result(), (Object)component));
    }

    private static Dynamic<?> fixRemainderComponent(Dynamic<?> remainder, String componentId, Set<String> hiddenTooltips, UnaryOperator<Dynamic<?>> fixer) {
        return remainder.update(componentId, component -> {
            boolean showInTooltip = component.get("show_in_tooltip").asBoolean(true);
            if (!showInTooltip) {
                hiddenTooltips.add(componentId);
            }
            return (Dynamic)fixer.apply(component.remove("show_in_tooltip"));
        });
    }

    private static Typed<?> fixAdventureModePredicate(Typed<?> typedComponents, OpticFinder<?> componentFinder, Type<?> newType, String componentId, Set<String> hiddenTooltips) {
        return typedComponents.updateTyped(componentFinder, newType, typedComponent -> Util.writeAndReadTypedOrThrow(typedComponent, newType, component -> {
            OptionalDynamic predicates = component.get("predicates");
            if (predicates.result().isEmpty()) {
                return component;
            }
            boolean showInTooltip = component.get("show_in_tooltip").asBoolean(true);
            if (!showInTooltip) {
                hiddenTooltips.add(componentId);
            }
            return (Dynamic)predicates.result().get();
        }));
    }
}

