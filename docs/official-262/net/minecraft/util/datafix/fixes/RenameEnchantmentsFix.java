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
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RenameEnchantmentsFix
extends DataFix {
    private final String name;
    private final Map<String, String> renames;

    public RenameEnchantmentsFix(Schema outputSchema, String name, Map<String, String> renames) {
        super(outputSchema, false);
        this.name = name;
        this.renames = renames;
    }

    protected TypeRewriteRule makeRule() {
        Type item = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder tagFinder = item.findField("tag");
        return this.fixTypeEverywhereTyped(this.name, item, input -> input.updateTyped(tagFinder, tag -> tag.update(DSL.remainderFinder(), this::fixTag)));
    }

    private Dynamic<?> fixTag(Dynamic<?> tag) {
        tag = this.fixEnchantmentList(tag, "Enchantments");
        tag = this.fixEnchantmentList(tag, "StoredEnchantments");
        return tag;
    }

    private Dynamic<?> fixEnchantmentList(Dynamic<?> itemStack, String field) {
        return itemStack.update(field, tag -> (Dynamic)tag.asStreamOpt().map(s -> s.map(element -> element.update("id", id -> (Dynamic)id.asString().map(stringId -> element.createString(this.renames.getOrDefault(NamespacedSchema.ensureNamespaced(stringId), (String)stringId))).mapOrElse(Function.identity(), fail -> id)))).map(arg_0 -> ((Dynamic)tag).createList(arg_0)).mapOrElse(Function.identity(), fail -> tag));
    }
}

