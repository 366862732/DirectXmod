/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class ItemStackEnchantmentNamesFix
extends DataFix {
    private static final Int2ObjectMap<String> MAP = (Int2ObjectMap)DataFixUtils.make((Object)new Int2ObjectOpenHashMap(), map -> {
        map.put(0, (Object)"minecraft:protection");
        map.put(1, (Object)"minecraft:fire_protection");
        map.put(2, (Object)"minecraft:feather_falling");
        map.put(3, (Object)"minecraft:blast_protection");
        map.put(4, (Object)"minecraft:projectile_protection");
        map.put(5, (Object)"minecraft:respiration");
        map.put(6, (Object)"minecraft:aqua_affinity");
        map.put(7, (Object)"minecraft:thorns");
        map.put(8, (Object)"minecraft:depth_strider");
        map.put(9, (Object)"minecraft:frost_walker");
        map.put(10, (Object)"minecraft:binding_curse");
        map.put(16, (Object)"minecraft:sharpness");
        map.put(17, (Object)"minecraft:smite");
        map.put(18, (Object)"minecraft:bane_of_arthropods");
        map.put(19, (Object)"minecraft:knockback");
        map.put(20, (Object)"minecraft:fire_aspect");
        map.put(21, (Object)"minecraft:looting");
        map.put(22, (Object)"minecraft:sweeping");
        map.put(32, (Object)"minecraft:efficiency");
        map.put(33, (Object)"minecraft:silk_touch");
        map.put(34, (Object)"minecraft:unbreaking");
        map.put(35, (Object)"minecraft:fortune");
        map.put(48, (Object)"minecraft:power");
        map.put(49, (Object)"minecraft:punch");
        map.put(50, (Object)"minecraft:flame");
        map.put(51, (Object)"minecraft:infinity");
        map.put(61, (Object)"minecraft:luck_of_the_sea");
        map.put(62, (Object)"minecraft:lure");
        map.put(65, (Object)"minecraft:loyalty");
        map.put(66, (Object)"minecraft:impaling");
        map.put(67, (Object)"minecraft:riptide");
        map.put(68, (Object)"minecraft:channeling");
        map.put(70, (Object)"minecraft:mending");
        map.put(71, (Object)"minecraft:vanishing_curse");
    });

    public ItemStackEnchantmentNamesFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type item = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder tagFinder = item.findField("tag");
        return this.fixTypeEverywhereTyped("ItemStackEnchantmentFix", item, input -> input.updateTyped(tagFinder, tag -> tag.update(DSL.remainderFinder(), this::fixTag)));
    }

    private Dynamic<?> fixTag(Dynamic<?> tag) {
        Optional newEnch = tag.get("ench").asStreamOpt().map(s -> s.map(element -> element.set("id", element.createString((String)MAP.getOrDefault(element.get("id").asInt(0), (Object)"null"))))).map(arg_0 -> tag.createList(arg_0)).result();
        if (newEnch.isPresent()) {
            tag = tag.remove("ench").set("Enchantments", (Dynamic)newEnch.get());
        }
        return tag.update("StoredEnchantments", list -> (Dynamic)DataFixUtils.orElse((Optional)list.asStreamOpt().map(l -> l.map(enchant -> enchant.set("id", enchant.createString((String)MAP.getOrDefault(enchant.get("id").asInt(0), (Object)"null"))))).map(arg_0 -> ((Dynamic)list).createList(arg_0)).result(), (Object)list));
    }
}

