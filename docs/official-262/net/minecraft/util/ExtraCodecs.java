/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.HashBiMap
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.ImmutableMultimap
 *  com.google.common.collect.ImmutableMultimap$Builder
 *  com.google.common.collect.Multimap
 *  com.google.common.primitives.UnsignedBytes
 *  com.google.gson.JsonElement
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 *  com.mojang.authlib.properties.PropertyMap
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Codec$ResultFunction
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.Decoder
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JavaOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 *  com.mojang.serialization.codecs.BaseMapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.floats.FloatArrayList
 *  it.unimi.dsi.fastutil.objects.Object2BooleanMap
 *  it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.apache.commons.io.FilenameUtils
 *  org.apache.commons.lang3.StringEscapeUtils
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.joml.AxisAngle4f
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector2f
 *  org.joml.Vector2fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector3i
 *  org.joml.Vector3ic
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public class ExtraCodecs {
    public static final Codec<JsonElement> JSON = ExtraCodecs.converter(JsonOps.INSTANCE);
    public static final Codec<Object> JAVA = ExtraCodecs.converter(JavaOps.INSTANCE);
    public static final Codec<Tag> NBT = ExtraCodecs.converter(NbtOps.INSTANCE);
    public static final Codec<Vector2fc> VECTOR2F = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 2).map(d -> new Vector2f(((Float)d.get(0)).floatValue(), ((Float)d.get(1)).floatValue())), vec -> List.of(Float.valueOf(vec.x()), Float.valueOf(vec.y())));
    public static final Codec<Vector3fc> VECTOR3F = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 3).map(d -> new Vector3f(((Float)d.get(0)).floatValue(), ((Float)d.get(1)).floatValue(), ((Float)d.get(2)).floatValue())), vec -> List.of(Float.valueOf(vec.x()), Float.valueOf(vec.y()), Float.valueOf(vec.z())));
    public static final Codec<Vector3ic> VECTOR3I = Codec.INT.listOf().comapFlatMap(input -> Util.fixedSize(input, 3).map(d -> new Vector3i(((Integer)d.get(0)).intValue(), ((Integer)d.get(1)).intValue(), ((Integer)d.get(2)).intValue())), vec -> List.of(Integer.valueOf(vec.x()), Integer.valueOf(vec.y()), Integer.valueOf(vec.z())));
    public static final Codec<Vector4fc> VECTOR4F = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 4).map(d -> new Vector4f(((Float)d.get(0)).floatValue(), ((Float)d.get(1)).floatValue(), ((Float)d.get(2)).floatValue(), ((Float)d.get(3)).floatValue())), vec -> List.of(Float.valueOf(vec.x()), Float.valueOf(vec.y()), Float.valueOf(vec.z()), Float.valueOf(vec.w())));
    public static final Codec<Quaternionfc> QUATERNIONF_COMPONENTS = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 4).map(d -> new Quaternionf(((Float)d.get(0)).floatValue(), ((Float)d.get(1)).floatValue(), ((Float)d.get(2)).floatValue(), ((Float)d.get(3)).floatValue()).normalize()), q -> List.of(Float.valueOf(q.x()), Float.valueOf(q.y()), Float.valueOf(q.z()), Float.valueOf(q.w())));
    public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(i -> i.group((App)Codec.FLOAT.fieldOf("angle").forGetter(o -> Float.valueOf(o.angle)), (App)VECTOR3F.fieldOf("axis").forGetter(o -> new Vector3f(o.x, o.y, o.z))).apply((Applicative)i, AxisAngle4f::new));
    public static final Codec<Quaternionfc> QUATERNIONF = Codec.withAlternative(QUATERNIONF_COMPONENTS, (Codec)AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
    public static final Codec<Matrix4fc> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 16).map(l -> {
        Matrix4f result = new Matrix4f();
        for (int i = 0; i < l.size(); ++i) {
            result.setRowColumn(i >> 2, i & 3, ((Float)l.get(i)).floatValue());
        }
        return result.determineProperties();
    }), m -> {
        FloatArrayList output = new FloatArrayList(16);
        for (int i = 0; i < 16; ++i) {
            output.add(m.getRowColumn(i >> 2, i & 3));
        }
        return output;
    });
    private static final String HEX_COLOR_PREFIX = "#";
    public static final Codec<Integer> RGB_COLOR_CODEC = Codec.withAlternative((Codec)Codec.INT, VECTOR3F, v -> ARGB.colorFromFloat(1.0f, v.x(), v.y(), v.z()));
    public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.withAlternative((Codec)Codec.INT, VECTOR4F, v -> ARGB.colorFromFloat(v.w(), v.x(), v.y(), v.z()));
    public static final Codec<Integer> STRING_RGB_COLOR = Codec.withAlternative((Codec)ExtraCodecs.hexColor(6).xmap(ARGB::opaque, ARGB::transparent), RGB_COLOR_CODEC);
    public static final Codec<Integer> STRING_ARGB_COLOR = Codec.withAlternative(ExtraCodecs.hexColor(8), ARGB_COLOR_CODEC);
    public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE.flatComapMap(UnsignedBytes::toInt, integer -> {
        if (integer > 255) {
            return DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255");
        }
        return DataResult.success((Object)integer.byteValue());
    });
    public static final Codec<Integer> NON_NEGATIVE_INT = ExtraCodecs.intRangeWithMessage(0, Integer.MAX_VALUE, n -> "Value must be non-negative: " + n);
    public static final Codec<Integer> POSITIVE_INT = ExtraCodecs.intRangeWithMessage(1, Integer.MAX_VALUE, n -> "Value must be positive: " + n);
    public static final Codec<Long> NON_NEGATIVE_LONG = ExtraCodecs.longRangeWithMessage(0L, Long.MAX_VALUE, n -> "Value must be non-negative: " + n);
    public static final Codec<Long> POSITIVE_LONG = ExtraCodecs.longRangeWithMessage(1L, Long.MAX_VALUE, n -> "Value must be positive: " + n);
    public static final Codec<Float> NON_NEGATIVE_FLOAT = ExtraCodecs.floatRangeMinInclusiveWithMessage(0.0f, Float.MAX_VALUE, n -> "Value must be non-negative: " + n);
    public static final Codec<Float> POSITIVE_FLOAT = ExtraCodecs.floatRangeMinExclusiveWithMessage(0.0f, Float.MAX_VALUE, n -> "Value must be positive: " + n);
    public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(pattern -> {
        try {
            return DataResult.success((Object)Pattern.compile(pattern));
        }
        catch (PatternSyntaxException e) {
            return DataResult.error(() -> "Invalid regex pattern '" + pattern + "': " + e.getMessage());
        }
    }, Pattern::pattern);
    public static final Codec<Instant> INSTANT_ISO8601 = ExtraCodecs.temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
    public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(string -> {
        try {
            return DataResult.success((Object)Base64.getDecoder().decode((String)string));
        }
        catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Malformed base64 string");
        }
    }, bytes -> Base64.getEncoder().encodeToString((byte[])bytes));
    public static final Codec<String> ESCAPED_STRING = Codec.STRING.comapFlatMap(str -> DataResult.success((Object)StringEscapeUtils.unescapeJava((String)str)), StringEscapeUtils::escapeJava);
    public static final Codec<TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING.comapFlatMap(name -> name.startsWith(HEX_COLOR_PREFIX) ? Identifier.read(name.substring(1)).map(id -> new TagOrElementLocation((Identifier)id, true)) : Identifier.read(name).map(id -> new TagOrElementLocation((Identifier)id, false)), TagOrElementLocation::decoratedId);
    public static final Function<Optional<Long>, OptionalLong> toOptionalLong = o -> o.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = l -> l.isPresent() ? Optional.of(l.getAsLong()) : Optional.empty();
    public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM.xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> Arrays.stream(bitSet.toLongArray()));
    public static final int MAX_PROPERTY_NAME_LENGTH = 64;
    public static final int MAX_PROPERTY_VALUE_LENGTH = Short.MAX_VALUE;
    public static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
    public static final int MAX_PROPERTIES = 16;
    private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(i -> i.group((App)Codec.sizeLimitedString((int)64).fieldOf("name").forGetter(Property::name), (App)Codec.sizeLimitedString((int)Short.MAX_VALUE).fieldOf("value").forGetter(Property::value), (App)Codec.sizeLimitedString((int)1024).optionalFieldOf("signature").forGetter(property -> Optional.ofNullable(property.signature()))).apply((Applicative)i, (name, value, signature) -> new Property(name, value, (String)signature.orElse(null))));
    public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either((Codec)Codec.unboundedMap((Codec)Codec.STRING, (Codec)Codec.STRING.listOf()).validate(map -> map.size() > 16 ? DataResult.error(() -> "Cannot have more than 16 properties, but was " + map.size()) : DataResult.success((Object)map)), (Codec)PROPERTY.sizeLimitedListOf(16)).xmap(mapListEither -> {
        ImmutableMultimap.Builder result = ImmutableMultimap.builder();
        mapListEither.ifLeft(s -> s.forEach((name, properties) -> {
            for (String property : properties) {
                result.put(name, (Object)new Property(name, property));
            }
        })).ifRight(properties -> {
            for (Property property : properties) {
                result.put((Object)property.name(), (Object)property);
            }
        });
        return new PropertyMap((Multimap)result.build());
    }, propertyMap -> Either.right(propertyMap.values().stream().toList()));
    public static final Codec<String> PLAYER_NAME = Codec.string((int)0, (int)16).validate(name -> {
        if (StringUtil.isValidPlayerName(name)) {
            return DataResult.success((Object)name);
        }
        return DataResult.error(() -> "Player name contained disallowed characters: '" + name + "'");
    });
    public static final Codec<GameProfile> AUTHLIB_GAME_PROFILE = ExtraCodecs.gameProfileCodec(UUIDUtil.AUTHLIB_CODEC).codec();
    public static final MapCodec<GameProfile> STORED_GAME_PROFILE = ExtraCodecs.gameProfileCodec(UUIDUtil.CODEC);
    public static final Codec<String> NON_EMPTY_STRING = Codec.STRING.validate(value -> value.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success((Object)value));
    public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(s -> {
        int[] codepoint = s.codePoints().toArray();
        if (codepoint.length != 1) {
            return DataResult.error(() -> "Expected one codepoint, got: " + s);
        }
        return DataResult.success((Object)codepoint[0]);
    }, Character::toString);
    public static final Codec<String> RESOURCE_PATH_CODEC = Codec.STRING.validate(s -> {
        if (!Identifier.isValidPath(s)) {
            return DataResult.error(() -> "Invalid string to use as a resource path element: " + s);
        }
        return DataResult.success((Object)s);
    });
    public static final Codec<URI> UNTRUSTED_URI = Codec.STRING.comapFlatMap(string -> {
        try {
            return DataResult.success((Object)Util.parseAndValidateUntrustedUri(string));
        }
        catch (URISyntaxException e) {
            return DataResult.error(e::getMessage);
        }
    }, URI::toString);
    public static final Codec<String> CHAT_STRING = Codec.STRING.validate(string -> {
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (StringUtil.isAllowedChatCharacter(c)) continue;
            return DataResult.error(() -> "Disallowed chat character: '" + c + "'");
        }
        return DataResult.success((Object)string);
    });

    public static <T> Codec<T> converter(DynamicOps<T> ops) {
        return Codec.PASSTHROUGH.xmap(t -> t.convert(ops).getValue(), t -> new Dynamic(ops, t));
    }

    private static Codec<Integer> hexColor(int expectedDigits) {
        long maxValue = (1L << expectedDigits * 4) - 1L;
        return Codec.STRING.comapFlatMap(string -> {
            if (!string.startsWith(HEX_COLOR_PREFIX)) {
                return DataResult.error(() -> "Hex color must begin with #");
            }
            int digits = string.length() - HEX_COLOR_PREFIX.length();
            if (digits != expectedDigits) {
                return DataResult.error(() -> "Hex color is wrong size, expected " + expectedDigits + " digits but got " + digits);
            }
            try {
                long value = HexFormat.fromHexDigitsToLong(string, HEX_COLOR_PREFIX.length(), string.length());
                if (value < 0L || value > maxValue) {
                    return DataResult.error(() -> "Color value out of range: " + string);
                }
                return DataResult.success((Object)((int)value));
            }
            catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid color value: " + string);
            }
        }, value -> HEX_COLOR_PREFIX + HexFormat.of().toHexDigits(value.intValue(), expectedDigits));
    }

    public static <P, I> Codec<I> intervalCodec(Codec<P> pointCodec, String lowerBoundName, String upperBoundName, BiFunction<P, P, DataResult<I>> makeInterval, Function<I, P> getMin, Function<I, P> getMax) {
        Codec arrayCodec = Codec.list(pointCodec).comapFlatMap(list -> Util.fixedSize(list, 2).flatMap(l -> {
            Object min = l.get(0);
            Object max = l.get(1);
            return (DataResult)makeInterval.apply(min, max);
        }), p -> ImmutableList.of(getMin.apply(p), getMax.apply(p)));
        Codec objectCodec = RecordCodecBuilder.create(i -> i.group((App)pointCodec.fieldOf(lowerBoundName).forGetter(Pair::getFirst), (App)pointCodec.fieldOf(upperBoundName).forGetter(Pair::getSecond)).apply((Applicative)i, Pair::of)).comapFlatMap(p -> (DataResult)makeInterval.apply(p.getFirst(), p.getSecond()), i -> Pair.of(getMin.apply(i), getMax.apply(i)));
        Codec arrayOrObjectCodec = Codec.withAlternative((Codec)arrayCodec, (Codec)objectCodec);
        return Codec.either(pointCodec, (Codec)arrayOrObjectCodec).comapFlatMap(either -> (DataResult)either.map(min -> (DataResult)makeInterval.apply(min, min), DataResult::success), p -> {
            Object max;
            Object min = getMin.apply(p);
            if (Objects.equals(min, max = getMax.apply(p))) {
                return Either.left(min);
            }
            return Either.right((Object)p);
        });
    }

    public static <A> Codec.ResultFunction<A> orElsePartial(final A value) {
        return new Codec.ResultFunction<A>(){

            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
                MutableObject message = new MutableObject();
                Optional result = a.resultOrPartial(arg_0 -> ((MutableObject)message).setValue(arg_0));
                if (result.isPresent()) {
                    return a;
                }
                return DataResult.error(() -> "(" + (String)message.get() + " -> using default)", (Object)Pair.of((Object)value, input));
            }

            public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
                return t;
            }

            public String toString() {
                return "OrElsePartial[" + String.valueOf(value) + "]";
            }
        };
    }

    public static <E> Codec<E> idResolverCodec(ToIntFunction<E> toInt, IntFunction<@Nullable E> fromInt, int unknownId) {
        return Codec.INT.flatXmap(id -> Optional.ofNullable(fromInt.apply((int)id)).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown element id: " + id)), e -> {
            int id = toInt.applyAsInt(e);
            return id == unknownId ? DataResult.error(() -> "Element with unknown id: " + String.valueOf(e)) : DataResult.success((Object)id);
        });
    }

    public static <I, E> Codec<E> idResolverCodec(Codec<I> value, Function<I, @Nullable E> fromId, Function<E, @Nullable I> toId) {
        return value.flatXmap(id -> {
            Object element = fromId.apply(id);
            return element == null ? DataResult.error(() -> "Unknown element id: " + String.valueOf(id)) : DataResult.success(element);
        }, e -> {
            Object id = toId.apply(e);
            if (id == null) {
                return DataResult.error(() -> "Element with unknown id: " + String.valueOf(e));
            }
            return DataResult.success(id);
        });
    }

    public static <E> Codec<E> orCompressed(final Codec<E> normal, final Codec<E> compressed) {
        return new Codec<E>(){

            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                if (ops.compressMaps()) {
                    return compressed.encode(input, ops, prefix);
                }
                return normal.encode(input, ops, prefix);
            }

            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                if (ops.compressMaps()) {
                    return compressed.decode(ops, input);
                }
                return normal.decode(ops, input);
            }

            public String toString() {
                return String.valueOf(normal) + " orCompressed " + String.valueOf(compressed);
            }
        };
    }

    public static <E> MapCodec<E> orCompressed(final MapCodec<E> normal, final MapCodec<E> compressed) {
        return new MapCodec<E>(){

            public <T> RecordBuilder<T> encode(E input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                if (ops.compressMaps()) {
                    return compressed.encode(input, ops, prefix);
                }
                return normal.encode(input, ops, prefix);
            }

            public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
                if (ops.compressMaps()) {
                    return compressed.decode(ops, input);
                }
                return normal.decode(ops, input);
            }

            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return compressed.keys(ops);
            }

            public String toString() {
                return String.valueOf(normal) + " orCompressed " + String.valueOf(compressed);
            }
        };
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> codec, final Function<E, Lifecycle> decodeLifecycle, final Function<E, Lifecycle> encodeLifecycle) {
        return codec.mapResult(new Codec.ResultFunction<E>(){

            public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<E, T>> a) {
                return a.result().map(r -> a.setLifecycle((Lifecycle)decodeLifecycle.apply(r.getFirst()))).orElse(a);
            }

            public <T> DataResult<T> coApply(DynamicOps<T> ops, E input, DataResult<T> t) {
                return t.setLifecycle((Lifecycle)encodeLifecycle.apply(input));
            }

            public String toString() {
                return "WithLifecycle[" + String.valueOf(decodeLifecycle) + " " + String.valueOf(encodeLifecycle) + "]";
            }
        });
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> lifecycleGetter) {
        return ExtraCodecs.overrideLifecycle(codec, lifecycleGetter, lifecycleGetter);
    }

    public static <K, V> StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> keyCodec, Codec<V> elementCodec) {
        return new StrictUnboundedMapCodec<K, V>(keyCodec, elementCodec);
    }

    public static <E> Codec<List<E>> compactListCodec(Codec<E> elementCodec) {
        return ExtraCodecs.compactListCodec(elementCodec, elementCodec.listOf());
    }

    public static <E> Codec<List<E>> compactListCodec(Codec<E> elementCodec, Codec<List<E>> listCodec) {
        return Codec.either(listCodec, elementCodec).xmap(e -> (List)e.map(l -> l, List::of), v -> v.size() == 1 ? Either.right(v.getFirst()) : Either.left((Object)v));
    }

    private static Codec<Integer> intRangeWithMessage(int minInclusive, int maxInclusive, Function<Integer, String> error) {
        return Codec.INT.validate(value -> {
            if (value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0) {
                return DataResult.success((Object)value);
            }
            return DataResult.error(() -> (String)error.apply((Integer)value));
        });
    }

    public static Codec<Integer> intRange(int minInclusive, int maxInclusive) {
        return ExtraCodecs.intRangeWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
    }

    private static Codec<Long> longRangeWithMessage(long minInclusive, long maxInclusive, Function<Long, String> error) {
        return Codec.LONG.validate(value -> {
            if ((long)value.compareTo(minInclusive) >= 0L && (long)value.compareTo(maxInclusive) <= 0L) {
                return DataResult.success((Object)value);
            }
            return DataResult.error(() -> (String)error.apply((Long)value));
        });
    }

    public static Codec<Long> longRange(int minInclusive, int maxInclusive) {
        return ExtraCodecs.longRangeWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
    }

    private static Codec<Float> floatRangeMinInclusiveWithMessage(float minInclusive, float maxInclusive, Function<Float, String> error) {
        return Codec.FLOAT.validate(value -> {
            if (value.compareTo(Float.valueOf(minInclusive)) >= 0 && value.compareTo(Float.valueOf(maxInclusive)) <= 0) {
                return DataResult.success((Object)value);
            }
            return DataResult.error(() -> (String)error.apply((Float)value));
        });
    }

    private static Codec<Float> floatRangeMinExclusiveWithMessage(float minExclusive, float maxInclusive, Function<Float, String> error) {
        return Codec.FLOAT.validate(value -> {
            if (value.compareTo(Float.valueOf(minExclusive)) > 0 && value.compareTo(Float.valueOf(maxInclusive)) <= 0) {
                return DataResult.success((Object)value);
            }
            return DataResult.error(() -> (String)error.apply((Float)value));
        });
    }

    public static Codec<Float> floatRange(float minInclusive, float maxInclusive) {
        return ExtraCodecs.floatRangeMinInclusiveWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
    }

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> listCodec) {
        return listCodec.validate(list -> list.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success((Object)list));
    }

    public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> listCodec) {
        return listCodec.validate(list -> {
            if (list.unwrap().right().filter(List::isEmpty).isPresent()) {
                return DataResult.error(() -> "List must have contents");
            }
            return DataResult.success((Object)list);
        });
    }

    public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(Codec<M> mapCodec) {
        return mapCodec.validate(map -> map.isEmpty() ? DataResult.error(() -> "Map must have contents") : DataResult.success((Object)map));
    }

    public static <E> MapCodec<E> retrieveContext(Function<DynamicOps<?>, DataResult<E>> getter) {
        class ContextRetrievalCodec
        extends MapCodec<E> {
            final /* synthetic */ Function val$getter;

            ContextRetrievalCodec(Function function) {
                this.val$getter = function;
            }

            public <T> RecordBuilder<T> encode(E input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return prefix;
            }

            public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
                return (DataResult)this.val$getter.apply(ops);
            }

            public String toString() {
                return "ContextRetrievalCodec[" + String.valueOf(this.val$getter) + "]";
            }

            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }
        }
        return new ContextRetrievalCodec(getter);
    }

    public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> typeGetter) {
        return container -> {
            Iterator it = container.iterator();
            if (it.hasNext()) {
                Object firstType = typeGetter.apply(it.next());
                while (it.hasNext()) {
                    Object next = it.next();
                    Object nextType = typeGetter.apply(next);
                    if (nextType == firstType) continue;
                    return DataResult.error(() -> "Mixed type list: element " + String.valueOf(next) + " had type " + String.valueOf(nextType) + ", but list is of type " + String.valueOf(firstType));
                }
            }
            return DataResult.success((Object)container, (Lifecycle)Lifecycle.stable());
        };
    }

    public static <A> Codec<A> catchDecoderException(final Codec<A> codec) {
        return Codec.of(codec, (Decoder)new Decoder<A>(){

            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                try {
                    return codec.decode(ops, input);
                }
                catch (Exception e) {
                    return DataResult.error(() -> "Caught exception decoding " + String.valueOf(input) + ": " + e.getMessage());
                }
            }
        });
    }

    public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter formatter) {
        return Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success((Object)formatter.parse((CharSequence)s));
            }
            catch (Exception e) {
                return DataResult.error(e::getMessage);
            }
        }, formatter::format);
    }

    public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> fieldCodec) {
        return fieldCodec.xmap(toOptionalLong, fromOptionalLong);
    }

    private static MapCodec<GameProfile> gameProfileCodec(Codec<UUID> uuidCodec) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)uuidCodec.fieldOf("id").forGetter(GameProfile::id), (App)PLAYER_NAME.fieldOf("name").forGetter(GameProfile::name), (App)PROPERTY_MAP.optionalFieldOf("properties", (Object)PropertyMap.EMPTY).forGetter(GameProfile::properties)).apply((Applicative)i, GameProfile::new));
    }

    public static <K, V> Codec<Map<K, V>> sizeLimitedMap(Codec<Map<K, V>> codec, int maxSizeInclusive) {
        return codec.validate(map -> {
            if (map.size() > maxSizeInclusive) {
                return DataResult.error(() -> "Map is too long: " + map.size() + ", expected range [0-" + maxSizeInclusive + "]");
            }
            return DataResult.success((Object)map);
        });
    }

    public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> keyCodec) {
        return Codec.unboundedMap(keyCodec, (Codec)Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
    }

    @Deprecated
    public static <K, V> MapCodec<V> dispatchOptionalValue(final String typeKey, final String valueKey, final Codec<K> typeCodec, final Function<? super V, ? extends K> typeGetter, final Function<? super K, ? extends Codec<? extends V>> valueCodec) {
        return new MapCodec<V>(){

            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(typeKey), ops.createString(valueKey));
            }

            public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
                Object typeName = input.get(typeKey);
                if (typeName == null) {
                    return DataResult.error(() -> "Missing \"" + typeKey + "\" in: " + String.valueOf(input));
                }
                return typeCodec.decode(ops, typeName).flatMap(type -> {
                    Object value = Objects.requireNonNullElseGet(input.get(valueKey), () -> ((DynamicOps)ops).emptyMap());
                    return ((Codec)valueCodec.apply(type.getFirst())).decode(ops, value).map(Pair::getFirst);
                });
            }

            public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> builder) {
                Object type = typeGetter.apply(input);
                builder.add(typeKey, typeCodec.encodeStart(ops, type));
                DataResult<T> parameters = this.encode((Codec)valueCodec.apply(type), input, ops);
                if (parameters.result().isEmpty() || !Objects.equals(parameters.result().get(), ops.emptyMap())) {
                    builder.add(valueKey, parameters);
                }
                return builder;
            }

            private <T, V2 extends V> DataResult<T> encode(Codec<V2> codec, V input, DynamicOps<T> ops) {
                return codec.encodeStart(ops, input);
            }
        };
    }

    public static <A> Codec<Optional<A>> optionalEmptyMap(final Codec<A> codec) {
        return new Codec<Optional<A>>(){

            public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> ops, T input) {
                if (7.isEmptyMap(ops, input)) {
                    return DataResult.success((Object)Pair.of(Optional.empty(), input));
                }
                return codec.decode(ops, input).map(pair -> pair.mapFirst(Optional::of));
            }

            private static <T> boolean isEmptyMap(DynamicOps<T> ops, T input) {
                Optional map = ops.getMap(input).result();
                return map.isPresent() && ((MapLike)map.get()).entries().findAny().isEmpty();
            }

            public <T> DataResult<T> encode(Optional<A> input, DynamicOps<T> ops, T prefix) {
                if (input.isEmpty()) {
                    return DataResult.success((Object)ops.emptyMap());
                }
                return codec.encode(input.get(), ops, prefix);
            }
        };
    }

    @Deprecated
    public static <E extends Enum<E>> Codec<E> legacyEnum(Function<String, E> valueOf) {
        return Codec.STRING.comapFlatMap(key -> {
            try {
                return DataResult.success((Object)((Enum)valueOf.apply((String)key)));
            }
            catch (IllegalArgumentException ignored) {
                return DataResult.error(() -> "No value with id: " + key);
            }
        }, Enum::toString);
    }

    public static Codec<Path> pathCodec(Function<String, Path> pathFactory) {
        return Codec.STRING.xmap(pathFactory, path -> FilenameUtils.separatorsToUnix((String)path.toString()));
    }

    public static Codec<Path> relaiveNormalizedSubPathCodec(Function<String, Path> pathFactory) {
        return ExtraCodecs.pathCodec(pathFactory).xmap(Path::normalize, Path::normalize).validate(path -> {
            if (path.isAbsolute()) {
                return DataResult.error(() -> "Illegal absolute path: " + String.valueOf(path));
            }
            if (path.startsWith("..") || path.startsWith(".") || FileUtil.isEmptyPath(path)) {
                return DataResult.error(() -> "Illegal path traversal: " + String.valueOf(path));
            }
            return DataResult.success((Object)path);
        });
    }

    public static Codec<Path> guardedPathCodec(Path baseFolder) {
        FileSystem fileSystem = baseFolder.getFileSystem();
        Objects.requireNonNull(fileSystem);
        FileSystem fileSystem2 = fileSystem;
        return ExtraCodecs.relaiveNormalizedSubPathCodec(x$0 -> fileSystem2.getPath((String)x$0, new String[0])).xmap(baseFolder::resolve, baseFolder::relativize);
    }

    public static <A> MapCodec<A> optionalAlwaysPresentFieldOf(Codec<A> elementCodec, String name, A defaultValue, boolean lenient) {
        return Codec.optionalField((String)name, elementCodec, (boolean)lenient).xmap(o -> o.orElse(defaultValue), Optional::of);
    }

    public static <A> MapCodec<A> optionalAlwaysPresentFieldOf(Codec<A> elementCodec, String name, A defaultValue) {
        return ExtraCodecs.optionalAlwaysPresentFieldOf(elementCodec, name, defaultValue, false);
    }

    public record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements BaseMapCodec<K, V>,
    Codec<Map<K, V>>
    {
        public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
            ImmutableMap.Builder read = ImmutableMap.builder();
            for (Pair pair : input.entries().toList()) {
                DataResult v;
                DataResult k = this.keyCodec().parse(ops, pair.getFirst());
                DataResult entry = k.apply2stable(Pair::of, v = this.elementCodec().parse(ops, pair.getSecond()));
                Optional error = entry.error();
                if (error.isPresent()) {
                    String errorMessage = ((DataResult.Error)error.get()).message();
                    return DataResult.error(() -> {
                        if (k.result().isPresent()) {
                            return "Map entry '" + String.valueOf(k.result().get()) + "' : " + errorMessage;
                        }
                        return errorMessage;
                    });
                }
                if (entry.result().isPresent()) {
                    Pair kvPair = (Pair)entry.result().get();
                    read.put(kvPair.getFirst(), kvPair.getSecond());
                    continue;
                }
                return DataResult.error(() -> "Empty or invalid map contents are not allowed");
            }
            ImmutableMap elements = read.build();
            return DataResult.success((Object)elements);
        }

        public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> this.decode(ops, (Object)map)).map(r -> Pair.of((Object)r, (Object)input));
        }

        public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
            return this.encode(input, ops, ops.mapBuilder()).build(prefix);
        }

        @Override
        public String toString() {
            return "StrictUnboundedMapCodec[" + String.valueOf(this.keyCodec) + " -> " + String.valueOf(this.elementCodec) + "]";
        }
    }

    public record TagOrElementLocation(Identifier id, boolean tag) {
        @Override
        public String toString() {
            return this.decoratedId();
        }

        private String decoratedId() {
            return this.tag ? ExtraCodecs.HEX_COLOR_PREFIX + String.valueOf(this.id) : this.id.toString();
        }
    }

    public static class LateBoundIdMapper<I, V> {
        private final BiMap<I, V> idToValue = HashBiMap.create();

        public Codec<V> codec(Codec<I> idCodec) {
            BiMap valueToId = this.idToValue.inverse();
            return ExtraCodecs.idResolverCodec(idCodec, arg_0 -> this.idToValue.get(arg_0), arg_0 -> valueToId.get(arg_0));
        }

        public LateBoundIdMapper<I, V> put(I id, V value) {
            Objects.requireNonNull(value, () -> "Value for " + String.valueOf(id) + " is null");
            this.idToValue.put(id, value);
            return this;
        }

        public Set<V> values() {
            return Collections.unmodifiableSet(this.idToValue.values());
        }
    }
}

