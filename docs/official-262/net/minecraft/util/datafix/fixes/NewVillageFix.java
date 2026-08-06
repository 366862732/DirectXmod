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
 *  com.mojang.datafixers.types.templates.CompoundList$CompoundListType
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class NewVillageFix
extends DataFix {
    public NewVillageFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        CompoundList.CompoundListType startsType = DSL.compoundList((Type)DSL.string(), (Type)this.getInputSchema().getType(References.STRUCTURE_FEATURE));
        OpticFinder finder = startsType.finder();
        return this.cap(startsType);
    }

    private <SF> TypeRewriteRule cap(CompoundList.CompoundListType<String, SF> startsType) {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        Type structureType = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
        OpticFinder levelFinder = chunkType.findField("Level");
        OpticFinder structuresFinder = levelFinder.type().findField("Structures");
        OpticFinder startsFinder = structuresFinder.type().findField("Starts");
        OpticFinder listFinder = startsType.finder();
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped("NewVillageFix", chunkType, input -> input.updateTyped(levelFinder, level -> level.updateTyped(structuresFinder, structures -> structures.updateTyped(startsFinder, starts -> starts.update(listFinder, list -> list.stream().filter(pair -> !Objects.equals(pair.getFirst(), "Village")).map(pair -> pair.mapFirst(name -> name.equals("New_Village") ? "Village" : name)).collect(Collectors.toList()))).update(DSL.remainderFinder(), tag -> tag.update("References", references -> {
            Optional village = references.get("New_Village").result();
            return ((Dynamic)DataFixUtils.orElse(village.map(v -> references.remove("New_Village").set("Village", v)), (Object)references)).remove("Village");
        }))))), (TypeRewriteRule)this.fixTypeEverywhereTyped("NewVillageStartFix", structureType, input -> input.update(DSL.remainderFinder(), tag -> tag.update("id", id -> Objects.equals(NamespacedSchema.ensureNamespaced(id.asString("")), "minecraft:new_village") ? id.createString("minecraft:village") : id))));
    }
}

