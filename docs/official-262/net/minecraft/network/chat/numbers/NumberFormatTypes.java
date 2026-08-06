/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.network.chat.numbers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class NumberFormatTypes {
    public static final MapCodec<NumberFormat> MAP_CODEC = BuiltInRegistries.NUMBER_FORMAT_TYPE.byNameCodec().dispatchMap(NumberFormat::type, NumberFormatType::mapCodec);
    public static final Codec<NumberFormat> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, NumberFormat> STREAM_CODEC = ByteBufCodecs.registry(Registries.NUMBER_FORMAT_TYPE).dispatch(NumberFormat::type, NumberFormatType::streamCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<NumberFormat>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

    public static NumberFormatType<?> bootstrap(Registry<NumberFormatType<?>> registry) {
        Registry.register(registry, "blank", BlankFormat.TYPE);
        Registry.register(registry, "styled", StyledFormat.TYPE);
        return Registry.register(registry, "fixed", FixedFormat.TYPE);
    }
}

