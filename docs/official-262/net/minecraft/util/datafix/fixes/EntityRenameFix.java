/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.TaggedChoice$TaggedChoiceType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public abstract class EntityRenameFix
extends DataFix {
    protected final String name;

    public EntityRenameFix(String name, Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        TaggedChoice.TaggedChoiceType oldType = this.getInputSchema().findChoiceType(References.ENTITY);
        TaggedChoice.TaggedChoiceType newType = this.getOutputSchema().findChoiceType(References.ENTITY);
        Function<String, Type> patchedInputTypes = Util.memoize(name -> {
            Type type = (Type)oldType.types().get(name);
            return ExtraDataFixUtils.patchSubType(type, oldType, newType);
        });
        return this.fixTypeEverywhere(this.name, (Type)oldType, (Type)newType, ops -> input -> {
            String oldName = (String)input.getFirst();
            Type oldEntityType = (Type)patchedInputTypes.apply(oldName);
            Pair<String, Typed<?>> newEntity = this.fix(oldName, this.getEntity(input.getSecond(), (DynamicOps<?>)ops, (Type)oldEntityType));
            Type expectedType = (Type)newType.types().get(newEntity.getFirst());
            if (!expectedType.equals((Object)((Typed)newEntity.getSecond()).getType(), true, true)) {
                throw new IllegalStateException(String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", expectedType, ((Typed)newEntity.getSecond()).getType()));
            }
            return Pair.of((Object)((String)newEntity.getFirst()), (Object)((Typed)newEntity.getSecond()).getValue());
        });
    }

    private <A> Typed<A> getEntity(Object input, DynamicOps<?> ops, Type<A> oldEntityType) {
        return new Typed(oldEntityType, ops, input);
    }

    protected abstract Pair<String, Typed<?>> fix(String var1, Typed<?> var2);
}

