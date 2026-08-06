/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.References;

public class TextComponentStringifiedFlagsFix
extends DataFix {
    public TextComponentStringifiedFlagsFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type textComponentType = this.getInputSchema().getType(References.TEXT_COMPONENT);
        return this.fixTypeEverywhere("TextComponentStringyFlagsFix", textComponentType, ops -> named -> named.mapSecond(e1 -> e1.mapRight(p2 -> p2.mapSecond(p3 -> p3.mapSecond(p4 -> p4.mapSecond(remainder -> remainder.update("bold", TextComponentStringifiedFlagsFix::stringToBool).update("italic", TextComponentStringifiedFlagsFix::stringToBool).update("underlined", TextComponentStringifiedFlagsFix::stringToBool).update("strikethrough", TextComponentStringifiedFlagsFix::stringToBool).update("obfuscated", TextComponentStringifiedFlagsFix::stringToBool)))))));
    }

    private static <T> Dynamic<T> stringToBool(Dynamic<T> input) {
        Optional string = input.asString().result();
        if (string.isPresent()) {
            return input.createBoolean(Boolean.parseBoolean((String)string.get()));
        }
        return input;
    }
}

