/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.Hook$HookFunction
 *  com.mojang.datafixers.types.templates.TypeTemplate
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class V1451_6
extends NamespacedSchema {
    public static final String SPECIAL_OBJECTIVE_MARKER = "_special";
    protected static final Hook.HookFunction UNPACK_OBJECTIVE_ID = new Hook.HookFunction(){

        public <T> T apply(DynamicOps<T> ops, T value) {
            Dynamic input = new Dynamic(ops, value);
            return (T)((Dynamic)DataFixUtils.orElse(input.get("CriteriaName").asString().result().map(name -> {
                int colonPos = name.indexOf(58);
                if (colonPos < 0) {
                    return Pair.of((Object)V1451_6.SPECIAL_OBJECTIVE_MARKER, (Object)name);
                }
                try {
                    Identifier statType = Identifier.bySeparator(name.substring(0, colonPos), '.');
                    Identifier statId = Identifier.bySeparator(name.substring(colonPos + 1), '.');
                    return Pair.of((Object)statType.toString(), (Object)statId.toString());
                }
                catch (Exception e) {
                    return Pair.of((Object)V1451_6.SPECIAL_OBJECTIVE_MARKER, (Object)name);
                }
            }).map(explodedId -> input.set("CriteriaType", input.createMap((Map)ImmutableMap.of((Object)input.createString("type"), (Object)input.createString((String)explodedId.getFirst()), (Object)input.createString("id"), (Object)input.createString((String)explodedId.getSecond()))))), (Object)input)).getValue();
        }
    };
    protected static final Hook.HookFunction REPACK_OBJECTIVE_ID = new Hook.HookFunction(){

        public <T> T apply(DynamicOps<T> ops, T value) {
            Dynamic input = new Dynamic(ops, value);
            Optional<Dynamic> repackedId = input.get("CriteriaType").get().result().flatMap(type -> {
                Optional statType = type.get("type").asString().result();
                Optional statId = type.get("id").asString().result();
                if (statType.isPresent() && statId.isPresent()) {
                    String unpackedType = (String)statType.get();
                    if (unpackedType.equals(V1451_6.SPECIAL_OBJECTIVE_MARKER)) {
                        return Optional.of(input.createString((String)statId.get()));
                    }
                    return Optional.of(type.createString(V1451_6.packNamespacedWithDot(unpackedType) + ":" + V1451_6.packNamespacedWithDot((String)statId.get())));
                }
                return Optional.empty();
            });
            return (T)((Dynamic)DataFixUtils.orElse(repackedId.map(id -> input.set("CriteriaName", id).remove("CriteriaType")), (Object)input)).getValue();
        }
    };

    public V1451_6(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
        super.registerTypes(schema, entityTypes, blockEntityTypes);
        Supplier<TypeTemplate> ITEM_STATS = () -> DSL.compoundList((TypeTemplate)References.ITEM_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()));
        schema.registerType(false, References.STATS, () -> DSL.optionalFields((String)"stats", (TypeTemplate)DSL.optionalFields((Pair[])new Pair[]{Pair.of((Object)"minecraft:mined", (Object)DSL.compoundList((TypeTemplate)References.BLOCK_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:crafted", (Object)((TypeTemplate)ITEM_STATS.get())), Pair.of((Object)"minecraft:used", (Object)((TypeTemplate)ITEM_STATS.get())), Pair.of((Object)"minecraft:broken", (Object)((TypeTemplate)ITEM_STATS.get())), Pair.of((Object)"minecraft:picked_up", (Object)((TypeTemplate)ITEM_STATS.get())), Pair.of((Object)"minecraft:dropped", (Object)((TypeTemplate)ITEM_STATS.get())), Pair.of((Object)"minecraft:killed", (Object)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:killed_by", (Object)DSL.compoundList((TypeTemplate)References.ENTITY_NAME.in(schema), (TypeTemplate)DSL.constType((Type)DSL.intType()))), Pair.of((Object)"minecraft:custom", (Object)DSL.compoundList((TypeTemplate)DSL.constType(V1451_6.namespacedString()), (TypeTemplate)DSL.constType((Type)DSL.intType())))})));
        Map<String, Supplier<TypeTemplate>> criterionTypes = V1451_6.createCriterionTypes(schema);
        schema.registerType(false, References.OBJECTIVE, () -> DSL.hook((TypeTemplate)DSL.optionalFields((String)"CriteriaType", (TypeTemplate)DSL.taggedChoiceLazy((String)"type", (Type)DSL.string(), (Map)criterionTypes), (String)"DisplayName", (TypeTemplate)References.TEXT_COMPONENT.in(schema)), (Hook.HookFunction)UNPACK_OBJECTIVE_ID, (Hook.HookFunction)REPACK_OBJECTIVE_ID));
    }

    protected static Map<String, Supplier<TypeTemplate>> createCriterionTypes(Schema schema) {
        Supplier<TypeTemplate> itemCriterion = () -> DSL.optionalFields((String)"id", (TypeTemplate)References.ITEM_NAME.in(schema));
        Supplier<TypeTemplate> blockCriterion = () -> DSL.optionalFields((String)"id", (TypeTemplate)References.BLOCK_NAME.in(schema));
        Supplier<TypeTemplate> entityCriterion = () -> DSL.optionalFields((String)"id", (TypeTemplate)References.ENTITY_NAME.in(schema));
        HashMap criterionTypes = Maps.newHashMap();
        criterionTypes.put("minecraft:mined", blockCriterion);
        criterionTypes.put("minecraft:crafted", itemCriterion);
        criterionTypes.put("minecraft:used", itemCriterion);
        criterionTypes.put("minecraft:broken", itemCriterion);
        criterionTypes.put("minecraft:picked_up", itemCriterion);
        criterionTypes.put("minecraft:dropped", itemCriterion);
        criterionTypes.put("minecraft:killed", entityCriterion);
        criterionTypes.put("minecraft:killed_by", entityCriterion);
        criterionTypes.put("minecraft:custom", () -> DSL.optionalFields((String)"id", (TypeTemplate)DSL.constType(V1451_6.namespacedString())));
        criterionTypes.put(SPECIAL_OBJECTIVE_MARKER, () -> DSL.optionalFields((String)"id", (TypeTemplate)DSL.constType((Type)DSL.string())));
        return criterionTypes;
    }

    public static String packNamespacedWithDot(String location) {
        Identifier parsedLoc = Identifier.tryParse(location);
        return parsedLoc != null ? parsedLoc.getNamespace() + "." + parsedLoc.getPath() : location;
    }
}

