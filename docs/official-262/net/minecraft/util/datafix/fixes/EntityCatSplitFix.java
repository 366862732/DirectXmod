/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.util.datafix.fixes.SimpleEntityRenameFix;

public class EntityCatSplitFix
extends SimpleEntityRenameFix {
    public EntityCatSplitFix(Schema outputSchema, boolean changesType) {
        super("EntityCatSplitFix", outputSchema, changesType);
    }

    @Override
    protected Pair<String, Dynamic<?>> getNewNameAndTag(String name, Dynamic<?> tag) {
        if (Objects.equals("minecraft:ocelot", name)) {
            int type = tag.get("CatType").asInt(0);
            if (type == 0) {
                String ownerName = tag.get("Owner").asString("");
                String ownerUUID = tag.get("OwnerUUID").asString("");
                if (!ownerName.isEmpty() || !ownerUUID.isEmpty()) {
                    tag.set("Trusting", tag.createBoolean(true));
                }
            } else if (type > 0 && type < 4) {
                tag = tag.set("CatType", tag.createInt(type));
                tag = tag.set("OwnerUUID", tag.createString(tag.get("OwnerUUID").asString("")));
                return Pair.of((Object)"minecraft:cat", (Object)tag);
            }
        }
        return Pair.of((Object)name, tag);
    }
}

