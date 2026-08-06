/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public interface LoggedChatEvent {
    public static final Codec<LoggedChatEvent> CODEC = StringRepresentable.fromEnum(Type::values).dispatch(LoggedChatEvent::type, Type::codec);

    public Type type();

    @Environment(value=EnvType.CLIENT)
    public static enum Type implements StringRepresentable
    {
        PLAYER("player", () -> LoggedChatMessage.Player.CODEC),
        SYSTEM("system", () -> LoggedChatMessage.System.CODEC);

        private final String serializedName;
        private final Supplier<MapCodec<? extends LoggedChatEvent>> codec;

        private Type(String serializedName, Supplier<MapCodec<? extends LoggedChatEvent>> codec) {
            this.serializedName = serializedName;
            this.codec = codec;
        }

        private MapCodec<? extends LoggedChatEvent> codec() {
            return this.codec.get();
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}

