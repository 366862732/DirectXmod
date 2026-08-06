/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.FieldFinder
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.CompoundList$CompoundListType
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.datafixers.util.Unit
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FieldFinder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.WorldGenSettingsFix;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MissingDimensionFix
extends DataFix {
    public MissingDimensionFix(Schema schema, boolean changesType) {
        super(schema, changesType);
    }

    protected static <A> Type<Pair<A, Dynamic<?>>> fields(String name, Type<A> type) {
        return DSL.and((Type)DSL.field((String)name, type), (Type)DSL.remainderType());
    }

    protected static <A> Type<Pair<Either<A, Unit>, Dynamic<?>>> optionalFields(String name, Type<A> type) {
        return DSL.and((Type)DSL.optional((Type)DSL.field((String)name, type)), (Type)DSL.remainderType());
    }

    protected static <A1, A2> Type<Pair<Either<A1, Unit>, Pair<Either<A2, Unit>, Dynamic<?>>>> optionalFields(String name1, Type<A1> type1, String name2, Type<A2> type2) {
        return DSL.and((Type)DSL.optional((Type)DSL.field((String)name1, type1)), (Type)DSL.optional((Type)DSL.field((String)name2, type2)), (Type)DSL.remainderType());
    }

    protected TypeRewriteRule makeRule() {
        Schema schema = this.getInputSchema();
        Type generatorType = DSL.taggedChoiceType((String)"type", (Type)DSL.string(), (Map)ImmutableMap.of((Object)"minecraft:debug", (Object)DSL.remainderType(), (Object)"minecraft:flat", MissingDimensionFix.flatType(schema), (Object)"minecraft:noise", MissingDimensionFix.optionalFields("biome_source", DSL.taggedChoiceType((String)"type", (Type)DSL.string(), (Map)ImmutableMap.of((Object)"minecraft:fixed", MissingDimensionFix.fields("biome", schema.getType(References.BIOME)), (Object)"minecraft:multi_noise", (Object)DSL.list(MissingDimensionFix.fields("biome", schema.getType(References.BIOME))), (Object)"minecraft:checkerboard", MissingDimensionFix.fields("biomes", DSL.list((Type)schema.getType(References.BIOME))), (Object)"minecraft:vanilla_layered", (Object)DSL.remainderType(), (Object)"minecraft:the_end", (Object)DSL.remainderType())), "settings", DSL.or((Type)DSL.string(), MissingDimensionFix.optionalFields("default_block", schema.getType(References.BLOCK_NAME), "default_fluid", schema.getType(References.BLOCK_NAME))))));
        CompoundList.CompoundListType dimensionsType = DSL.compoundList(NamespacedSchema.namespacedString(), MissingDimensionFix.fields("generator", generatorType));
        Type expectedDimensionsType = DSL.and((Type)dimensionsType, (Type)DSL.remainderType());
        Type settings = schema.getType(References.WORLD_GEN_SETTINGS);
        FieldFinder dimensionsFinder = new FieldFinder("dimensions", expectedDimensionsType);
        if (!settings.findFieldType("dimensions").equals((Object)expectedDimensionsType)) {
            throw new IllegalStateException();
        }
        OpticFinder dimensionListFinder = dimensionsType.finder();
        return this.fixTypeEverywhereTyped("MissingDimensionFix", settings, input -> input.updateTyped((OpticFinder)dimensionsFinder, dimensions -> dimensions.updateTyped(dimensionListFinder, generators -> {
            if (!(generators.getValue() instanceof List)) {
                throw new IllegalStateException("List exptected");
            }
            if (((List)generators.getValue()).isEmpty()) {
                Dynamic tag = (Dynamic)input.get(DSL.remainderFinder());
                Dynamic newDimensions = this.recreateSettings(tag);
                return (Typed)DataFixUtils.orElse(dimensionsType.readTyped(newDimensions).result().map(Pair::getFirst), (Object)generators);
            }
            return generators;
        })));
    }

    protected static Type<? extends Pair<? extends Either<? extends Pair<? extends Either<?, Unit>, ? extends Pair<? extends Either<? extends List<? extends Pair<? extends Either<?, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>> flatType(Schema schema) {
        return MissingDimensionFix.optionalFields("settings", MissingDimensionFix.optionalFields("biome", schema.getType(References.BIOME), "layers", DSL.list(MissingDimensionFix.optionalFields("block", schema.getType(References.BLOCK_NAME)))));
    }

    private <T> Dynamic<T> recreateSettings(Dynamic<T> tag) {
        long seed = tag.get("seed").asLong(0L);
        return new Dynamic(tag.getOps(), WorldGenSettingsFix.vanillaLevels(tag, seed, WorldGenSettingsFix.defaultOverworld(tag, seed), false));
    }
}

