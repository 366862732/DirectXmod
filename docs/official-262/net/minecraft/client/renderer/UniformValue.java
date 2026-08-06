/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.joml.Vector2fc
 *  org.joml.Vector3fc
 *  org.joml.Vector3ic
 *  org.joml.Vector4fc
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public interface UniformValue {
    public static final Codec<UniformValue> CODEC = Type.CODEC.dispatch(UniformValue::type, t -> t.valueCodec);

    public void writeTo(Std140Builder var1);

    public void addSize(Std140SizeCalculator var1);

    public Type type();

    @Environment(value=EnvType.CLIENT)
    public static enum Type implements StringRepresentable
    {
        INT("int", IntUniform.CODEC),
        IVEC3("ivec3", IVec3Uniform.CODEC),
        FLOAT("float", FloatUniform.CODEC),
        VEC2("vec2", Vec2Uniform.CODEC),
        VEC3("vec3", Vec3Uniform.CODEC),
        VEC4("vec4", Vec4Uniform.CODEC),
        MATRIX4X4("matrix4x4", Matrix4x4Uniform.CODEC);

        public static final Codec<Type> CODEC;
        private final String name;
        private final MapCodec<? extends UniformValue> valueCodec;

        private Type(String name, Codec<? extends UniformValue> valueCodec) {
            this.name = name;
            this.valueCodec = valueCodec.fieldOf("value");
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Matrix4x4Uniform(Matrix4fc value) implements UniformValue
    {
        public static final Codec<Matrix4x4Uniform> CODEC = ExtraCodecs.MATRIX4F.xmap(Matrix4x4Uniform::new, Matrix4x4Uniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putMat4f(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putMat4f();
        }

        @Override
        public Type type() {
            return Type.MATRIX4X4;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Vec4Uniform(Vector4fc value) implements UniformValue
    {
        public static final Codec<Vec4Uniform> CODEC = ExtraCodecs.VECTOR4F.xmap(Vec4Uniform::new, Vec4Uniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putVec4(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putVec4();
        }

        @Override
        public Type type() {
            return Type.VEC4;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Vec3Uniform(Vector3fc value) implements UniformValue
    {
        public static final Codec<Vec3Uniform> CODEC = ExtraCodecs.VECTOR3F.xmap(Vec3Uniform::new, Vec3Uniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putVec3(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putVec3();
        }

        @Override
        public Type type() {
            return Type.VEC3;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Vec2Uniform(Vector2fc value) implements UniformValue
    {
        public static final Codec<Vec2Uniform> CODEC = ExtraCodecs.VECTOR2F.xmap(Vec2Uniform::new, Vec2Uniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putVec2(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putVec2();
        }

        @Override
        public Type type() {
            return Type.VEC2;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record FloatUniform(float value) implements UniformValue
    {
        public static final Codec<FloatUniform> CODEC = Codec.FLOAT.xmap(FloatUniform::new, FloatUniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putFloat(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putFloat();
        }

        @Override
        public Type type() {
            return Type.FLOAT;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record IVec3Uniform(Vector3ic value) implements UniformValue
    {
        public static final Codec<IVec3Uniform> CODEC = ExtraCodecs.VECTOR3I.xmap(IVec3Uniform::new, IVec3Uniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putIVec3(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putIVec3();
        }

        @Override
        public Type type() {
            return Type.IVEC3;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record IntUniform(int value) implements UniformValue
    {
        public static final Codec<IntUniform> CODEC = Codec.INT.xmap(IntUniform::new, IntUniform::value);

        @Override
        public void writeTo(Std140Builder builder) {
            builder.putInt(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator calculator) {
            calculator.putInt();
        }

        @Override
        public Type type() {
            return Type.INT;
        }
    }
}

