/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.chat;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public record ChatTypeDecoration(String translationKey, List<Parameter> parameters, Style style) {
    public static final Codec<ChatTypeDecoration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("translation_key").forGetter(ChatTypeDecoration::translationKey), (App)Parameter.CODEC.listOf().fieldOf("parameters").forGetter(ChatTypeDecoration::parameters), (App)Style.Serializer.CODEC.optionalFieldOf("style", (Object)Style.EMPTY).forGetter(ChatTypeDecoration::style)).apply((Applicative)i, ChatTypeDecoration::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatTypeDecoration> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ChatTypeDecoration::translationKey, Parameter.STREAM_CODEC.apply(ByteBufCodecs.list()), ChatTypeDecoration::parameters, Style.Serializer.TRUSTED_STREAM_CODEC, ChatTypeDecoration::style, ChatTypeDecoration::new);

    public static ChatTypeDecoration withSender(String translationKey) {
        return new ChatTypeDecoration(translationKey, List.of(Parameter.SENDER, Parameter.CONTENT), Style.EMPTY);
    }

    public static ChatTypeDecoration incomingDirectMessage(String translationKey) {
        Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
        return new ChatTypeDecoration(translationKey, List.of(Parameter.SENDER, Parameter.CONTENT), style);
    }

    public static ChatTypeDecoration outgoingDirectMessage(String translationKey) {
        Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
        return new ChatTypeDecoration(translationKey, List.of(Parameter.TARGET, Parameter.CONTENT), style);
    }

    public static ChatTypeDecoration teamMessage(String translationKey) {
        return new ChatTypeDecoration(translationKey, List.of(Parameter.TARGET, Parameter.SENDER, Parameter.CONTENT), Style.EMPTY);
    }

    public Component decorate(Component content, ChatType.Bound chatType) {
        Object[] parameters = this.resolveParameters(content, chatType);
        return Component.translatable(this.translationKey, parameters).withStyle(this.style);
    }

    private Component[] resolveParameters(Component content, ChatType.Bound chatType) {
        Component[] resolved = new Component[this.parameters.size()];
        for (int i = 0; i < resolved.length; ++i) {
            Parameter parameter = this.parameters.get(i);
            resolved[i] = parameter.select(content, chatType);
        }
        return resolved;
    }

    public static enum Parameter implements StringRepresentable
    {
        SENDER(0, "sender", (content, chatType) -> chatType.name()),
        TARGET(1, "target", (content, chatType) -> chatType.targetName().orElse(CommonComponents.EMPTY)),
        CONTENT(2, "content", (content, chatType) -> content);

        private static final IntFunction<Parameter> BY_ID;
        public static final Codec<Parameter> CODEC;
        public static final StreamCodec<ByteBuf, Parameter> STREAM_CODEC;
        private final int id;
        private final String name;
        private final Selector selector;

        private Parameter(int id, String name, Selector selector) {
            this.id = id;
            this.name = name;
            this.selector = selector;
        }

        public Component select(Component content, ChatType.Bound chatType) {
            return this.selector.select(content, chatType);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            BY_ID = ByIdMap.continuous(p -> p.id, Parameter.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            CODEC = StringRepresentable.fromEnum(Parameter::values);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p -> p.id);
        }

        public static interface Selector {
            public Component select(Component var1, ChatType.Bound var2);
        }
    }
}

