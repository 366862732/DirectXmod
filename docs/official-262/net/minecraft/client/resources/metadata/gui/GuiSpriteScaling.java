/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public interface GuiSpriteScaling {
    public static final Codec<GuiSpriteScaling> CODEC = Type.CODEC.dispatch(GuiSpriteScaling::type, Type::codec);
    public static final GuiSpriteScaling DEFAULT = new Stretch();

    public Type type();

    @Environment(value=EnvType.CLIENT)
    public static enum Type implements StringRepresentable
    {
        STRETCH("stretch", Stretch.CODEC),
        TILE("tile", Tile.CODEC),
        NINE_SLICE("nine_slice", NineSlice.CODEC);

        public static final Codec<Type> CODEC;
        private final String key;
        private final MapCodec<? extends GuiSpriteScaling> codec;

        private Type(String key, MapCodec<? extends GuiSpriteScaling> codec) {
            this.key = key;
            this.codec = codec;
        }

        @Override
        public String getSerializedName() {
            return this.key;
        }

        public MapCodec<? extends GuiSpriteScaling> codec() {
            return this.codec;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Stretch() implements GuiSpriteScaling
    {
        public static final MapCodec<Stretch> CODEC = MapCodec.unit(Stretch::new);

        @Override
        public Type type() {
            return Type.STRETCH;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record NineSlice(int width, int height, Border border, boolean stretchInner) implements GuiSpriteScaling
    {
        public static final MapCodec<NineSlice> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(NineSlice::width), (App)ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(NineSlice::height), (App)Border.CODEC.fieldOf("border").forGetter(NineSlice::border), (App)Codec.BOOL.optionalFieldOf("stretch_inner", (Object)false).forGetter(NineSlice::stretchInner)).apply((Applicative)i, NineSlice::new)).validate(NineSlice::validate);

        private static DataResult<NineSlice> validate(NineSlice nineSlice) {
            Border border = nineSlice.border();
            if (border.left() + border.right() >= nineSlice.width()) {
                return DataResult.error(() -> "Nine-sliced texture has no horizontal center slice: " + border.left() + " + " + border.right() + " >= " + nineSlice.width());
            }
            if (border.top() + border.bottom() >= nineSlice.height()) {
                return DataResult.error(() -> "Nine-sliced texture has no vertical center slice: " + border.top() + " + " + border.bottom() + " >= " + nineSlice.height());
            }
            return DataResult.success((Object)nineSlice);
        }

        @Override
        public Type type() {
            return Type.NINE_SLICE;
        }

        @Environment(value=EnvType.CLIENT)
        public record Border(int left, int top, int right, int bottom) {
            private static final Codec<Border> VALUE_CODEC = ExtraCodecs.POSITIVE_INT.flatComapMap(size -> new Border((int)size, (int)size, (int)size, (int)size), border -> {
                OptionalInt size = border.unpackValue();
                if (size.isPresent()) {
                    return DataResult.success((Object)size.getAsInt());
                }
                return DataResult.error(() -> "Border has different side sizes");
            });
            private static final Codec<Border> RECORD_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(Border::left), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(Border::top), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(Border::right), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(Border::bottom)).apply((Applicative)i, Border::new));
            private static final Codec<Border> CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC).xmap(Either::unwrap, border -> {
                if (border.unpackValue().isPresent()) {
                    return Either.left((Object)border);
                }
                return Either.right((Object)border);
            });

            private OptionalInt unpackValue() {
                if (this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom()) {
                    return OptionalInt.of(this.left());
                }
                return OptionalInt.empty();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Tile(int width, int height) implements GuiSpriteScaling
    {
        public static final MapCodec<Tile> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(Tile::width), (App)ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(Tile::height)).apply((Applicative)i, Tile::new));

        @Override
        public Type type() {
            return Type.TILE;
        }
    }
}

