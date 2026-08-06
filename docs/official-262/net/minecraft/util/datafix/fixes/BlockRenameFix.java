/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class BlockRenameFix
extends DataFix {
    private final String name;

    public BlockRenameFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    public TypeRewriteRule makeRule() {
        Type expectedType;
        Type blockType = this.getInputSchema().getType(References.BLOCK_NAME);
        if (!Objects.equals(blockType, expectedType = DSL.named((String)References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString()))) {
            throw new IllegalStateException("block type is not what was expected.");
        }
        TypeRewriteRule blockRule = this.fixTypeEverywhere(this.name + " for block", expectedType, ops -> input -> input.mapSecond(this::renameBlock));
        TypeRewriteRule blockStateRule = this.fixTypeEverywhereTyped(this.name + " for block_state", this.getInputSchema().getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), this::fixBlockState));
        TypeRewriteRule flatBlockStateRule = this.fixTypeEverywhereTyped(this.name + " for flat_block_state", this.getInputSchema().getType(References.FLAT_BLOCK_STATE), input -> input.update(DSL.remainderFinder(), tag -> (Dynamic)DataFixUtils.orElse(tag.asString().result().map(this::fixFlatBlockState).map(arg_0 -> ((Dynamic)tag).createString(arg_0)), (Object)tag)));
        return TypeRewriteRule.seq((TypeRewriteRule)blockRule, (TypeRewriteRule[])new TypeRewriteRule[]{blockStateRule, flatBlockStateRule});
    }

    private Dynamic<?> fixBlockState(Dynamic<?> tag) {
        Optional name = tag.get("Name").asString().result();
        if (name.isPresent()) {
            return tag.set("Name", tag.createString(this.renameBlock((String)name.get())));
        }
        return tag;
    }

    private String fixFlatBlockState(String string) {
        int startProperties = string.indexOf(91);
        int startNbt = string.indexOf(123);
        int end = string.length();
        if (startProperties > 0) {
            end = startProperties;
        }
        if (startNbt > 0) {
            end = Math.min(end, startNbt);
        }
        String name = string.substring(0, end);
        String newName = this.renameBlock(name);
        return newName + string.substring(end);
    }

    protected abstract String renameBlock(String var1);

    public static DataFix create(Schema outputSchema, String name, final Function<String, String> renamer) {
        return new BlockRenameFix(outputSchema, name){

            @Override
            protected String renameBlock(String block) {
                return (String)renamer.apply(block);
            }
        };
    }
}

