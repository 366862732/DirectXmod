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

public class EntitySkeletonSplitFix
extends SimpleEntityRenameFix {
    public EntitySkeletonSplitFix(Schema outputSchema, boolean changesType) {
        super("EntitySkeletonSplitFix", outputSchema, changesType);
    }

    @Override
    protected Pair<String, Dynamic<?>> getNewNameAndTag(String name, Dynamic<?> tag) {
        if (Objects.equals(name, "Skeleton")) {
            int type = tag.get("SkeletonType").asInt(0);
            if (type == 1) {
                name = "WitherSkeleton";
            } else if (type == 2) {
                name = "Stray";
            }
        }
        return Pair.of((Object)name, tag);
    }
}

