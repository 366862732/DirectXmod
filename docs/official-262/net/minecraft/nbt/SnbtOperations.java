/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.serialization.DynamicOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import org.jspecify.annotations.Nullable;

public class SnbtOperations {
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_STRING_UUID = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_string_uuid")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NUMBER_OR_BOOLEAN = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_number_or_boolean")));
    public static final String BUILTIN_TRUE = "true";
    public static final String BUILTIN_FALSE = "false";
    public static final Map<BuiltinKey, BuiltinOperation> BUILTIN_OPERATIONS = Map.of(new BuiltinKey("bool", 1), new BuiltinOperation(){

        @Override
        public <T> T run(DynamicOps<T> ops, List<T> arguments, ParseState<StringReader> state) {
            Boolean result = 1.convert(ops, arguments.getFirst());
            if (result == null) {
                state.errorCollector().store(state.mark(), ERROR_EXPECTED_NUMBER_OR_BOOLEAN);
                return null;
            }
            return (T)ops.createBoolean(result.booleanValue());
        }

        private static <T> @Nullable Boolean convert(DynamicOps<T> ops, T arg) {
            Optional asBoolean = ops.getBooleanValue(arg).result();
            if (asBoolean.isPresent()) {
                return (Boolean)asBoolean.get();
            }
            Optional asNumber = ops.getNumberValue(arg).result();
            if (asNumber.isPresent()) {
                return ((Number)asNumber.get()).doubleValue() != 0.0;
            }
            return null;
        }
    }, new BuiltinKey("uuid", 1), new BuiltinOperation(){

        @Override
        public <T> T run(DynamicOps<T> ops, List<T> arguments, ParseState<StringReader> state) {
            UUID uuid;
            Optional arg = ops.getStringValue(arguments.getFirst()).result();
            if (arg.isEmpty()) {
                state.errorCollector().store(state.mark(), ERROR_EXPECTED_STRING_UUID);
                return null;
            }
            try {
                uuid = UUID.fromString((String)arg.get());
            }
            catch (IllegalArgumentException e) {
                state.errorCollector().store(state.mark(), ERROR_EXPECTED_STRING_UUID);
                return null;
            }
            return (T)ops.createIntList(IntStream.of(UUIDUtil.uuidToIntArray(uuid)));
        }
    });
    public static final SuggestionSupplier<StringReader> BUILTIN_IDS = new SuggestionSupplier<StringReader>(){
        private final Set<String> keys = Stream.concat(Stream.of("false", "true"), BUILTIN_OPERATIONS.keySet().stream().map(BuiltinKey::id)).collect(Collectors.toSet());

        @Override
        public Stream<String> possibleValues(ParseState<StringReader> state) {
            return this.keys.stream();
        }
    };

    public record BuiltinKey(String id, int argCount) {
        @Override
        public String toString() {
            return this.id + "/" + this.argCount;
        }
    }

    public static interface BuiltinOperation {
        public <T> @Nullable T run(DynamicOps<T> var1, List<T> var2, ParseState<StringReader> var3);
    }
}

