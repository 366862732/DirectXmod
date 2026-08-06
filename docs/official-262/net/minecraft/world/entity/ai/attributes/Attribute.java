/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class Attribute {
    public static final Codec<Holder<Attribute>> CODEC = BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE);
    private final double defaultValue;
    private boolean syncable;
    private final String descriptionId;
    private Sentiment sentiment = Sentiment.POSITIVE;

    protected Attribute(String descriptionId, double defaultValue) {
        this.defaultValue = defaultValue;
        this.descriptionId = descriptionId;
    }

    public double getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isClientSyncable() {
        return this.syncable;
    }

    public Attribute setSyncable(boolean syncable) {
        this.syncable = syncable;
        return this;
    }

    public Attribute setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
        return this;
    }

    public double sanitizeValue(double value) {
        return value;
    }

    public String getDescriptionId() {
        return this.descriptionId;
    }

    public ChatFormatting getStyle(boolean valueIncrease) {
        return this.sentiment.getStyle(valueIncrease);
    }

    public static enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE;


        public ChatFormatting getStyle(boolean valueIncrease) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> {
                    if (valueIncrease) {
                        yield ChatFormatting.BLUE;
                    }
                    yield ChatFormatting.RED;
                }
                case 1 -> ChatFormatting.GRAY;
                case 2 -> valueIncrease ? ChatFormatting.RED : ChatFormatting.BLUE;
            };
        }
    }
}

