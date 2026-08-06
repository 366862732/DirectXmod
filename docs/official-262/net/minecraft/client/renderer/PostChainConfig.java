/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArraySet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.UniformValue;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

@Environment(value=EnvType.CLIENT)
public record PostChainConfig(Map<Identifier, InternalTarget> internalTargets, List<Pass> passes) {
    public static final Codec<PostChainConfig> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.unboundedMap(Identifier.CODEC, InternalTarget.CODEC).optionalFieldOf("targets", Map.of()).forGetter(PostChainConfig::internalTargets), (App)Pass.CODEC.listOf().optionalFieldOf("passes", List.of()).forGetter(PostChainConfig::passes)).apply((Applicative)i, PostChainConfig::new));

    @Environment(value=EnvType.CLIENT)
    public record InternalTarget(Optional<Integer> width, Optional<Integer> height, boolean persistent, int clearColor) {
        public static final Codec<InternalTarget> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("width").forGetter(InternalTarget::width), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("height").forGetter(InternalTarget::height), (App)Codec.BOOL.optionalFieldOf("persistent", (Object)false).forGetter(InternalTarget::persistent), (App)ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("clear_color", (Object)0).forGetter(InternalTarget::clearColor)).apply((Applicative)i, InternalTarget::new));
    }

    @Environment(value=EnvType.CLIENT)
    public record Pass(Identifier vertexShaderId, Identifier fragmentShaderId, List<Input> inputs, Identifier outputTarget, Map<String, List<UniformValue>> uniforms) {
        private static final Codec<List<Input>> INPUTS_CODEC = Input.CODEC.listOf().validate(inputs -> {
            ObjectArraySet samplerName = new ObjectArraySet(inputs.size());
            for (Input input : inputs) {
                if (samplerName.add(input.samplerName())) continue;
                return DataResult.error(() -> "Encountered repeated sampler name: " + input.samplerName());
            }
            return DataResult.success((Object)inputs);
        });
        private static final Codec<Map<String, List<UniformValue>>> UNIFORM_BLOCKS_CODEC = Codec.unboundedMap((Codec)Codec.STRING, (Codec)UniformValue.CODEC.listOf());
        public static final Codec<Pass> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("vertex_shader").forGetter(Pass::vertexShaderId), (App)Identifier.CODEC.fieldOf("fragment_shader").forGetter(Pass::fragmentShaderId), (App)INPUTS_CODEC.optionalFieldOf("inputs", List.of()).forGetter(Pass::inputs), (App)Identifier.CODEC.fieldOf("output").forGetter(Pass::outputTarget), (App)UNIFORM_BLOCKS_CODEC.optionalFieldOf("uniforms", Map.of()).forGetter(Pass::uniforms)).apply((Applicative)i, Pass::new));

        public Stream<Identifier> referencedTargets() {
            Stream inputTargets = this.inputs.stream().flatMap(input -> input.referencedTargets().stream());
            return Stream.concat(inputTargets, Stream.of(this.outputTarget));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record TargetInput(String samplerName, Identifier targetId, boolean useDepthBuffer, boolean bilinear) implements Input
    {
        public static final Codec<TargetInput> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("sampler_name").forGetter(TargetInput::samplerName), (App)Identifier.CODEC.fieldOf("target").forGetter(TargetInput::targetId), (App)Codec.BOOL.optionalFieldOf("use_depth_buffer", (Object)false).forGetter(TargetInput::useDepthBuffer), (App)Codec.BOOL.optionalFieldOf("bilinear", (Object)false).forGetter(TargetInput::bilinear)).apply((Applicative)i, TargetInput::new));

        @Override
        public Set<Identifier> referencedTargets() {
            return Set.of(this.targetId);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record TextureInput(String samplerName, Identifier location, int width, int height, boolean bilinear) implements Input
    {
        public static final Codec<TextureInput> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("sampler_name").forGetter(TextureInput::samplerName), (App)Identifier.CODEC.fieldOf("location").forGetter(TextureInput::location), (App)ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(TextureInput::width), (App)ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(TextureInput::height), (App)Codec.BOOL.optionalFieldOf("bilinear", (Object)false).forGetter(TextureInput::bilinear)).apply((Applicative)i, TextureInput::new));

        @Override
        public Set<Identifier> referencedTargets() {
            return Set.of();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static sealed interface Input
    permits TargetInput, TextureInput {
        public static final Codec<Input> CODEC = Codec.xor(TextureInput.CODEC, TargetInput.CODEC).xmap(either -> (Input)either.map(Function.identity(), Function.identity()), input -> {
            Input input2 = input;
            Objects.requireNonNull(input2);
            Input selector0$temp = input2;
            int index$1 = 0;
            return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{TextureInput.class, TargetInput.class}, (Input)selector0$temp, index$1)) {
                default -> throw new MatchException(null, null);
                case 0 -> {
                    TextureInput texture = (TextureInput)selector0$temp;
                    yield Either.left((Object)texture);
                }
                case 1 -> {
                    TargetInput target = (TargetInput)selector0$temp;
                    yield Either.right((Object)target);
                }
            };
        });

        public String samplerName();

        public Set<Identifier> referencedTargets();
    }
}

