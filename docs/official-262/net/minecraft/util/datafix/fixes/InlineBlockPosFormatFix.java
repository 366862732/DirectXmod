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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class InlineBlockPosFormatFix
extends DataFix {
    public InlineBlockPosFormatFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        OpticFinder<?> vexFinder = this.entityFinder("minecraft:vex");
        OpticFinder<?> phantomFinder = this.entityFinder("minecraft:phantom");
        OpticFinder<?> turtleFinder = this.entityFinder("minecraft:turtle");
        List<OpticFinder<?>> blockAttachedFinders = List.of(this.entityFinder("minecraft:item_frame"), this.entityFinder("minecraft:glow_item_frame"), this.entityFinder("minecraft:painting"), this.entityFinder("minecraft:leash_knot"));
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhereTyped("InlineBlockPosFormatFix - player", this.getInputSchema().getType(References.PLAYER), player -> player.update(DSL.remainderFinder(), this::fixPlayer)), (TypeRewriteRule)this.fixTypeEverywhereTyped("InlineBlockPosFormatFix - entity", this.getInputSchema().getType(References.ENTITY), entity -> {
            entity = entity.update(DSL.remainderFinder(), this::fixLivingEntity).updateTyped(vexFinder, vex -> vex.update(DSL.remainderFinder(), this::fixVex)).updateTyped(phantomFinder, phantom -> phantom.update(DSL.remainderFinder(), this::fixPhantom)).updateTyped(turtleFinder, turtle -> turtle.update(DSL.remainderFinder(), this::fixTurtle));
            for (OpticFinder blockAttachedFinder : blockAttachedFinders) {
                entity = entity.updateTyped(blockAttachedFinder, blockAttached -> blockAttached.update(DSL.remainderFinder(), this::fixBlockAttached));
            }
            return entity;
        }));
    }

    private OpticFinder<?> entityFinder(String choiceName) {
        return DSL.namedChoice((String)choiceName, (Type)this.getInputSchema().getChoiceType(References.ENTITY, choiceName));
    }

    private Dynamic<?> fixPlayer(Dynamic<?> tag) {
        Optional enteredNetherPos;
        tag = this.fixLivingEntity(tag);
        Optional spawnX = tag.get("SpawnX").asNumber().result();
        Optional spawnY = tag.get("SpawnY").asNumber().result();
        Optional spawnZ = tag.get("SpawnZ").asNumber().result();
        if (spawnX.isPresent() && spawnY.isPresent() && spawnZ.isPresent()) {
            Dynamic respawn = tag.createMap(Map.of(tag.createString("pos"), ExtraDataFixUtils.createBlockPos(tag, ((Number)spawnX.get()).intValue(), ((Number)spawnY.get()).intValue(), ((Number)spawnZ.get()).intValue())));
            respawn = Dynamic.copyField(tag, (String)"SpawnAngle", (Dynamic)respawn, (String)"angle");
            respawn = Dynamic.copyField(tag, (String)"SpawnDimension", (Dynamic)respawn, (String)"dimension");
            respawn = Dynamic.copyField(tag, (String)"SpawnForced", (Dynamic)respawn, (String)"forced");
            tag = tag.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
            tag = tag.set("respawn", respawn);
        }
        if ((enteredNetherPos = tag.get("enteredNetherPosition").result()).isPresent()) {
            tag = tag.remove("enteredNetherPosition").set("entered_nether_pos", tag.createList(Stream.of(tag.createDouble(((Dynamic)enteredNetherPos.get()).get("x").asDouble(0.0)), tag.createDouble(((Dynamic)enteredNetherPos.get()).get("y").asDouble(0.0)), tag.createDouble(((Dynamic)enteredNetherPos.get()).get("z").asDouble(0.0)))));
        }
        return tag;
    }

    private Dynamic<?> fixLivingEntity(Dynamic<?> tag) {
        return ExtraDataFixUtils.fixInlineBlockPos(tag, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
    }

    private Dynamic<?> fixVex(Dynamic<?> tag) {
        return ExtraDataFixUtils.fixInlineBlockPos(tag.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
    }

    private Dynamic<?> fixPhantom(Dynamic<?> tag) {
        return ExtraDataFixUtils.fixInlineBlockPos(tag.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
    }

    private Dynamic<?> fixTurtle(Dynamic<?> tag) {
        tag = tag.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
        tag = ExtraDataFixUtils.fixInlineBlockPos(tag, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
        return tag.renameField("HasEgg", "has_egg");
    }

    private Dynamic<?> fixBlockAttached(Dynamic<?> tag) {
        return ExtraDataFixUtils.fixInlineBlockPos(tag, "TileX", "TileY", "TileZ", "block_pos");
    }
}

