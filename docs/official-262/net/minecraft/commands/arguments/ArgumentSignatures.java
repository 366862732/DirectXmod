/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignableCommand;
import org.jspecify.annotations.Nullable;

public record ArgumentSignatures(List<Entry> entries) {
    public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
    private static final int MAX_ARGUMENT_COUNT = 8;
    private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

    public ArgumentSignatures(FriendlyByteBuf input) {
        this(input.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), Entry::new));
    }

    public void write(FriendlyByteBuf output) {
        output.writeCollection(this.entries, (out, entry) -> entry.write((FriendlyByteBuf)((Object)out)));
    }

    public static ArgumentSignatures signCommand(SignableCommand<?> command, Signer signer) {
        List<Entry> entries = command.arguments().stream().map(argument -> {
            MessageSignature signature = signer.sign(argument.value());
            if (signature != null) {
                return new Entry(argument.name(), signature);
            }
            return null;
        }).filter(Objects::nonNull).toList();
        return new ArgumentSignatures(entries);
    }

    @FunctionalInterface
    public static interface Signer {
        public @Nullable MessageSignature sign(String var1);
    }

    public record Entry(String name, MessageSignature signature) {
        public Entry(FriendlyByteBuf input) {
            this(input.readUtf(16), MessageSignature.read(input));
        }

        public void write(FriendlyByteBuf output) {
            output.writeUtf(this.name, 16);
            MessageSignature.write(output, this.signature);
        }
    }
}

