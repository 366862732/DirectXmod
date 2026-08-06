/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType$StringType
 */
package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer
implements ArgumentTypeInfo<StringArgumentType, Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf out) {
        out.writeEnum((Enum<?>)template.type);
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf in) {
        StringArgumentType.StringType type = in.readEnum(StringArgumentType.StringType.class);
        return new Template(this, type);
    }

    @Override
    public void serializeToJson(Template template, JsonObject out) {
        out.addProperty("type", switch (template.type) {
            default -> throw new MatchException(null, null);
            case StringArgumentType.StringType.SINGLE_WORD -> "word";
            case StringArgumentType.StringType.QUOTABLE_PHRASE -> "phrase";
            case StringArgumentType.StringType.GREEDY_PHRASE -> "greedy";
        });
    }

    @Override
    public Template unpack(StringArgumentType argument) {
        return new Template(this, argument.getType());
    }

    public final class Template
    implements ArgumentTypeInfo.Template<StringArgumentType> {
        private final StringArgumentType.StringType type;
        final /* synthetic */ StringArgumentSerializer this$0;

        public Template(StringArgumentSerializer this$0, StringArgumentType.StringType type) {
            StringArgumentSerializer stringArgumentSerializer = this$0;
            Objects.requireNonNull(stringArgumentSerializer);
            this.this$0 = stringArgumentSerializer;
            this.type = type;
        }

        @Override
        public StringArgumentType instantiate(CommandBuildContext context) {
            return switch (this.type) {
                default -> throw new MatchException(null, null);
                case StringArgumentType.StringType.SINGLE_WORD -> StringArgumentType.word();
                case StringArgumentType.StringType.QUOTABLE_PHRASE -> StringArgumentType.string();
                case StringArgumentType.StringType.GREEDY_PHRASE -> StringArgumentType.greedyString();
            };
        }

        @Override
        public ArgumentTypeInfo<StringArgumentType, ?> type() {
            return this.this$0;
        }
    }
}

