/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Splitter
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.OptionalDynamic
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;

public class ItemStackComponentizationFix
extends DataFix {
    private static final int HIDE_ENCHANTMENTS = 1;
    private static final int HIDE_MODIFIERS = 2;
    private static final int HIDE_UNBREAKABLE = 4;
    private static final int HIDE_CAN_DESTROY = 8;
    private static final int HIDE_CAN_PLACE = 16;
    private static final int HIDE_ADDITIONAL = 32;
    private static final int HIDE_DYE = 64;
    private static final int HIDE_UPGRADES = 128;
    private static final Set<String> POTION_HOLDER_IDS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");
    private static final Set<String> BUCKETED_MOB_IDS = Set.of("minecraft:pufferfish_bucket", "minecraft:salmon_bucket", "minecraft:cod_bucket", "minecraft:tropical_fish_bucket", "minecraft:axolotl_bucket", "minecraft:tadpole_bucket");
    private static final List<String> BUCKETED_MOB_TAGS = List.of("NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag");
    private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of("attached", "bottom", "conditional", "disarmed", "drag", "enabled", "extended", "eye", "falling", "hanging", "has_bottle_0", "has_bottle_1", "has_bottle_2", "has_record", "has_book", "inverted", "in_wall", "lit", "locked", "occupied", "open", "persistent", "powered", "short", "signal_fire", "snowy", "triggered", "unstable", "waterlogged", "berries", "bloom", "shrieking", "can_summon", "up", "down", "north", "east", "south", "west", "slot_0_occupied", "slot_1_occupied", "slot_2_occupied", "slot_3_occupied", "slot_4_occupied", "slot_5_occupied", "cracked", "crafting");
    private static final Splitter PROPERTY_SPLITTER = Splitter.on((char)',');

    public ItemStackComponentizationFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    private static void fixItemStack(ItemStackData itemStack, Dynamic<?> dynamic) {
        int hideFlags = itemStack.removeTag("HideFlags").asInt(0);
        itemStack.moveTagToComponent("Damage", "minecraft:damage", dynamic.createInt(0));
        itemStack.moveTagToComponent("RepairCost", "minecraft:repair_cost", dynamic.createInt(0));
        itemStack.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
        itemStack.removeTag("BlockStateTag").result().ifPresent(blockStateTag -> itemStack.setComponent("minecraft:block_state", ItemStackComponentizationFix.fixBlockStateTag(blockStateTag)));
        itemStack.moveTagToComponent("EntityTag", "minecraft:entity_data");
        itemStack.fixSubTag("BlockEntityTag", false, blockEntityTag -> {
            String id = NamespacedSchema.ensureNamespaced(blockEntityTag.get("id").asString(""));
            Dynamic withoutId = (blockEntityTag = ItemStackComponentizationFix.fixBlockEntityTag(itemStack, blockEntityTag, id)).remove("id");
            if (withoutId.equals((Object)blockEntityTag.emptyMap())) {
                return withoutId;
            }
            return blockEntityTag;
        });
        itemStack.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
        if (itemStack.removeTag("Unbreakable").asBoolean(false)) {
            Dynamic component = dynamic.emptyMap();
            if ((hideFlags & 4) != 0) {
                component = component.set("show_in_tooltip", dynamic.createBoolean(false));
            }
            itemStack.setComponent("minecraft:unbreakable", component);
        }
        ItemStackComponentizationFix.fixEnchantments(itemStack, dynamic, "Enchantments", "minecraft:enchantments", (hideFlags & 1) != 0);
        if (itemStack.is("minecraft:enchanted_book")) {
            ItemStackComponentizationFix.fixEnchantments(itemStack, dynamic, "StoredEnchantments", "minecraft:stored_enchantments", (hideFlags & 0x20) != 0);
        }
        itemStack.fixSubTag("display", false, display -> ItemStackComponentizationFix.fixDisplay(itemStack, display, hideFlags));
        ItemStackComponentizationFix.fixAdventureModeChecks(itemStack, dynamic, hideFlags);
        ItemStackComponentizationFix.fixAttributeModifiers(itemStack, dynamic, hideFlags);
        Optional trim = itemStack.removeTag("Trim").result();
        if (trim.isPresent()) {
            Dynamic fixedTrim = (Dynamic)trim.get();
            if ((hideFlags & 0x80) != 0) {
                fixedTrim = fixedTrim.set("show_in_tooltip", fixedTrim.createBoolean(false));
            }
            itemStack.setComponent("minecraft:trim", fixedTrim);
        }
        if ((hideFlags & 0x20) != 0) {
            itemStack.setComponent("minecraft:hide_additional_tooltip", dynamic.emptyMap());
        }
        if (itemStack.is("minecraft:crossbow")) {
            itemStack.removeTag("Charged");
            itemStack.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", dynamic.createList(Stream.empty()));
        }
        if (itemStack.is("minecraft:bundle")) {
            itemStack.moveTagToComponent("Items", "minecraft:bundle_contents", dynamic.createList(Stream.empty()));
        }
        if (itemStack.is("minecraft:filled_map")) {
            itemStack.moveTagToComponent("map", "minecraft:map_id");
            Map<Dynamic, Dynamic> decorations = itemStack.removeTag("Decorations").asStream().map(ItemStackComponentizationFix::fixMapDecoration).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (first, second) -> first));
            if (!decorations.isEmpty()) {
                itemStack.setComponent("minecraft:map_decorations", dynamic.createMap(decorations));
            }
        }
        if (itemStack.is(POTION_HOLDER_IDS)) {
            ItemStackComponentizationFix.fixPotionContents(itemStack, dynamic);
        }
        if (itemStack.is("minecraft:writable_book")) {
            ItemStackComponentizationFix.fixWritableBook(itemStack, dynamic);
        }
        if (itemStack.is("minecraft:written_book")) {
            ItemStackComponentizationFix.fixWrittenBook(itemStack, dynamic);
        }
        if (itemStack.is("minecraft:suspicious_stew")) {
            itemStack.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
        }
        if (itemStack.is("minecraft:debug_stick")) {
            itemStack.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
        }
        if (itemStack.is(BUCKETED_MOB_IDS)) {
            ItemStackComponentizationFix.fixBucketedMobData(itemStack, dynamic);
        }
        if (itemStack.is("minecraft:goat_horn")) {
            itemStack.moveTagToComponent("instrument", "minecraft:instrument");
        }
        if (itemStack.is("minecraft:knowledge_book")) {
            itemStack.moveTagToComponent("Recipes", "minecraft:recipes");
        }
        if (itemStack.is("minecraft:compass")) {
            ItemStackComponentizationFix.fixLodestoneTracker(itemStack, dynamic);
        }
        if (itemStack.is("minecraft:firework_rocket")) {
            ItemStackComponentizationFix.fixFireworkRocket(itemStack);
        }
        if (itemStack.is("minecraft:firework_star")) {
            ItemStackComponentizationFix.fixFireworkStar(itemStack);
        }
        if (itemStack.is("minecraft:player_head")) {
            itemStack.removeTag("SkullOwner").result().ifPresent(skullOwner -> itemStack.setComponent("minecraft:profile", ItemStackComponentizationFix.fixProfile(skullOwner)));
        }
    }

    private static Dynamic<?> fixBlockStateTag(Dynamic<?> blockStateTag) {
        return (Dynamic)DataFixUtils.orElse(blockStateTag.asMapOpt().result().map(entries -> entries.collect(Collectors.toMap(Pair::getFirst, entry -> {
            Optional bool;
            String key = ((Dynamic)entry.getFirst()).asString("");
            Dynamic value = (Dynamic)entry.getSecond();
            if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(key) && (bool = value.asBoolean().result()).isPresent()) {
                return value.createString(String.valueOf(bool.get()));
            }
            Optional number = value.asNumber().result();
            if (number.isPresent()) {
                return value.createString(((Number)number.get()).toString());
            }
            return value;
        }))).map(arg_0 -> blockStateTag.createMap(arg_0)), blockStateTag);
    }

    private static Dynamic<?> fixDisplay(ItemStackData itemStack, Dynamic<?> display, int hideFlags) {
        Optional locName;
        boolean hideDye;
        display.get("Name").result().filter(LegacyComponentDataFixUtils::isStrictlyValidJson).ifPresent(name -> itemStack.setComponent("minecraft:custom_name", (Dynamic<?>)name));
        OptionalDynamic lore = display.get("Lore");
        if (lore.result().isPresent()) {
            itemStack.setComponent("minecraft:lore", display.createList(display.get("Lore").asStream().filter(LegacyComponentDataFixUtils::isStrictlyValidJson)));
        }
        Optional<Integer> color = display.get("color").asNumber().result().map(Number::intValue);
        boolean bl = hideDye = (hideFlags & 0x40) != 0;
        if (color.isPresent() || hideDye) {
            Dynamic dyedColor = display.emptyMap().set("rgb", display.createInt(color.orElse(10511680).intValue()));
            if (hideDye) {
                dyedColor = dyedColor.set("show_in_tooltip", display.createBoolean(false));
            }
            itemStack.setComponent("minecraft:dyed_color", dyedColor);
        }
        if ((locName = display.get("LocName").asString().result()).isPresent()) {
            itemStack.setComponent("minecraft:item_name", LegacyComponentDataFixUtils.createTranslatableComponent(display.getOps(), (String)locName.get()));
        }
        if (itemStack.is("minecraft:filled_map")) {
            itemStack.setComponent("minecraft:map_color", display.get("MapColor"));
            display = display.remove("MapColor");
        }
        return display.remove("Name").remove("Lore").remove("color").remove("LocName");
    }

    private static <T> Dynamic<T> fixBlockEntityTag(ItemStackData itemStack, Dynamic<T> blockEntity, String id) {
        itemStack.setComponent("minecraft:lock", blockEntity.get("Lock"));
        blockEntity = blockEntity.remove("Lock");
        Optional lootTable = blockEntity.get("LootTable").result();
        if (lootTable.isPresent()) {
            Dynamic containerLoot = blockEntity.emptyMap().set("loot_table", (Dynamic)lootTable.get());
            long seed = blockEntity.get("LootTableSeed").asLong(0L);
            if (seed != 0L) {
                containerLoot = containerLoot.set("seed", blockEntity.createLong(seed));
            }
            itemStack.setComponent("minecraft:container_loot", containerLoot);
            blockEntity = blockEntity.remove("LootTable").remove("LootTableSeed");
        }
        return switch (id) {
            case "minecraft:skull" -> {
                itemStack.setComponent("minecraft:note_block_sound", blockEntity.get("note_block_sound"));
                yield blockEntity.remove("note_block_sound");
            }
            case "minecraft:decorated_pot" -> {
                itemStack.setComponent("minecraft:pot_decorations", blockEntity.get("sherds"));
                Optional item = blockEntity.get("item").result();
                if (item.isPresent()) {
                    itemStack.setComponent("minecraft:container", blockEntity.createList(Stream.of(blockEntity.emptyMap().set("slot", blockEntity.createInt(0)).set("item", (Dynamic)item.get()))));
                }
                yield blockEntity.remove("sherds").remove("item");
            }
            case "minecraft:banner" -> {
                itemStack.setComponent("minecraft:banner_patterns", blockEntity.get("patterns"));
                Optional base = blockEntity.get("Base").asNumber().result();
                if (base.isPresent()) {
                    itemStack.setComponent("minecraft:base_color", blockEntity.createString(ExtraDataFixUtils.dyeColorIdToName(((Number)base.get()).intValue())));
                }
                yield blockEntity.remove("patterns").remove("Base");
            }
            case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
                List items = blockEntity.get("Items").asList(dynamic -> dynamic.emptyMap().set("slot", dynamic.createInt(dynamic.get("Slot").asByte((byte)0) & 0xFF)).set("item", dynamic.remove("Slot")));
                if (!items.isEmpty()) {
                    itemStack.setComponent("minecraft:container", blockEntity.createList(items.stream()));
                }
                yield blockEntity.remove("Items");
            }
            case "minecraft:beehive" -> {
                itemStack.setComponent("minecraft:bees", blockEntity.get("bees"));
                yield blockEntity.remove("bees");
            }
            default -> blockEntity;
        };
    }

    private static void fixEnchantments(ItemStackData itemStack, Dynamic<?> dynamic, String key, String componentType, boolean hideInTooltip) {
        OptionalDynamic<?> rawEnchantments = itemStack.removeTag(key);
        List<Pair> enchantments = rawEnchantments.asList(Function.identity()).stream().flatMap(enchantment -> ItemStackComponentizationFix.parseEnchantment(enchantment).stream()).filter(enchantment -> (Integer)enchantment.getSecond() > 0).toList();
        if (!enchantments.isEmpty() || hideInTooltip) {
            Dynamic component = dynamic.emptyMap();
            Dynamic levels = dynamic.emptyMap();
            for (Pair enchantment2 : enchantments) {
                levels = levels.set((String)enchantment2.getFirst(), dynamic.createInt(((Integer)enchantment2.getSecond()).intValue()));
            }
            component = component.set("levels", levels);
            if (hideInTooltip) {
                component = component.set("show_in_tooltip", dynamic.createBoolean(false));
            }
            itemStack.setComponent(componentType, component);
        }
        if (rawEnchantments.result().isPresent() && enchantments.isEmpty()) {
            itemStack.setComponent("minecraft:enchantment_glint_override", dynamic.createBoolean(true));
        }
    }

    private static Optional<Pair<String, Integer>> parseEnchantment(Dynamic<?> entry) {
        return entry.get("id").asString().apply2stable((id, level) -> Pair.of((Object)id, (Object)Mth.clamp(level.intValue(), 0, 255)), entry.get("lvl").asNumber()).result();
    }

    private static void fixAdventureModeChecks(ItemStackData itemStack, Dynamic<?> dynamic, int hideFlags) {
        ItemStackComponentizationFix.fixBlockStatePredicates(itemStack, dynamic, "CanDestroy", "minecraft:can_break", (hideFlags & 8) != 0);
        ItemStackComponentizationFix.fixBlockStatePredicates(itemStack, dynamic, "CanPlaceOn", "minecraft:can_place_on", (hideFlags & 0x10) != 0);
    }

    private static void fixBlockStatePredicates(ItemStackData itemStack, Dynamic<?> dynamic, String tag, String componentId, boolean hideInTooltip) {
        Optional oldPredicate = itemStack.removeTag(tag).result();
        if (oldPredicate.isEmpty()) {
            return;
        }
        Dynamic component = dynamic.emptyMap().set("predicates", dynamic.createList(((Dynamic)oldPredicate.get()).asStream().map(value -> (Dynamic)DataFixUtils.orElse((Optional)value.asString().map(string -> ItemStackComponentizationFix.fixBlockStatePredicate(value, string)).result(), (Object)value))));
        if (hideInTooltip) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
        }
        itemStack.setComponent(componentId, component);
    }

    private static Dynamic<?> fixBlockStatePredicate(Dynamic<?> dynamic, String string) {
        int startProperties = string.indexOf(91);
        int startNbt = string.indexOf(123);
        int blockNameEnd = string.length();
        if (startProperties != -1) {
            blockNameEnd = startProperties;
        }
        if (startNbt != -1) {
            blockNameEnd = Math.min(blockNameEnd, startNbt);
        }
        String blockOrTagName = string.substring(0, blockNameEnd);
        Dynamic predicate = dynamic.emptyMap().set("blocks", dynamic.createString(blockOrTagName.trim()));
        int endProperties = string.indexOf(93);
        if (startProperties != -1 && endProperties != -1) {
            Dynamic properties = dynamic.emptyMap();
            Iterable flatProperties = PROPERTY_SPLITTER.split((CharSequence)string.substring(startProperties + 1, endProperties));
            for (String property : flatProperties) {
                int assignment = property.indexOf(61);
                if (assignment == -1) continue;
                String key = property.substring(0, assignment).trim();
                String value = property.substring(assignment + 1).trim();
                properties = properties.set(key, dynamic.createString(value));
            }
            predicate = predicate.set("state", properties);
        }
        int endNbt = string.indexOf(125);
        if (startNbt != -1 && endNbt != -1) {
            predicate = predicate.set("nbt", dynamic.createString(string.substring(startNbt, endNbt + 1)));
        }
        return predicate;
    }

    private static void fixAttributeModifiers(ItemStackData itemStack, Dynamic<?> dynamic, int hideFlags) {
        OptionalDynamic<?> attributeModifiersField = itemStack.removeTag("AttributeModifiers");
        if (attributeModifiersField.result().isEmpty()) {
            return;
        }
        boolean hideInTooltip = (hideFlags & 2) != 0;
        List attributeModifiers = attributeModifiersField.asList(ItemStackComponentizationFix::fixAttributeModifier);
        Dynamic component = dynamic.emptyMap().set("modifiers", dynamic.createList(attributeModifiers.stream()));
        if (hideInTooltip) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
        }
        itemStack.setComponent("minecraft:attribute_modifiers", component);
    }

    private static Dynamic<?> fixAttributeModifier(Dynamic<?> input) {
        Dynamic result = input.emptyMap().set("name", input.createString("")).set("amount", input.createDouble(0.0)).set("operation", input.createString("add_value"));
        result = Dynamic.copyField(input, (String)"AttributeName", (Dynamic)result, (String)"type");
        result = Dynamic.copyField(input, (String)"Slot", (Dynamic)result, (String)"slot");
        result = Dynamic.copyField(input, (String)"UUID", (Dynamic)result, (String)"uuid");
        result = Dynamic.copyField(input, (String)"Name", (Dynamic)result, (String)"name");
        result = Dynamic.copyField(input, (String)"Amount", (Dynamic)result, (String)"amount");
        result = Dynamic.copyAndFixField(input, (String)"Operation", (Dynamic)result, (String)"operation", operation -> operation.createString(switch (operation.asInt(0)) {
            default -> "add_value";
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
        }));
        return result;
    }

    private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(Dynamic<?> decoration) {
        Dynamic id = (Dynamic)DataFixUtils.orElseGet((Optional)decoration.get("id").result(), () -> decoration.createString(""));
        Dynamic value = decoration.emptyMap().set("type", decoration.createString(ItemStackComponentizationFix.fixMapDecorationType(decoration.get("type").asInt(0)))).set("x", decoration.createDouble(decoration.get("x").asDouble(0.0))).set("z", decoration.createDouble(decoration.get("z").asDouble(0.0))).set("rotation", decoration.createFloat((float)decoration.get("rot").asDouble(0.0)));
        return Pair.of((Object)id, (Object)value);
    }

    private static String fixMapDecorationType(int id) {
        return switch (id) {
            default -> "player";
            case 1 -> "frame";
            case 2 -> "red_marker";
            case 3 -> "blue_marker";
            case 4 -> "target_x";
            case 5 -> "target_point";
            case 6 -> "player_off_map";
            case 7 -> "player_off_limits";
            case 8 -> "mansion";
            case 9 -> "monument";
            case 10 -> "banner_white";
            case 11 -> "banner_orange";
            case 12 -> "banner_magenta";
            case 13 -> "banner_light_blue";
            case 14 -> "banner_yellow";
            case 15 -> "banner_lime";
            case 16 -> "banner_pink";
            case 17 -> "banner_gray";
            case 18 -> "banner_light_gray";
            case 19 -> "banner_cyan";
            case 20 -> "banner_purple";
            case 21 -> "banner_blue";
            case 22 -> "banner_brown";
            case 23 -> "banner_green";
            case 24 -> "banner_red";
            case 25 -> "banner_black";
            case 26 -> "red_x";
            case 27 -> "village_desert";
            case 28 -> "village_plains";
            case 29 -> "village_savanna";
            case 30 -> "village_snowy";
            case 31 -> "village_taiga";
            case 32 -> "jungle_temple";
            case 33 -> "swamp_hut";
        };
    }

    private static void fixPotionContents(ItemStackData itemStack, Dynamic<?> dynamic) {
        Dynamic<?> component = dynamic.emptyMap();
        Optional<String> potion = itemStack.removeTag("Potion").asString().result().filter(id -> !id.equals("minecraft:empty"));
        if (potion.isPresent()) {
            component = component.set("potion", dynamic.createString(potion.get()));
        }
        component = itemStack.moveTagInto("CustomPotionColor", component, "custom_color");
        if (!(component = itemStack.moveTagInto("custom_potion_effects", component, "custom_effects")).equals((Object)dynamic.emptyMap())) {
            itemStack.setComponent("minecraft:potion_contents", component);
        }
    }

    private static void fixWritableBook(ItemStackData itemStack, Dynamic<?> dynamic) {
        Dynamic<?> pages = ItemStackComponentizationFix.fixBookPages(itemStack, dynamic);
        if (pages != null) {
            itemStack.setComponent("minecraft:writable_book_content", dynamic.emptyMap().set("pages", pages));
        }
    }

    private static void fixWrittenBook(ItemStackData itemStack, Dynamic<?> dynamic) {
        Dynamic<?> pages = ItemStackComponentizationFix.fixBookPages(itemStack, dynamic);
        String title = itemStack.removeTag("title").asString("");
        Optional filteredTitle = itemStack.removeTag("filtered_title").asString().result();
        Dynamic component = dynamic.emptyMap();
        component = component.set("title", ItemStackComponentizationFix.createFilteredText(dynamic, title, filteredTitle));
        component = itemStack.moveTagInto("author", component, "author");
        component = itemStack.moveTagInto("resolved", component, "resolved");
        component = itemStack.moveTagInto("generation", component, "generation");
        if (pages != null) {
            component = component.set("pages", pages);
        }
        itemStack.setComponent("minecraft:written_book_content", component);
    }

    private static @Nullable Dynamic<?> fixBookPages(ItemStackData itemStack, Dynamic<?> dynamic) {
        List pages = itemStack.removeTag("pages").asList(page -> page.asString(""));
        Map filteredPages = itemStack.removeTag("filtered_pages").asMap(key -> key.asString("0"), page -> page.asString(""));
        if (pages.isEmpty()) {
            return null;
        }
        ArrayList fixedPages = new ArrayList(pages.size());
        for (int i = 0; i < pages.size(); ++i) {
            String page2 = (String)pages.get(i);
            String filteredPage = (String)filteredPages.get(String.valueOf(i));
            fixedPages.add(ItemStackComponentizationFix.createFilteredText(dynamic, page2, Optional.ofNullable(filteredPage)));
        }
        return dynamic.createList(fixedPages.stream());
    }

    private static Dynamic<?> createFilteredText(Dynamic<?> dynamic, String text, Optional<String> filtered) {
        Dynamic fixedPage = dynamic.emptyMap().set("raw", dynamic.createString(text));
        if (filtered.isPresent()) {
            fixedPage = fixedPage.set("filtered", dynamic.createString(filtered.get()));
        }
        return fixedPage;
    }

    private static void fixBucketedMobData(ItemStackData itemStack, Dynamic<?> dynamic) {
        Dynamic<?> data = dynamic.emptyMap();
        for (String key : BUCKETED_MOB_TAGS) {
            data = itemStack.moveTagInto(key, data, key);
        }
        if (!data.equals((Object)dynamic.emptyMap())) {
            itemStack.setComponent("minecraft:bucket_entity_data", data);
        }
    }

    private static void fixLodestoneTracker(ItemStackData itemStack, Dynamic<?> dynamic) {
        Optional lodestonePos = itemStack.removeTag("LodestonePos").result();
        Optional lodestoneDimension = itemStack.removeTag("LodestoneDimension").result();
        if (lodestonePos.isEmpty() && lodestoneDimension.isEmpty()) {
            return;
        }
        boolean lodestoneTracked = itemStack.removeTag("LodestoneTracked").asBoolean(true);
        Dynamic component = dynamic.emptyMap();
        if (lodestonePos.isPresent() && lodestoneDimension.isPresent()) {
            component = component.set("target", dynamic.emptyMap().set("pos", (Dynamic)lodestonePos.get()).set("dimension", (Dynamic)lodestoneDimension.get()));
        }
        if (!lodestoneTracked) {
            component = component.set("tracked", dynamic.createBoolean(false));
        }
        itemStack.setComponent("minecraft:lodestone_tracker", component);
    }

    private static void fixFireworkStar(ItemStackData itemStack) {
        itemStack.fixSubTag("Explosion", true, explosion -> {
            itemStack.setComponent("minecraft:firework_explosion", ItemStackComponentizationFix.fixFireworkExplosion(explosion));
            return explosion.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
        });
    }

    private static void fixFireworkRocket(ItemStackData itemStack) {
        itemStack.fixSubTag("Fireworks", true, fireworks -> {
            Stream<Dynamic> explosions = fireworks.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
            int flight = fireworks.get("Flight").asInt(0);
            itemStack.setComponent("minecraft:fireworks", fireworks.emptyMap().set("explosions", fireworks.createList(explosions)).set("flight_duration", fireworks.createByte((byte)flight)));
            return fireworks.remove("Explosions").remove("Flight");
        });
    }

    private static Dynamic<?> fixFireworkExplosion(Dynamic<?> explosion) {
        explosion = explosion.set("shape", explosion.createString(switch (explosion.get("Type").asInt(0)) {
            default -> "small_ball";
            case 1 -> "large_ball";
            case 2 -> "star";
            case 3 -> "creeper";
            case 4 -> "burst";
        })).remove("Type");
        explosion = explosion.renameField("Colors", "colors");
        explosion = explosion.renameField("FadeColors", "fade_colors");
        explosion = explosion.renameField("Trail", "has_trail");
        explosion = explosion.renameField("Flicker", "has_twinkle");
        return explosion;
    }

    public static Dynamic<?> fixProfile(Dynamic<?> dynamic) {
        Optional simpleName = dynamic.asString().result();
        if (simpleName.isPresent()) {
            if (ItemStackComponentizationFix.isValidPlayerName((String)simpleName.get())) {
                return dynamic.emptyMap().set("name", dynamic.createString((String)simpleName.get()));
            }
            return dynamic.emptyMap();
        }
        String name = dynamic.get("Name").asString("");
        Optional id = dynamic.get("Id").result();
        Dynamic<?> properties = ItemStackComponentizationFix.fixProfileProperties(dynamic.get("Properties"));
        Dynamic profile = dynamic.emptyMap();
        if (ItemStackComponentizationFix.isValidPlayerName(name)) {
            profile = profile.set("name", dynamic.createString(name));
        }
        if (id.isPresent()) {
            profile = profile.set("id", (Dynamic)id.get());
        }
        if (properties != null) {
            profile = profile.set("properties", properties);
        }
        return profile;
    }

    private static boolean isValidPlayerName(String name) {
        if (name.length() > 16) {
            return false;
        }
        return name.chars().filter(c -> c <= 32 || c >= 127).findAny().isEmpty();
    }

    private static @Nullable Dynamic<?> fixProfileProperties(OptionalDynamic<?> dynamic) {
        Map properties = dynamic.asMap(key -> key.asString(""), list -> list.asList(property -> {
            String value = property.get("Value").asString("");
            Optional signature = property.get("Signature").asString().result();
            return Pair.of((Object)value, (Object)signature);
        }));
        if (properties.isEmpty()) {
            return null;
        }
        return dynamic.createList(properties.entrySet().stream().flatMap(entry -> ((List)entry.getValue()).stream().map(pair -> {
            Dynamic property = dynamic.emptyMap().set("name", dynamic.createString((String)entry.getKey())).set("value", dynamic.createString((String)pair.getFirst()));
            Optional signature = (Optional)pair.getSecond();
            if (signature.isPresent()) {
                return property.set("signature", dynamic.createString((String)signature.get()));
            }
            return property;
        })));
    }

    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead("ItemStack componentization", this.getInputSchema().getType(References.ITEM_STACK), this.getOutputSchema().getType(References.ITEM_STACK), dynamic -> {
            Optional<Dynamic> fixedItemStack = ItemStackData.read(dynamic).map(itemStack -> {
                ItemStackComponentizationFix.fixItemStack(itemStack, itemStack.tag);
                return itemStack.write();
            });
            return (Dynamic)DataFixUtils.orElse(fixedItemStack, (Object)dynamic);
        });
    }

    private static class ItemStackData {
        private final String item;
        private final int count;
        private Dynamic<?> components;
        private final Dynamic<?> remainder;
        private Dynamic<?> tag;

        private ItemStackData(String item, int count, Dynamic<?> remainder) {
            this.item = NamespacedSchema.ensureNamespaced(item);
            this.count = count;
            this.components = remainder.emptyMap();
            this.tag = remainder.get("tag").orElseEmptyMap();
            this.remainder = remainder.remove("tag");
        }

        public static Optional<ItemStackData> read(Dynamic<?> dynamic) {
            return dynamic.get("id").asString().apply2stable((item, count) -> new ItemStackData((String)item, count.intValue(), (Dynamic<?>)dynamic.remove("id").remove("Count")), dynamic.get("Count").asNumber()).result();
        }

        public OptionalDynamic<?> removeTag(String key) {
            OptionalDynamic value = this.tag.get(key);
            this.tag = this.tag.remove(key);
            return value;
        }

        public void setComponent(String type, Dynamic<?> value) {
            this.components = this.components.set(type, value);
        }

        public void setComponent(String type, OptionalDynamic<?> optionalValue) {
            optionalValue.result().ifPresent(value -> {
                this.components = this.components.set(type, value);
            });
        }

        public Dynamic<?> moveTagInto(String fromKey, Dynamic<?> target, String toKey) {
            Optional value = this.removeTag(fromKey).result();
            if (value.isPresent()) {
                return target.set(toKey, (Dynamic)value.get());
            }
            return target;
        }

        public void moveTagToComponent(String key, String type, Dynamic<?> defaultValue) {
            Optional value = this.removeTag(key).result();
            if (value.isPresent() && !((Dynamic)value.get()).equals(defaultValue)) {
                this.setComponent(type, (Dynamic)value.get());
            }
        }

        public void moveTagToComponent(String key, String type) {
            this.removeTag(key).result().ifPresent(value -> this.setComponent(type, (Dynamic<?>)value));
        }

        public void fixSubTag(String key, boolean dontFixWhenFieldIsMissing, UnaryOperator<Dynamic<?>> function) {
            OptionalDynamic value = this.tag.get(key);
            if (dontFixWhenFieldIsMissing && value.result().isEmpty()) {
                return;
            }
            Dynamic map = value.orElseEmptyMap();
            this.tag = (map = (Dynamic)function.apply(map)).equals((Object)map.emptyMap()) ? this.tag.remove(key) : this.tag.set(key, map);
        }

        public Dynamic<?> write() {
            Dynamic result = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
            if (!this.tag.equals((Object)this.tag.emptyMap())) {
                this.components = this.components.set("minecraft:custom_data", this.tag);
            }
            if (!this.components.equals((Object)this.tag.emptyMap())) {
                result = result.set("components", this.components);
            }
            return ItemStackData.mergeRemainder(result, this.remainder);
        }

        private static <T> Dynamic<T> mergeRemainder(Dynamic<T> itemStack, Dynamic<?> remainder) {
            DynamicOps ops = itemStack.getOps();
            return ops.getMap(itemStack.getValue()).flatMap(itemStackMap -> ops.mergeToMap(remainder.convert(ops).getValue(), itemStackMap)).map(merged -> new Dynamic(ops, merged)).result().orElse(itemStack);
        }

        public boolean is(String id) {
            return this.item.equals(id);
        }

        public boolean is(Set<String> ids) {
            return ids.contains(this.item);
        }

        public boolean hasComponent(String id) {
            return this.components.get(id).result().isPresent();
        }
    }
}

