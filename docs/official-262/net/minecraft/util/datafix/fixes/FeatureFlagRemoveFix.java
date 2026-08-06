/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.util.datafix.fixes.References;

public class FeatureFlagRemoveFix
extends DataFix {
    private final String name;
    private final Set<String> flagsToRemove;

    public FeatureFlagRemoveFix(Schema outputSchema, String name, Set<String> flagsToRemove) {
        super(outputSchema, false);
        this.name = name;
        this.flagsToRemove = flagsToRemove;
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(this.name, this.getInputSchema().getType(References.LIGHTWEIGHT_LEVEL), input -> input.update(DSL.remainderFinder(), this::fixTag));
    }

    private <T> Dynamic<T> fixTag(Dynamic<T> tag) {
        List inactiveFeatures = tag.get("removed_features").asStream().collect(Collectors.toCollection(ArrayList::new));
        Dynamic result = tag.update("enabled_features", features -> (Dynamic)DataFixUtils.orElse(features.asStreamOpt().result().map(s -> s.filter(feature -> {
            Optional asString = feature.asString().result();
            if (asString.isEmpty()) {
                return true;
            }
            boolean shouldRemove = this.flagsToRemove.contains(asString.get());
            if (shouldRemove) {
                inactiveFeatures.add(tag.createString((String)asString.get()));
            }
            return !shouldRemove;
        })).map(arg_0 -> ((Dynamic)tag).createList(arg_0)), (Object)features));
        if (!inactiveFeatures.isEmpty()) {
            result = result.set("removed_features", tag.createList(inactiveFeatures.stream()));
        }
        return result;
    }
}

