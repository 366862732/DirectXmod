/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

public class CompilableString<T> {
    private final String source;
    private final T compiled;

    private CompilableString(String source, T compiled) {
        this.source = source;
        this.compiled = compiled;
    }

    public static <T> Codec<CompilableString<T>> codec(Function<String, DataResult<T>> compiler) {
        return Codec.STRING.comapFlatMap(s -> ((DataResult)compiler.apply((String)s)).map(compiled -> new CompilableString<Object>((String)s, compiled)), CompilableString::source);
    }

    public String source() {
        return this.source;
    }

    public T compiled() {
        return this.compiled;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (!(o instanceof CompilableString)) return false;
        CompilableString that = (CompilableString)o;
        if (!Objects.equals(this.source, that.source)) return false;
        return true;
    }

    public int hashCode() {
        return this.source.hashCode();
    }

    public String toString() {
        return this.source;
    }

    public static abstract class CommandParserHelper<T>
    implements Function<String, DataResult<T>> {
        private static final DynamicCommandExceptionType TRAILING_DATA = new DynamicCommandExceptionType(commandAndRemainder -> Component.translatableEscape("command.trailing_data", commandAndRemainder));

        @Override
        public final DataResult<T> apply(String contents) {
            StringReader reader = new StringReader(contents);
            try {
                T result = this.parse(reader);
                if (reader.canRead()) {
                    String parsed = reader.getString().substring(0, reader.getCursor());
                    String leftovers = reader.getString().substring(reader.getCursor());
                    throw TRAILING_DATA.create((Object)(parsed + "[" + leftovers + "]"));
                }
                return DataResult.success(result);
            }
            catch (CommandSyntaxException ex) {
                return DataResult.error(() -> this.errorMessage(contents, ex));
            }
        }

        protected abstract T parse(StringReader var1) throws CommandSyntaxException;

        protected abstract String errorMessage(String var1, CommandSyntaxException var2);
    }
}

