/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public enum BooleanModifier implements AttributeModifier<Boolean, Boolean>
{
    AND,
    NAND,
    OR,
    NOR,
    XOR,
    XNOR;


    @Override
    public Boolean apply(Boolean subject, Boolean argument) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> argument != false && subject != false;
            case 1 -> argument == false || subject == false;
            case 2 -> argument != false || subject != false;
            case 3 -> argument == false && subject == false;
            case 4 -> argument ^ subject;
            case 5 -> argument == subject;
        };
    }

    @Override
    public Codec<Boolean> argumentCodec(EnvironmentAttribute<Boolean> type) {
        return Codec.BOOL;
    }

    @Override
    public LerpFunction<Boolean> argumentKeyframeLerp(EnvironmentAttribute<Boolean> type) {
        return LerpFunction.ofConstant();
    }
}

