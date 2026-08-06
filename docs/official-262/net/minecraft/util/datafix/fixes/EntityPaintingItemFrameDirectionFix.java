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
import net.minecraft.util.datafix.fixes.References;

public class EntityPaintingItemFrameDirectionFix
extends DataFix {
    private static final int[][] DIRECTIONS = new int[][]{{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

    public EntityPaintingItemFrameDirectionFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    private Dynamic<?> doFix(Dynamic<?> input, boolean isPainting, boolean isItemFrame) {
        if ((isPainting || isItemFrame) && input.get("Facing").asNumber().result().isEmpty()) {
            int direction;
            if (input.get("Direction").asNumber().result().isPresent()) {
                direction = input.get("Direction").asByte((byte)0) % DIRECTIONS.length;
                int[] steps = DIRECTIONS[direction];
                input = input.set("TileX", input.createInt(input.get("TileX").asInt(0) + steps[0]));
                input = input.set("TileY", input.createInt(input.get("TileY").asInt(0) + steps[1]));
                input = input.set("TileZ", input.createInt(input.get("TileZ").asInt(0) + steps[2]));
                input = input.remove("Direction");
                if (isItemFrame && input.get("ItemRotation").asNumber().result().isPresent()) {
                    input = input.set("ItemRotation", input.createByte((byte)(input.get("ItemRotation").asByte((byte)0) * 2)));
                }
            } else {
                direction = input.get("Dir").asByte((byte)0) % DIRECTIONS.length;
                input = input.remove("Dir");
            }
            input = input.set("Facing", input.createByte((byte)direction));
        }
        return input;
    }

    public TypeRewriteRule makeRule() {
        Type paintingType = this.getInputSchema().getChoiceType(References.ENTITY, "Painting");
        OpticFinder paintingF = DSL.namedChoice((String)"Painting", (Type)paintingType);
        Type itemFrameType = this.getInputSchema().getChoiceType(References.ENTITY, "ItemFrame");
        OpticFinder itemFrameF = DSL.namedChoice((String)"ItemFrame", (Type)itemFrameType);
        Type entityType = this.getInputSchema().getType(References.ENTITY);
        TypeRewriteRule paintingRule = this.fixTypeEverywhereTyped("EntityPaintingFix", entityType, input -> input.updateTyped(paintingF, paintingType, entity -> entity.update(DSL.remainderFinder(), tag -> this.doFix((Dynamic<?>)tag, true, false))));
        TypeRewriteRule itemFrameRule = this.fixTypeEverywhereTyped("EntityItemFrameFix", entityType, input -> input.updateTyped(itemFrameF, itemFrameType, entity -> entity.update(DSL.remainderFinder(), tag -> this.doFix((Dynamic<?>)tag, false, true))));
        return TypeRewriteRule.seq((TypeRewriteRule)paintingRule, (TypeRewriteRule)itemFrameRule);
    }
}

