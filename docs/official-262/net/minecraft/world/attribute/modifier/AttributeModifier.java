/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.BooleanModifier;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.attribute.modifier.IntegerModifier;

public interface AttributeModifier<Subject, Argument> {
    public static final Map<OperationId, AttributeModifier<Boolean, ?>> BOOLEAN_LIBRARY = Map.of(OperationId.AND, BooleanModifier.AND, OperationId.NAND, BooleanModifier.NAND, OperationId.OR, BooleanModifier.OR, OperationId.NOR, BooleanModifier.NOR, OperationId.XOR, BooleanModifier.XOR, OperationId.XNOR, BooleanModifier.XNOR);
    public static final Map<OperationId, AttributeModifier<Float, ?>> FLOAT_LIBRARY = Map.of(OperationId.ALPHA_BLEND, FloatModifier.ALPHA_BLEND, OperationId.ADD, FloatModifier.ADD, OperationId.SUBTRACT, FloatModifier.SUBTRACT, OperationId.MULTIPLY, FloatModifier.MULTIPLY, OperationId.MINIMUM, FloatModifier.MINIMUM, OperationId.MAXIMUM, FloatModifier.MAXIMUM);
    public static final Map<OperationId, AttributeModifier<Integer, ?>> RGB_COLOR_LIBRARY = Map.of(OperationId.ALPHA_BLEND, ColorModifier.ALPHA_BLEND, OperationId.ADD, ColorModifier.ADD, OperationId.SUBTRACT, ColorModifier.SUBTRACT, OperationId.MULTIPLY, ColorModifier.MULTIPLY_RGB, OperationId.BLEND_TO_GRAY, ColorModifier.BLEND_TO_GRAY);
    public static final Map<OperationId, AttributeModifier<Integer, ?>> ARGB_COLOR_LIBRARY = Map.of(OperationId.ALPHA_BLEND, ColorModifier.ALPHA_BLEND, OperationId.ADD, ColorModifier.ADD, OperationId.SUBTRACT, ColorModifier.SUBTRACT, OperationId.MULTIPLY, ColorModifier.MULTIPLY_ARGB, OperationId.BLEND_TO_GRAY, ColorModifier.BLEND_TO_GRAY);
    public static final Map<OperationId, AttributeModifier<Integer, ?>> INTEGER_LIBRARY = Map.of(OperationId.ADD, IntegerModifier.ADD, OperationId.SUBTRACT, IntegerModifier.SUBTRACT, OperationId.MULTIPLY, IntegerModifier.MULTIPLY, OperationId.MINIMUM, IntegerModifier.MINIMUM, OperationId.MAXIMUM, IntegerModifier.MAXIMUM);

    public static <Value> AttributeModifier<Value, Value> override() {
        return OverrideModifier.INSTANCE;
    }

    public Subject apply(Subject var1, Argument var2);

    public Codec<Argument> argumentCodec(EnvironmentAttribute<Subject> var1);

    public LerpFunction<Argument> argumentKeyframeLerp(EnvironmentAttribute<Subject> var1);

    public record OverrideModifier<Value>() implements AttributeModifier<Value, Value>
    {
        private static final OverrideModifier<?> INSTANCE = new OverrideModifier();

        @Override
        public Value apply(Value subject, Value argument) {
            return argument;
        }

        @Override
        public Codec<Value> argumentCodec(EnvironmentAttribute<Value> attribute) {
            return attribute.valueCodec();
        }

        @Override
        public LerpFunction<Value> argumentKeyframeLerp(EnvironmentAttribute<Value> attribute) {
            return attribute.type().keyframeLerp();
        }
    }

    public static enum OperationId implements StringRepresentable
    {
        OVERRIDE("override"),
        ALPHA_BLEND("alpha_blend"),
        ADD("add"),
        SUBTRACT("subtract"),
        MULTIPLY("multiply"),
        BLEND_TO_GRAY("blend_to_gray"),
        MINIMUM("minimum"),
        MAXIMUM("maximum"),
        AND("and"),
        NAND("nand"),
        OR("or"),
        NOR("nor"),
        XOR("xor"),
        XNOR("xnor");

        public static final Codec<OperationId> CODEC;
        private final String name;

        private OperationId(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(OperationId::values);
        }
    }
}

