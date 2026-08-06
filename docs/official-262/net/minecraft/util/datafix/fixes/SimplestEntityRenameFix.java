/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TaggedChoice$TaggedChoiceType
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class SimplestEntityRenameFix
extends DataFix {
    private final String name;

    public SimplestEntityRenameFix(String name, Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoice.TaggedChoiceType oldType = this.getInputSchema().findChoiceType(References.ENTITY);
        TaggedChoice.TaggedChoiceType newType = this.getOutputSchema().findChoiceType(References.ENTITY);
        Type entityNameType = DSL.named((String)References.ENTITY_NAME.typeName(), NamespacedSchema.namespacedString());
        if (!Objects.equals(this.getOutputSchema().getType(References.ENTITY_NAME), entityNameType)) {
            throw new IllegalStateException("Entity name type is not what was expected.");
        }
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhere(this.name, (Type)oldType, (Type)newType, ops -> input -> input.mapFirst(oldName -> {
            String newName = this.rename((String)oldName);
            Type oldEntityType = (Type)oldType.types().get(oldName);
            Type newEntityType = (Type)newType.types().get(newName);
            if (!newEntityType.equals((Object)oldEntityType, true, true)) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", newEntityType, oldEntityType));
            }
            return newName;
        })), (TypeRewriteRule)this.fixTypeEverywhere(this.name + " for entity name", entityNameType, ops -> input -> input.mapSecond(this::rename)));
    }

    protected abstract String rename(String var1);
}

