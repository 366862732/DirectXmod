/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.primitives.UnsignedBytes
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JavaOps
 *  it.unimi.dsi.fastutil.bytes.ByteArrayList
 *  it.unimi.dsi.fastutil.chars.CharList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import java.lang.runtime.SwitchBootstraps;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import net.minecraft.nbt.SnbtOperations;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPatternParseRule;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.NumberRunParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;
import org.jspecify.annotations.Nullable;

public class SnbtGrammar {
    private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new DynamicCommandExceptionType(message -> Component.translatableEscape("snbt.parser.number_parse_failure", message));
    private static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new DynamicCommandExceptionType(length -> Component.translatableEscape("snbt.parser.expected_hex_escape", length));
    private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new DynamicCommandExceptionType(codepoint -> Component.translatableEscape("snbt.parser.invalid_codepoint", codepoint));
    private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new DynamicCommandExceptionType(operation -> Component.translatableEscape("snbt.parser.no_such_operation", operation));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_integer_type")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_float_type")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_non_negative_number")));
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.invalid_character_name")));
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.invalid_array_element_type")));
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.invalid_unquoted_start")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_unquoted_string")));
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.invalid_string_contents")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_binary_numeral")));
    private static final DelayedException<CommandSyntaxException> ERROR_UNDESCORE_NOT_ALLOWED = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.underscore_not_allowed")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_decimal_numeral")));
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.expected_hex_numeral")));
    private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.empty_key")));
    private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.leading_zero_not_allowed")));
    private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(new SimpleCommandExceptionType((Message)Component.translatable("snbt.parser.infinity_not_allowed")));
    private static final HexFormat HEX_ESCAPE = HexFormat.of().withUpperCase();
    private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule((DelayedException)ERROR_EXPECTED_BINARY_NUMERAL, (DelayedException)ERROR_UNDESCORE_NOT_ALLOWED){

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule((DelayedException)ERROR_EXPECTED_DECIMAL_NUMERAL, (DelayedException)ERROR_UNDESCORE_NOT_ALLOWED){

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule((DelayedException)ERROR_EXPECTED_HEX_NUMERAL, (DelayedException)ERROR_UNDESCORE_NOT_ALLOWED){

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    };
    private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, (DelayedException)ERROR_INVALID_STRING_CONTENTS){

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '\"', '\'', '\\' -> false;
                default -> true;
            };
        }
    };
    private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()){

        @Override
        protected boolean isAccepted(char c) {
            return SnbtGrammar.canStartNumber(c);
        }
    };
    private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

    private static DelayedException<CommandSyntaxException> createNumberParseError(NumberFormatException ex) {
        return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, ex.getMessage());
    }

    public static @Nullable String escapeControlCharacters(char c) {
        return switch (c) {
            case '\b' -> "b";
            case '\t' -> "t";
            case '\n' -> "n";
            case '\f' -> "f";
            case '\r' -> "r";
            default -> c < ' ' ? "x" + HEX_ESCAPE.toHexDigits((byte)c) : null;
        };
    }

    private static boolean isAllowedToStartUnquotedString(char c) {
        return !SnbtGrammar.canStartNumber(c);
    }

    private static boolean canStartNumber(char c) {
        return switch (c) {
            case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }

    private static boolean needsUnderscoreRemoval(String contents) {
        return contents.indexOf(95) != -1;
    }

    private static void cleanAndAppend(StringBuilder output, String contents) {
        SnbtGrammar.cleanAndAppend(output, contents, SnbtGrammar.needsUnderscoreRemoval(contents));
    }

    private static void cleanAndAppend(StringBuilder output, String contents, boolean needsUnderscoreRemoval) {
        if (needsUnderscoreRemoval) {
            for (char c : contents.toCharArray()) {
                if (c == '_') continue;
                output.append(c);
            }
        } else {
            output.append(contents);
        }
    }

    private static short parseUnsignedShort(String string, int radix) {
        int parse = Integer.parseInt(string, radix);
        if (parse >> 16 == 0) {
            return (short)parse;
        }
        throw new NumberFormatException("out of range: " + parse);
    }

    private static <T> @Nullable T createFloat(DynamicOps<T> ops, Sign sign, @Nullable String whole, @Nullable String fraction, @Nullable Signed<String> exponent, @Nullable TypeSuffix typeSuffix, ParseState<?> state) {
        StringBuilder result = new StringBuilder();
        sign.append(result);
        if (whole != null) {
            SnbtGrammar.cleanAndAppend(result, whole);
        }
        if (fraction != null) {
            result.append('.');
            SnbtGrammar.cleanAndAppend(result, fraction);
        }
        if (exponent != null) {
            result.append('e');
            exponent.sign().append(result);
            SnbtGrammar.cleanAndAppend(result, (String)exponent.value);
        }
        try {
            String contents = result.toString();
            TypeSuffix typeSuffix2 = typeSuffix;
            int n = 0;
            return switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"FLOAT", "DOUBLE"}, (TypeSuffix)typeSuffix2, n)) {
                case 0 -> SnbtGrammar.convertFloat(ops, state, contents);
                case 1 -> SnbtGrammar.convertDouble(ops, state, contents);
                case -1 -> SnbtGrammar.convertDouble(ops, state, contents);
                default -> {
                    state.errorCollector().store(state.mark(), ERROR_EXPECTED_FLOAT_TYPE);
                    yield null;
                }
            };
        }
        catch (NumberFormatException e) {
            state.errorCollector().store(state.mark(), SnbtGrammar.createNumberParseError(e));
            return null;
        }
    }

    private static <T> @Nullable T convertFloat(DynamicOps<T> ops, ParseState<?> state, String contents) {
        float value = Float.parseFloat(contents);
        if (!Float.isFinite(value)) {
            state.errorCollector().store(state.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        }
        return (T)ops.createFloat(value);
    }

    private static <T> @Nullable T convertDouble(DynamicOps<T> ops, ParseState<?> state, String contents) {
        double value = Double.parseDouble(contents);
        if (!Double.isFinite(value)) {
            state.errorCollector().store(state.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        }
        return (T)ops.createDouble(value);
    }

    private static String joinList(List<String> list) {
        return switch (list.size()) {
            case 0 -> "";
            case 1 -> list.getFirst();
            default -> String.join((CharSequence)"", list);
        };
    }

    public static <T> Grammar<T> createParser(DynamicOps<T> ops) {
        Object trueValue = ops.createBoolean(true);
        Object falseValue = ops.createBoolean(false);
        Object emptyMapValue = ops.emptyMap();
        Object emptyList = ops.emptyList();
        Dictionary<StringReader> rules = new Dictionary<StringReader>();
        Atom sign = Atom.of("sign");
        rules.put(sign, Term.alternative(Term.sequence(StringReaderTerms.character('+'), Term.marker(sign, Sign.PLUS)), Term.sequence(StringReaderTerms.character('-'), Term.marker(sign, Sign.MINUS))), scope -> (Sign)((Object)((Object)scope.getOrThrow(sign))));
        Atom integerSuffix = Atom.of("integer_suffix");
        rules.put(integerSuffix, Term.alternative(Term.sequence(StringReaderTerms.characters('u', 'U'), Term.alternative(Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.BYTE))), Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.SHORT))), Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.INT))), Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.LONG))))), Term.sequence(StringReaderTerms.characters('s', 'S'), Term.alternative(Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.BYTE))), Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.SHORT))), Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.INT))), Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(integerSuffix, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.LONG))))), Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(integerSuffix, new IntegerSuffix(null, TypeSuffix.BYTE))), Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(integerSuffix, new IntegerSuffix(null, TypeSuffix.SHORT))), Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(integerSuffix, new IntegerSuffix(null, TypeSuffix.INT))), Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(integerSuffix, new IntegerSuffix(null, TypeSuffix.LONG)))), scope -> (IntegerSuffix)scope.getOrThrow(integerSuffix));
        Atom binaryNumeral = Atom.of("binary_numeral");
        rules.put(binaryNumeral, BINARY_NUMERAL);
        Atom decimalNumeral = Atom.of("decimal_numeral");
        rules.put(decimalNumeral, DECIMAL_NUMERAL);
        Atom hexNumeral = Atom.of("hex_numeral");
        rules.put(hexNumeral, HEX_NUMERAL);
        Atom integerLiteral = Atom.of("integer_literal");
        NamedRule integerLiteralRule = rules.put(integerLiteral, Term.sequence(Term.optional(rules.named(sign)), Term.alternative(Term.sequence(StringReaderTerms.character('0'), Term.cut(), Term.alternative(Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), rules.named(hexNumeral)), Term.sequence(StringReaderTerms.characters('b', 'B'), rules.named(binaryNumeral)), Term.sequence(rules.named(decimalNumeral), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)), Term.marker(decimalNumeral, "0"))), rules.named(decimalNumeral)), Term.optional(rules.named(integerSuffix))), scope -> {
            IntegerSuffix suffix = scope.getOrDefault(integerSuffix, IntegerSuffix.EMPTY);
            Sign signValue = scope.getOrDefault(sign, Sign.PLUS);
            String decimalContents = (String)scope.get(decimalNumeral);
            if (decimalContents != null) {
                return new IntegerLiteral(signValue, Base.DECIMAL, decimalContents, suffix);
            }
            String hexContents = (String)scope.get(hexNumeral);
            if (hexContents != null) {
                return new IntegerLiteral(signValue, Base.HEX, hexContents, suffix);
            }
            String binaryContents = (String)scope.getOrThrow(binaryNumeral);
            return new IntegerLiteral(signValue, Base.BINARY, binaryContents, suffix);
        });
        Atom floatTypeSuffix = Atom.of("float_type_suffix");
        rules.put(floatTypeSuffix, Term.alternative(Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(floatTypeSuffix, TypeSuffix.FLOAT)), Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(floatTypeSuffix, TypeSuffix.DOUBLE))), scope -> (TypeSuffix)((Object)((Object)scope.getOrThrow(floatTypeSuffix))));
        Atom floatExponentPart = Atom.of("float_exponent_part");
        rules.put(floatExponentPart, Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(rules.named(sign)), rules.named(decimalNumeral)), scope -> new Signed<String>(scope.getOrDefault(sign, Sign.PLUS), (String)scope.getOrThrow(decimalNumeral)));
        Atom floatWholePart = Atom.of("float_whole_part");
        Atom floatFractionPart = Atom.of("float_fraction_part");
        Atom floatLiteral = Atom.of("float_literal");
        rules.putComplex(floatLiteral, Term.sequence(Term.optional(rules.named(sign)), Term.alternative(Term.sequence(rules.namedWithAlias(decimalNumeral, floatWholePart), StringReaderTerms.character('.'), Term.cut(), Term.optional(rules.namedWithAlias(decimalNumeral, floatFractionPart)), Term.optional(rules.named(floatExponentPart)), Term.optional(rules.named(floatTypeSuffix))), Term.sequence(StringReaderTerms.character('.'), Term.cut(), rules.namedWithAlias(decimalNumeral, floatFractionPart), Term.optional(rules.named(floatExponentPart)), Term.optional(rules.named(floatTypeSuffix))), Term.sequence(rules.namedWithAlias(decimalNumeral, floatWholePart), rules.named(floatExponentPart), Term.cut(), Term.optional(rules.named(floatTypeSuffix))), Term.sequence(rules.namedWithAlias(decimalNumeral, floatWholePart), Term.optional(rules.named(floatExponentPart)), rules.named(floatTypeSuffix)))), state -> {
            Scope scope = state.scope();
            Sign wholeSign = scope.getOrDefault(sign, Sign.PLUS);
            String whole = (String)scope.get(floatWholePart);
            String fraction = (String)scope.get(floatFractionPart);
            Signed exponent = (Signed)scope.get(floatExponentPart);
            TypeSuffix typeSuffix = (TypeSuffix)((Object)((Object)scope.get(floatTypeSuffix)));
            return SnbtGrammar.createFloat(ops, wholeSign, whole, fraction, exponent, typeSuffix, state);
        });
        Atom stringHex2 = Atom.of("string_hex_2");
        rules.put(stringHex2, new SimpleHexLiteralParseRule(2));
        Atom stringHex4 = Atom.of("string_hex_4");
        rules.put(stringHex4, new SimpleHexLiteralParseRule(4));
        Atom stringHex8 = Atom.of("string_hex_8");
        rules.put(stringHex8, new SimpleHexLiteralParseRule(8));
        Atom stringUnicodeName = Atom.of("string_unicode_name");
        rules.put(stringUnicodeName, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
        Atom stringEscapeSequence = Atom.of("string_escape_sequence");
        rules.putComplex(stringEscapeSequence, Term.alternative(Term.sequence(StringReaderTerms.character('b'), Term.marker(stringEscapeSequence, "\b")), Term.sequence(StringReaderTerms.character('s'), Term.marker(stringEscapeSequence, " ")), Term.sequence(StringReaderTerms.character('t'), Term.marker(stringEscapeSequence, "\t")), Term.sequence(StringReaderTerms.character('n'), Term.marker(stringEscapeSequence, "\n")), Term.sequence(StringReaderTerms.character('f'), Term.marker(stringEscapeSequence, "\f")), Term.sequence(StringReaderTerms.character('r'), Term.marker(stringEscapeSequence, "\r")), Term.sequence(StringReaderTerms.character('\\'), Term.marker(stringEscapeSequence, "\\")), Term.sequence(StringReaderTerms.character('\''), Term.marker(stringEscapeSequence, "'")), Term.sequence(StringReaderTerms.character('\"'), Term.marker(stringEscapeSequence, "\"")), Term.sequence(StringReaderTerms.character('x'), rules.named(stringHex2)), Term.sequence(StringReaderTerms.character('u'), rules.named(stringHex4)), Term.sequence(StringReaderTerms.character('U'), rules.named(stringHex8)), Term.sequence(StringReaderTerms.character('N'), StringReaderTerms.character('{'), rules.named(stringUnicodeName), StringReaderTerms.character('}'))), state -> {
            int codePoint;
            Scope scope = state.scope();
            String plainEscape = (String)scope.getAny(stringEscapeSequence);
            if (plainEscape != null) {
                return plainEscape;
            }
            String hexEscape = (String)scope.getAny(stringHex2, stringHex4, stringHex8);
            if (hexEscape != null) {
                int codePoint2 = HexFormat.fromHexDigits(hexEscape);
                if (!Character.isValidCodePoint(codePoint2)) {
                    state.errorCollector().store(state.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", codePoint2)));
                    return null;
                }
                return Character.toString(codePoint2);
            }
            String character = (String)scope.getOrThrow(stringUnicodeName);
            try {
                codePoint = Character.codePointOf(character);
            }
            catch (IllegalArgumentException e) {
                state.errorCollector().store(state.mark(), ERROR_INVALID_CHARACTER_NAME);
                return null;
            }
            return Character.toString(codePoint);
        });
        Atom stringPlainContents = Atom.of("string_plain_contents");
        rules.put(stringPlainContents, PLAIN_STRING_CHUNK);
        Atom stringChunks = Atom.of("string_chunks");
        Atom stringContents = Atom.of("string_contents");
        Atom singleQuotedStringChunk = Atom.of("single_quoted_string_chunk");
        NamedRule singleQuotedStringChunkRule = rules.put(singleQuotedStringChunk, Term.alternative(rules.namedWithAlias(stringPlainContents, stringContents), Term.sequence(StringReaderTerms.character('\\'), rules.namedWithAlias(stringEscapeSequence, stringContents)), Term.sequence(StringReaderTerms.character('\"'), Term.marker(stringContents, "\""))), scope -> (String)scope.getOrThrow(stringContents));
        Atom singleQuotedStringContents = Atom.of("single_quoted_string_contents");
        rules.put(singleQuotedStringContents, Term.repeated(singleQuotedStringChunkRule, stringChunks), scope -> SnbtGrammar.joinList((List)scope.getOrThrow(stringChunks)));
        Atom doubleQuotedStringChunk = Atom.of("double_quoted_string_chunk");
        NamedRule doubleQuotedStringChunkRule = rules.put(doubleQuotedStringChunk, Term.alternative(rules.namedWithAlias(stringPlainContents, stringContents), Term.sequence(StringReaderTerms.character('\\'), rules.namedWithAlias(stringEscapeSequence, stringContents)), Term.sequence(StringReaderTerms.character('\''), Term.marker(stringContents, "'"))), scope -> (String)scope.getOrThrow(stringContents));
        Atom doubleQuotedStringContents = Atom.of("double_quoted_string_contents");
        rules.put(doubleQuotedStringContents, Term.repeated(doubleQuotedStringChunkRule, stringChunks), scope -> SnbtGrammar.joinList((List)scope.getOrThrow(stringChunks)));
        Atom quotedStringLiteral = Atom.of("quoted_string_literal");
        rules.put(quotedStringLiteral, Term.alternative(Term.sequence(StringReaderTerms.character('\"'), Term.cut(), Term.optional(rules.namedWithAlias(doubleQuotedStringContents, stringContents)), StringReaderTerms.character('\"')), Term.sequence(StringReaderTerms.character('\''), Term.optional(rules.namedWithAlias(singleQuotedStringContents, stringContents)), StringReaderTerms.character('\''))), scope -> (String)scope.getOrThrow(stringContents));
        Atom unquotedString = Atom.of("unquoted_string");
        rules.put(unquotedString, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
        Atom literal = Atom.of("literal");
        Atom argumentList = Atom.of("arguments");
        rules.put(argumentList, Term.repeatedWithTrailingSeparator(rules.forward(literal), argumentList, StringReaderTerms.character(',')), scope -> (List)scope.getOrThrow(argumentList));
        Atom unquotedStringOrBuiltIn = Atom.of("unquoted_string_or_builtin");
        rules.putComplex(unquotedStringOrBuiltIn, Term.sequence(rules.named(unquotedString), Term.optional(Term.sequence(StringReaderTerms.character('('), rules.named(argumentList), StringReaderTerms.character(')')))), state -> {
            Scope scope = state.scope();
            String contents = (String)scope.getOrThrow(unquotedString);
            if (contents.isEmpty() || !SnbtGrammar.isAllowedToStartUnquotedString(contents.charAt(0))) {
                state.errorCollector().store(state.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
                return null;
            }
            List arguments = (List)scope.get(argumentList);
            if (arguments != null) {
                SnbtOperations.BuiltinKey key = new SnbtOperations.BuiltinKey(contents, arguments.size());
                SnbtOperations.BuiltinOperation operation = SnbtOperations.BUILTIN_OPERATIONS.get(key);
                if (operation != null) {
                    return operation.run(ops, arguments, state);
                }
                state.errorCollector().store(state.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, key.toString()));
                return null;
            }
            if (contents.equalsIgnoreCase("true")) {
                return trueValue;
            }
            if (contents.equalsIgnoreCase("false")) {
                return falseValue;
            }
            return ops.createString(contents);
        });
        Atom mapKey = Atom.of("map_key");
        rules.put(mapKey, Term.alternative(rules.named(quotedStringLiteral), rules.named(unquotedString)), scope -> (String)scope.getAnyOrThrow(quotedStringLiteral, unquotedString));
        Atom mapEntry = Atom.of("map_entry");
        NamedRule mapEntryRule = rules.putComplex(mapEntry, Term.sequence(rules.named(mapKey), StringReaderTerms.character(':'), rules.named(literal)), state -> {
            Scope scope = state.scope();
            String key = (String)scope.getOrThrow(mapKey);
            if (key.isEmpty()) {
                state.errorCollector().store(state.mark(), ERROR_EMPTY_KEY);
                return null;
            }
            Object value = scope.getOrThrow(literal);
            return Map.entry(key, value);
        });
        Atom mapEntries = Atom.of("map_entries");
        rules.put(mapEntries, Term.repeatedWithTrailingSeparator(mapEntryRule, mapEntries, StringReaderTerms.character(',')), scope -> (List)scope.getOrThrow(mapEntries));
        Atom mapLiteral = Atom.of("map_literal");
        rules.put(mapLiteral, Term.sequence(StringReaderTerms.character('{'), rules.named(mapEntries), StringReaderTerms.character('}')), scope -> {
            List entries = (List)scope.getOrThrow(mapEntries);
            if (entries.isEmpty()) {
                return emptyMapValue;
            }
            ImmutableMap.Builder builder = ImmutableMap.builderWithExpectedSize((int)entries.size());
            for (Map.Entry e : entries) {
                builder.put(ops.createString((String)e.getKey()), e.getValue());
            }
            return ops.createMap((Map)builder.buildKeepingLast());
        });
        Atom listEntries = Atom.of("list_entries");
        rules.put(listEntries, Term.repeatedWithTrailingSeparator(rules.forward(literal), listEntries, StringReaderTerms.character(',')), scope -> (List)scope.getOrThrow(listEntries));
        Atom arrayPrefix = Atom.of("array_prefix");
        rules.put(arrayPrefix, Term.alternative(Term.sequence(StringReaderTerms.character('B'), Term.marker(arrayPrefix, ArrayPrefix.BYTE)), Term.sequence(StringReaderTerms.character('L'), Term.marker(arrayPrefix, ArrayPrefix.LONG)), Term.sequence(StringReaderTerms.character('I'), Term.marker(arrayPrefix, ArrayPrefix.INT))), scope -> (ArrayPrefix)((Object)((Object)scope.getOrThrow(arrayPrefix))));
        Atom intArrayEntries = Atom.of("int_array_entries");
        rules.put(intArrayEntries, Term.repeatedWithTrailingSeparator(integerLiteralRule, intArrayEntries, StringReaderTerms.character(',')), scope -> (List)scope.getOrThrow(intArrayEntries));
        Atom listLiteral = Atom.of("list_literal");
        rules.putComplex(listLiteral, Term.sequence(StringReaderTerms.character('['), Term.alternative(Term.sequence(rules.named(arrayPrefix), StringReaderTerms.character(';'), rules.named(intArrayEntries)), rules.named(listEntries)), StringReaderTerms.character(']')), state -> {
            Scope scope = state.scope();
            ArrayPrefix arrayType = (ArrayPrefix)((Object)((Object)scope.get(arrayPrefix)));
            if (arrayType != null) {
                List entries = (List)scope.getOrThrow(intArrayEntries);
                return entries.isEmpty() ? arrayType.create(ops) : arrayType.create(ops, entries, state);
            }
            List entries = (List)scope.getOrThrow(listEntries);
            return entries.isEmpty() ? emptyList : ops.createList(entries.stream());
        });
        NamedRule literalRule = rules.putComplex(literal, Term.alternative(Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(rules.namedWithAlias(floatLiteral, literal), rules.named(integerLiteral))), Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('\"', '\'')), Term.cut(), rules.named(quotedStringLiteral)), Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), rules.namedWithAlias(mapLiteral, literal)), Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), rules.namedWithAlias(listLiteral, literal)), rules.namedWithAlias(unquotedStringOrBuiltIn, literal)), state -> {
            Scope scope = state.scope();
            String quotedString = (String)scope.get(quotedStringLiteral);
            if (quotedString != null) {
                return ops.createString(quotedString);
            }
            IntegerLiteral integer = (IntegerLiteral)scope.get(integerLiteral);
            if (integer != null) {
                return integer.create(ops, state);
            }
            return scope.getOrThrow(literal);
        });
        return new Grammar<Object>(rules, literalRule);
    }

    private static enum Sign {
        PLUS,
        MINUS;


        public void append(StringBuilder output) {
            if (this == MINUS) {
                output.append("-");
            }
        }
    }

    private record Signed<T>(Sign sign, T value) {
    }

    private static enum TypeSuffix {
        FLOAT,
        DOUBLE,
        BYTE,
        SHORT,
        INT,
        LONG;

    }

    private record IntegerSuffix(@Nullable SignedPrefix signed, @Nullable TypeSuffix type) {
        public static final IntegerSuffix EMPTY = new IntegerSuffix(null, null);
    }

    private static enum SignedPrefix {
        SIGNED,
        UNSIGNED;

    }

    private static class SimpleHexLiteralParseRule
    extends GreedyPredicateParseRule {
        public SimpleHexLiteralParseRule(int size) {
            super(size, size, DelayedException.create(ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(size)));
        }

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    }

    private static enum ArrayPrefix {
        BYTE(TypeSuffix.BYTE, new TypeSuffix[0]){
            private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

            @Override
            public <T> T create(DynamicOps<T> ops) {
                return (T)ops.createByteList(EMPTY_BUFFER);
            }

            @Override
            public <T> @Nullable T create(DynamicOps<T> ops, List<IntegerLiteral> entries, ParseState<?> state) {
                ByteArrayList result = new ByteArrayList();
                for (IntegerLiteral entry : entries) {
                    Number parsedNumber = this.buildNumber(entry, state);
                    if (parsedNumber == null) {
                        return null;
                    }
                    result.add(parsedNumber.byteValue());
                }
                return (T)ops.createByteList(ByteBuffer.wrap(result.toByteArray()));
            }
        }
        ,
        INT(TypeSuffix.INT, new TypeSuffix[]{TypeSuffix.BYTE, TypeSuffix.SHORT}){

            @Override
            public <T> T create(DynamicOps<T> ops) {
                return (T)ops.createIntList(IntStream.empty());
            }

            @Override
            public <T> @Nullable T create(DynamicOps<T> ops, List<IntegerLiteral> entries, ParseState<?> state) {
                IntStream.Builder result = IntStream.builder();
                for (IntegerLiteral entry : entries) {
                    Number parsedNumber = this.buildNumber(entry, state);
                    if (parsedNumber == null) {
                        return null;
                    }
                    result.add(parsedNumber.intValue());
                }
                return (T)ops.createIntList(result.build());
            }
        }
        ,
        LONG(TypeSuffix.LONG, new TypeSuffix[]{TypeSuffix.BYTE, TypeSuffix.SHORT, TypeSuffix.INT}){

            @Override
            public <T> T create(DynamicOps<T> ops) {
                return (T)ops.createLongList(LongStream.empty());
            }

            @Override
            public <T> @Nullable T create(DynamicOps<T> ops, List<IntegerLiteral> entries, ParseState<?> state) {
                LongStream.Builder result = LongStream.builder();
                for (IntegerLiteral entry : entries) {
                    Number parsedNumber = this.buildNumber(entry, state);
                    if (parsedNumber == null) {
                        return null;
                    }
                    result.add(parsedNumber.longValue());
                }
                return (T)ops.createLongList(result.build());
            }
        };

        private final TypeSuffix defaultType;
        private final Set<TypeSuffix> additionalTypes;

        private ArrayPrefix(TypeSuffix defaultType, TypeSuffix ... additionalTypes) {
            this.additionalTypes = Set.of(additionalTypes);
            this.defaultType = defaultType;
        }

        public boolean isAllowed(TypeSuffix type) {
            return type == this.defaultType || this.additionalTypes.contains((Object)type);
        }

        public abstract <T> T create(DynamicOps<T> var1);

        public abstract <T> @Nullable T create(DynamicOps<T> var1, List<IntegerLiteral> var2, ParseState<?> var3);

        protected @Nullable Number buildNumber(IntegerLiteral entry, ParseState<?> state) {
            TypeSuffix actualType = this.computeType(entry.suffix);
            if (actualType == null) {
                state.errorCollector().store(state.mark(), ERROR_INVALID_ARRAY_ELEMENT_TYPE);
                return null;
            }
            return (Number)entry.create(JavaOps.INSTANCE, actualType, state);
        }

        private @Nullable TypeSuffix computeType(IntegerSuffix value) {
            TypeSuffix type = value.type();
            if (type == null) {
                return this.defaultType;
            }
            if (!this.isAllowed(type)) {
                return null;
            }
            return type;
        }
    }

    private record IntegerLiteral(Sign sign, Base base, String digits, IntegerSuffix suffix) {
        private SignedPrefix signedOrDefault() {
            if (this.suffix.signed != null) {
                return this.suffix.signed;
            }
            return switch (this.base.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0, 2 -> SignedPrefix.UNSIGNED;
                case 1 -> SignedPrefix.SIGNED;
            };
        }

        private String cleanupDigits(Sign sign) {
            boolean needsUnderscoreRemoval = SnbtGrammar.needsUnderscoreRemoval(this.digits);
            if (sign == Sign.MINUS || needsUnderscoreRemoval) {
                StringBuilder result = new StringBuilder();
                sign.append(result);
                SnbtGrammar.cleanAndAppend(result, this.digits, needsUnderscoreRemoval);
                return result.toString();
            }
            return this.digits;
        }

        public <T> @Nullable T create(DynamicOps<T> ops, ParseState<?> state) {
            return this.create(ops, Objects.requireNonNullElse(this.suffix.type, TypeSuffix.INT), state);
        }

        public <T> @Nullable T create(DynamicOps<T> ops, TypeSuffix type, ParseState<?> state) {
            boolean isSigned;
            boolean bl = isSigned = this.signedOrDefault() == SignedPrefix.SIGNED;
            if (!isSigned && this.sign == Sign.MINUS) {
                state.errorCollector().store(state.mark(), ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
                return null;
            }
            String fixedDigits = this.cleanupDigits(this.sign);
            int radix = switch (this.base.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> 2;
                case 1 -> 10;
                case 2 -> 16;
            };
            try {
                if (isSigned) {
                    return (T)(switch (type.ordinal()) {
                        case 2 -> ops.createByte(Byte.parseByte(fixedDigits, radix));
                        case 3 -> ops.createShort(Short.parseShort(fixedDigits, radix));
                        case 4 -> ops.createInt(Integer.parseInt(fixedDigits, radix));
                        case 5 -> ops.createLong(Long.parseLong(fixedDigits, radix));
                        default -> {
                            state.errorCollector().store(state.mark(), ERROR_EXPECTED_INTEGER_TYPE);
                            yield null;
                        }
                    });
                }
                return (T)(switch (type.ordinal()) {
                    case 2 -> ops.createByte(UnsignedBytes.parseUnsignedByte((String)fixedDigits, (int)radix));
                    case 3 -> ops.createShort(SnbtGrammar.parseUnsignedShort(fixedDigits, radix));
                    case 4 -> ops.createInt(Integer.parseUnsignedInt(fixedDigits, radix));
                    case 5 -> ops.createLong(Long.parseUnsignedLong(fixedDigits, radix));
                    default -> {
                        state.errorCollector().store(state.mark(), ERROR_EXPECTED_INTEGER_TYPE);
                        yield null;
                    }
                });
            }
            catch (NumberFormatException e) {
                state.errorCollector().store(state.mark(), SnbtGrammar.createNumberParseError(e));
                return null;
            }
        }
    }

    private static enum Base {
        BINARY,
        DECIMAL,
        HEX;

    }
}

