/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.MapDecoder
 *  com.mojang.serialization.MapEncoder
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;

public class ComponentSerialization {
    public static final Codec<Component> CODEC = Codec.recursive((String)"Component", ComponentSerialization::createCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(ByteBufCodecs::optional);
    public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);

    public static Codec<Component> flatRestrictedCodec(final int maxFlatSize) {
        return new Codec<Component>(){

            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                return CODEC.decode(ops, input).flatMap(pair -> {
                    if (this.isTooLarge(ops, (Component)pair.getFirst())) {
                        return DataResult.error(() -> "Component was too large: greater than max size " + maxFlatSize);
                    }
                    return DataResult.success((Object)pair);
                });
            }

            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                return CODEC.encodeStart(ops, (Object)input);
            }

            private <T> boolean isTooLarge(DynamicOps<T> ops, Component input) {
                DataResult json = CODEC.encodeStart(1.asJsonOps(ops), (Object)input);
                return json.isSuccess() && GsonHelper.encodesLongerThan((JsonElement)json.getOrThrow(), maxFlatSize);
            }

            private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> ops) {
                if (ops instanceof RegistryOps) {
                    RegistryOps registryOps = (RegistryOps)ops;
                    return registryOps.withParent(JsonOps.INSTANCE);
                }
                return JsonOps.INSTANCE;
            }
        };
    }

    private static MutableComponent createFromList(List<Component> list) {
        MutableComponent result = list.get(0).copy();
        for (int i = 1; i < list.size(); ++i) {
            result.append(list.get(i));
        }
        return result;
    }

    public static <T> MapCodec<T> createLegacyComponentMatcher(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends T>> types, Function<T, MapCodec<? extends T>> codecGetter, String typeFieldName) {
        FuzzyCodec<? extends T> compactCodec = new FuzzyCodec<T>(types.values(), codecGetter);
        MapCodec discriminatorCodec = types.codec((Codec<String>)Codec.STRING).dispatchMap(typeFieldName, codecGetter, c -> c);
        StrictEither<? extends T> contentsCodec = new StrictEither<T>(typeFieldName, discriminatorCodec, compactCodec);
        return ExtraCodecs.orCompressed(contentsCodec, discriminatorCodec);
    }

    private static Codec<Component> createCodec(Codec<Component> topSerializer) {
        ExtraCodecs.LateBoundIdMapper contentTypes = new ExtraCodecs.LateBoundIdMapper();
        ComponentSerialization.bootstrap(contentTypes);
        MapCodec<ComponentContents> compressedContentsCodec = ComponentSerialization.createLegacyComponentMatcher(contentTypes, ComponentContents::codec, "type");
        Codec fullCodec = RecordCodecBuilder.create(i -> i.group((App)compressedContentsCodec.forGetter(Component::getContents), (App)ExtraCodecs.nonEmptyList(topSerializer.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings), (App)Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)).apply((Applicative)i, MutableComponent::new));
        return Codec.either((Codec)Codec.either((Codec)Codec.STRING, ExtraCodecs.nonEmptyList(topSerializer.listOf())), (Codec)fullCodec).xmap(specialOrComponent -> (Component)specialOrComponent.map(special -> (Component)special.map(Component::literal, ComponentSerialization::createFromList), c -> c), component -> {
            String text = component.tryCollapseToString();
            return text != null ? Either.left((Object)Either.left((Object)text)) : Either.right((Object)component);
        });
    }

    private static void bootstrap(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> contentTypes) {
        contentTypes.put("text", PlainTextContents.MAP_CODEC);
        contentTypes.put("translatable", TranslatableContents.MAP_CODEC);
        contentTypes.put("keybind", KeybindContents.MAP_CODEC);
        contentTypes.put("score", ScoreContents.MAP_CODEC);
        contentTypes.put("selector", SelectorContents.MAP_CODEC);
        contentTypes.put("nbt", NbtContents.MAP_CODEC);
        contentTypes.put("object", ObjectContents.MAP_CODEC);
    }

    private static class FuzzyCodec<T>
    extends MapCodec<T> {
        private final Collection<MapCodec<? extends T>> codecs;
        private final Function<T, ? extends MapEncoder<? extends T>> encoderGetter;

        public FuzzyCodec(Collection<MapCodec<? extends T>> codecs, Function<T, ? extends MapEncoder<? extends T>> encoderGetter) {
            this.codecs = codecs;
            this.encoderGetter = encoderGetter;
        }

        public <S> DataResult<T> decode(DynamicOps<S> ops, MapLike<S> input) {
            for (MapDecoder mapDecoder : this.codecs) {
                DataResult result = mapDecoder.decode(ops, input);
                if (!result.result().isPresent()) continue;
                return result;
            }
            return DataResult.error(() -> "No matching codec found");
        }

        public <S> RecordBuilder<S> encode(T input, DynamicOps<S> ops, RecordBuilder<S> prefix) {
            MapEncoder<? extends T> encoder = this.encoderGetter.apply(input);
            return encoder.encode(input, ops, prefix);
        }

        public <S> Stream<S> keys(DynamicOps<S> ops) {
            return this.codecs.stream().flatMap(c -> c.keys(ops)).distinct();
        }

        public String toString() {
            return "FuzzyCodec[" + String.valueOf(this.codecs) + "]";
        }
    }

    private static class StrictEither<T>
    extends MapCodec<T> {
        private final String typeFieldName;
        private final MapCodec<T> typed;
        private final MapCodec<T> fuzzy;

        public StrictEither(String typeFieldName, MapCodec<T> typed, MapCodec<T> fuzzy) {
            this.typeFieldName = typeFieldName;
            this.typed = typed;
            this.fuzzy = fuzzy;
        }

        public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
            if (input.get(this.typeFieldName) != null) {
                return this.typed.decode(ops, input);
            }
            return this.fuzzy.decode(ops, input);
        }

        public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            return this.fuzzy.encode(input, ops, prefix);
        }

        public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
            return Stream.concat(this.typed.keys(ops), this.fuzzy.keys(ops)).distinct();
        }
    }
}

