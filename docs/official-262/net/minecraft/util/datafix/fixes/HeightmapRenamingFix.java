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
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class HeightmapRenamingFix
extends DataFix {
    public HeightmapRenamingFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type inputType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder levelF = inputType.findField("Level");
        return this.fixTypeEverywhereTyped("HeightmapRenamingFix", inputType, input -> input.updateTyped(levelF, level -> level.update(DSL.remainderFinder(), this::fix)));
    }

    private Dynamic<?> fix(Dynamic<?> tag) {
        Optional rain;
        Optional light;
        Optional solid;
        Optional heightmaps = tag.get("Heightmaps").result();
        if (heightmaps.isEmpty()) {
            return tag;
        }
        Dynamic heightmapsTag = (Dynamic)heightmaps.get();
        Optional liquid = heightmapsTag.get("LIQUID").result();
        if (liquid.isPresent()) {
            heightmapsTag = heightmapsTag.remove("LIQUID");
            heightmapsTag = heightmapsTag.set("WORLD_SURFACE_WG", (Dynamic)liquid.get());
        }
        if ((solid = heightmapsTag.get("SOLID").result()).isPresent()) {
            heightmapsTag = heightmapsTag.remove("SOLID");
            heightmapsTag = heightmapsTag.set("OCEAN_FLOOR_WG", (Dynamic)solid.get());
            heightmapsTag = heightmapsTag.set("OCEAN_FLOOR", (Dynamic)solid.get());
        }
        if ((light = heightmapsTag.get("LIGHT").result()).isPresent()) {
            heightmapsTag = heightmapsTag.remove("LIGHT");
            heightmapsTag = heightmapsTag.set("LIGHT_BLOCKING", (Dynamic)light.get());
        }
        if ((rain = heightmapsTag.get("RAIN").result()).isPresent()) {
            heightmapsTag = heightmapsTag.remove("RAIN");
            heightmapsTag = heightmapsTag.set("MOTION_BLOCKING", (Dynamic)rain.get());
            heightmapsTag = heightmapsTag.set("MOTION_BLOCKING_NO_LEAVES", (Dynamic)rain.get());
        }
        return tag.set("Heightmaps", heightmapsTag);
    }
}

