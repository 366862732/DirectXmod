/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.decoration.painting;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.ExtraCodecs;

public record PaintingVariant(int width, int height, Identifier assetId, Optional<Component> title, Optional<Component> author) {
    public static final Codec<PaintingVariant> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.intRange(1, 16).fieldOf("width").forGetter(PaintingVariant::width), (App)ExtraCodecs.intRange(1, 16).fieldOf("height").forGetter(PaintingVariant::height), (App)Identifier.CODEC.fieldOf("asset_id").forGetter(PaintingVariant::assetId), (App)ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PaintingVariant::title), (App)ComponentSerialization.CODEC.optionalFieldOf("author").forGetter(PaintingVariant::author)).apply((Applicative)i, PaintingVariant::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PaintingVariant> DIRECT_STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PaintingVariant::width, ByteBufCodecs.VAR_INT, PaintingVariant::height, Identifier.STREAM_CODEC, PaintingVariant::assetId, ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, PaintingVariant::title, ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, PaintingVariant::author, PaintingVariant::new);
    public static final Codec<Holder<PaintingVariant>> CODEC = RegistryFixedCodec.create(Registries.PAINTING_VARIANT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<PaintingVariant>> STREAM_CODEC = ByteBufCodecs.holder(Registries.PAINTING_VARIANT, DIRECT_STREAM_CODEC);

    public int area() {
        return this.width() * this.height();
    }
}

