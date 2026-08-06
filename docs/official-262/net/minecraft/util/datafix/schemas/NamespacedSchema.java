/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.Const$PrimitiveType
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.codecs.PrimitiveCodec
 */
package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Const;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.resources.Identifier;

public class NamespacedSchema
extends Schema {
    public static final PrimitiveCodec<String> NAMESPACED_STRING_CODEC = new PrimitiveCodec<String>(){

        public <T> DataResult<String> read(DynamicOps<T> ops, T input) {
            return ops.getStringValue(input).map(NamespacedSchema::ensureNamespaced);
        }

        public <T> T write(DynamicOps<T> ops, String value) {
            return (T)ops.createString(value);
        }

        public String toString() {
            return "NamespacedString";
        }
    };
    private static final Type<String> NAMESPACED_STRING = new Const.PrimitiveType(NAMESPACED_STRING_CODEC);

    public NamespacedSchema(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static String ensureNamespaced(String input) {
        Identifier identifier = Identifier.tryParse(input);
        if (identifier != null) {
            return identifier.toString();
        }
        return input;
    }

    public static Type<String> namespacedString() {
        return NAMESPACED_STRING;
    }

    public Type<?> getChoiceType(DSL.TypeReference type, String choiceName) {
        return super.getChoiceType(type, NamespacedSchema.ensureNamespaced(choiceName));
    }
}

