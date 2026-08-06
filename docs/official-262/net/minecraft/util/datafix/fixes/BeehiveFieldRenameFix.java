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
 *  com.mojang.datafixers.types.templates.List$ListType
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class BeehiveFieldRenameFix
extends DataFix {
    public BeehiveFieldRenameFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    private Dynamic<?> fixBeehive(Dynamic<?> beehive) {
        return beehive.remove("Bees");
    }

    private Dynamic<?> fixBee(Dynamic<?> bee) {
        bee = bee.remove("EntityData");
        bee = bee.renameField("TicksInHive", "ticks_in_hive");
        bee = bee.renameField("MinOccupationTicks", "min_ticks_in_hive");
        return bee;
    }

    public TypeRewriteRule makeRule() {
        Type beehiveType = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:beehive");
        OpticFinder beehiveF = DSL.namedChoice((String)"minecraft:beehive", (Type)beehiveType);
        List.ListType beesType = (List.ListType)beehiveType.findFieldType("Bees");
        Type beeType = beesType.getElement();
        OpticFinder beesF = DSL.fieldFinder((String)"Bees", (Type)beesType);
        OpticFinder beeF = DSL.typeFinder((Type)beeType);
        Type entityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type newEntityType = this.getOutputSchema().getType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhereTyped("BeehiveFieldRenameFix", entityType, newEntityType, input -> ExtraDataFixUtils.cast(newEntityType, input.updateTyped(beehiveF, beehive -> beehive.update(DSL.remainderFinder(), this::fixBeehive).updateTyped(beesF, bees -> bees.updateTyped(beeF, bee -> bee.update(DSL.remainderFinder(), this::fixBee))))));
    }
}

