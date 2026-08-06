/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.fixes.AttributesRenameFix;

public class RenameNameplateToNameTagFix
extends AttributesRenameFix {
    public RenameNameplateToNameTagFix(Schema outputSchema) {
        super(outputSchema, "RenameNameplateToNameTag", RenameNameplateToNameTagFix::rename);
    }

    private static String rename(String oldName) {
        if (oldName.equals("minecraft:nameplate_distance")) {
            return "minecraft:name_tag_distance";
        }
        return oldName;
    }
}

