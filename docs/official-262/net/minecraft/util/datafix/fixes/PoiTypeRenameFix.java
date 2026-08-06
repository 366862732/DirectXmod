/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.AbstractPoiSectionFix;

public class PoiTypeRenameFix
extends AbstractPoiSectionFix {
    private final Function<String, String> renamer;

    public PoiTypeRenameFix(Schema outputSchema, String name, Function<String, String> renamer) {
        super(outputSchema, name);
        this.renamer = renamer;
    }

    @Override
    protected <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> stream) {
        return stream.map(element -> element.update("type", type -> (Dynamic)DataFixUtils.orElse((Optional)type.asString().map(this.renamer).map(arg_0 -> ((Dynamic)type).createString(arg_0)).result(), (Object)type)));
    }
}

