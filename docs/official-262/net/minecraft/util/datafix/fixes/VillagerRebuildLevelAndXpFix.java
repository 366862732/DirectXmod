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
 *  com.mojang.datafixers.types.templates.List$ListType
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
import com.mojang.datafixers.types.templates.List;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.fixes.References;

public class VillagerRebuildLevelAndXpFix
extends DataFix {
    private static final int TRADES_PER_LEVEL = 2;
    private static final int[] LEVEL_XP_THRESHOLDS = new int[]{0, 10, 50, 100, 150};

    public static int getMinXpPerLevel(int level) {
        return LEVEL_XP_THRESHOLDS[Mth.clamp(level - 1, 0, LEVEL_XP_THRESHOLDS.length - 1)];
    }

    public VillagerRebuildLevelAndXpFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Type villagerType = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:villager");
        OpticFinder entityF = DSL.namedChoice((String)"minecraft:villager", (Type)villagerType);
        OpticFinder offersF = villagerType.findField("Offers");
        Type offersType = offersF.type();
        OpticFinder recipeListF = offersType.findField("Recipes");
        List.ListType recipeListType = (List.ListType)recipeListF.type();
        OpticFinder recipeF = recipeListType.getElement().finder();
        return this.fixTypeEverywhereTyped("Villager level and xp rebuild", this.getInputSchema().getType(References.ENTITY), input -> input.updateTyped(entityF, villagerType, villager -> {
            Optional xp;
            int offerCount;
            Dynamic remainder = (Dynamic)villager.get(DSL.remainderFinder());
            int level = remainder.get("VillagerData").get("level").asInt(0);
            Typed<?> modifiedVillager = villager;
            if ((level == 0 || level == 1) && (level = Mth.clamp((offerCount = villager.getOptionalTyped(offersF).flatMap(o -> o.getOptionalTyped(recipeListF)).map(recipeList -> recipeList.getAllTyped(recipeF).size()).orElse(0).intValue()) / 2, 1, 5)) > 1) {
                modifiedVillager = VillagerRebuildLevelAndXpFix.addLevel(modifiedVillager, level);
            }
            if ((xp = remainder.get("Xp").asNumber().result()).isEmpty()) {
                modifiedVillager = VillagerRebuildLevelAndXpFix.addXpFromLevel(modifiedVillager, level);
            }
            return modifiedVillager;
        }));
    }

    private static Typed<?> addLevel(Typed<?> villager, int level) {
        return villager.update(DSL.remainderFinder(), remainder -> remainder.update("VillagerData", villagerData -> villagerData.set("level", villagerData.createInt(level))));
    }

    private static Typed<?> addXpFromLevel(Typed<?> villager, int level) {
        int xp = VillagerRebuildLevelAndXpFix.getMinXpPerLevel(level);
        return villager.update(DSL.remainderFinder(), remainder -> remainder.set("Xp", remainder.createInt(xp)));
    }
}

