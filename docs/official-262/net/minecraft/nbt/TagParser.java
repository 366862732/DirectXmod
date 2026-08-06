/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.nbt;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class TagParser<T> {
    public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType((Message)Component.translatable("argument.nbt.trailing"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new SimpleCommandExceptionType((Message)Component.translatable("argument.nbt.expected.compound"));
    public static final char ELEMENT_SEPARATOR = ',';
    public static final char NAME_VALUE_SEPARATOR = ':';
    private static final TagParser<Tag> NBT_OPS_PARSER = TagParser.create(NbtOps.INSTANCE);
    public static final Codec<CompoundTag> FLATTENED_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            Tag result = NBT_OPS_PARSER.parseFully((String)s);
            if (result instanceof CompoundTag) {
                CompoundTag compoundTag = (CompoundTag)result;
                return DataResult.success((Object)compoundTag, (Lifecycle)Lifecycle.stable());
            }
            return DataResult.error(() -> "Expected compound tag, got " + String.valueOf(result));
        }
        catch (CommandSyntaxException e) {
            return DataResult.error(() -> ((CommandSyntaxException)e).getMessage());
        }
    }, CompoundTag::toString);
    public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
    private final DynamicOps<T> ops;
    private final Grammar<T> grammar;

    private TagParser(DynamicOps<T> ops, Grammar<T> grammar) {
        this.ops = ops;
        this.grammar = grammar;
    }

    public DynamicOps<T> getOps() {
        return this.ops;
    }

    public static <T> TagParser<T> create(DynamicOps<T> ops) {
        return new TagParser<T>(ops, SnbtGrammar.createParser(ops));
    }

    private static CompoundTag castToCompoundOrThrow(StringReader reader, Tag result) throws CommandSyntaxException {
        if (result instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)result;
            return compoundTag;
        }
        throw ERROR_EXPECTED_COMPOUND.createWithContext((ImmutableStringReader)reader);
    }

    public static CompoundTag parseCompoundFully(String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        return TagParser.castToCompoundOrThrow(reader, NBT_OPS_PARSER.parseFully(reader));
    }

    public T parseFully(String input) throws CommandSyntaxException {
        return this.parseFully(new StringReader(input));
    }

    public T parseFully(StringReader reader) throws CommandSyntaxException {
        T result = this.grammar.parseForCommands(reader);
        reader.skipWhitespace();
        if (reader.canRead()) {
            throw ERROR_TRAILING_DATA.createWithContext((ImmutableStringReader)reader);
        }
        return result;
    }

    public T parseAsArgument(StringReader reader) throws CommandSyntaxException {
        return this.grammar.parseForCommands(reader);
    }

    public static CompoundTag parseCompoundAsArgument(StringReader reader) throws CommandSyntaxException {
        Tag result = NBT_OPS_PARSER.parseAsArgument(reader);
        return TagParser.castToCompoundOrThrow(reader, result);
    }
}

