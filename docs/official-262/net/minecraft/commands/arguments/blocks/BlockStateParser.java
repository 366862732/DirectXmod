/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.datafixers.util.Either
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockStateParser {
    public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType((Message)Component.translatable("argument.block.tag.disallowed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType(id -> Component.translatableEscape("argument.block.id.invalid", id));
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> Component.translatableEscape("argument.block.property.unknown", block, property));
    public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> Component.translatableEscape("argument.block.property.duplicate", property, block));
    public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType((block, property, value) -> Component.translatableEscape("argument.block.property.invalid", block, value, property));
    public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType((block, property) -> Component.translatableEscape("argument.block.property.novalue", property, block));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType((Message)Component.translatable("argument.block.property.unclosed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(tag -> Component.translatableEscape("arguments.block.tag.unknown", tag));
    private static final char SYNTAX_START_PROPERTIES = '[';
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_END_PROPERTIES = ']';
    private static final char SYNTAX_EQUALS = '=';
    private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
    private static final char SYNTAX_TAG = '#';
    private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
    private final HolderLookup<Block> blocks;
    private final StringReader reader;
    private final boolean forTesting;
    private final boolean allowNbt;
    private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
    private final Map<String, String> vagueProperties = Maps.newHashMap();
    private Identifier id = Identifier.withDefaultNamespace("");
    private @Nullable StateDefinition<Block, BlockState> definition;
    private @Nullable BlockState state;
    private @Nullable CompoundTag nbt;
    private @Nullable HolderSet<Block> tag;
    private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    private BlockStateParser(HolderLookup<Block> blocks, StringReader reader, boolean forTesting, boolean allowNbt) {
        this.blocks = blocks;
        this.reader = reader;
        this.forTesting = forTesting;
        this.allowNbt = allowNbt;
    }

    public static BlockResult parseForBlock(HolderLookup<Block> blocks, String value, boolean allowNbt) throws CommandSyntaxException {
        return BlockStateParser.parseForBlock(blocks, new StringReader(value), allowNbt);
    }

    public static BlockResult parseForBlock(HolderLookup<Block> blocks, StringReader reader, boolean allowNbt) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        try {
            BlockStateParser parser = new BlockStateParser(blocks, reader, false, allowNbt);
            parser.parse();
            return new BlockResult(parser.state, parser.properties, parser.nbt);
        }
        catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
        }
    }

    public static Either<BlockResult, TagResult> parseForTesting(HolderLookup<Block> blocks, String value, boolean allowNbt) throws CommandSyntaxException {
        return BlockStateParser.parseForTesting(blocks, new StringReader(value), allowNbt);
    }

    public static Either<BlockResult, TagResult> parseForTesting(HolderLookup<Block> blocks, StringReader reader, boolean allowNbt) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        try {
            BlockStateParser parser = new BlockStateParser(blocks, reader, true, allowNbt);
            parser.parse();
            if (parser.tag != null) {
                return Either.right((Object)new TagResult(parser.tag, parser.vagueProperties, parser.nbt));
            }
            return Either.left((Object)new BlockResult(parser.state, parser.properties, parser.nbt));
        }
        catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
        }
    }

    public static CompletableFuture<Suggestions> fillSuggestions(HolderLookup<Block> blocks, SuggestionsBuilder builder, boolean forTesting, boolean allowNbt) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        BlockStateParser parser = new BlockStateParser(blocks, reader, forTesting, allowNbt);
        try {
            parser.parse();
        }
        catch (CommandSyntaxException commandSyntaxException) {
            // empty catch block
        }
        return parser.suggestions.apply(builder.createOffset(reader.getCursor()));
    }

    private void parse() throws CommandSyntaxException {
        this.suggestions = this.forTesting ? this::suggestBlockIdOrTag : this::suggestItem;
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
            this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readVagueProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        } else {
            this.readBlock();
            this.suggestions = this::suggestOpenPropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        }
        if (this.allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }
    }

    private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }
        return this.suggestPropertyName(builder);
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }
        return this.suggestVaguePropertyName(builder);
    }

    private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Property<?> property : this.state.getProperties()) {
            if (this.properties.containsKey(property) || !property.getName().startsWith(prefix)) continue;
            builder.suggest(property.getName() + "=");
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (this.tag != null) {
            for (Holder holder : this.tag) {
                for (Property<?> property : ((Block)holder.value()).getStateDefinition().getProperties()) {
                    if (this.vagueProperties.containsKey(property.getName()) || !property.getName().startsWith(prefix)) continue;
                    builder.suggest(property.getName() + "=");
                }
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty() && this.hasBlockEntity()) {
            builder.suggest(String.valueOf('{'));
        }
        return builder.buildFuture();
    }

    private boolean hasBlockEntity() {
        if (this.state != null) {
            return this.state.hasBlockEntity();
        }
        if (this.tag != null) {
            for (Holder holder : this.tag) {
                if (!((Block)holder.value()).defaultBlockState().hasBlockEntity()) continue;
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('='));
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }
        if (builder.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
            builder.suggest(String.valueOf(','));
        }
        return builder.buildFuture();
    }

    private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder builder, Property<T> property) {
        for (Comparable value : property.getPossibleValues()) {
            if (value instanceof Integer) {
                Integer v = (Integer)value;
                builder.suggest(v.intValue());
                continue;
            }
            builder.suggest(property.getName(value));
        }
        return builder;
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder builder, String key) {
        boolean hasMoreProperties = false;
        if (this.tag != null) {
            block0: for (Holder holder : this.tag) {
                Block block = (Block)holder.value();
                Property<?> property = block.getStateDefinition().getProperty(key);
                if (property != null) {
                    BlockStateParser.addSuggestions(builder, property);
                }
                if (hasMoreProperties) continue;
                for (Property<?> prop : block.getStateDefinition().getProperties()) {
                    if (this.vagueProperties.containsKey(prop.getName())) continue;
                    hasMoreProperties = true;
                    continue block0;
                }
            }
        }
        if (hasMoreProperties) {
            builder.suggest(String.valueOf(','));
        }
        builder.suggest(String.valueOf(']'));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty() && this.tag != null) {
            Holder blockHolder;
            Block block;
            boolean hasProperties = false;
            boolean hasEntity = false;
            Iterator iterator = this.tag.iterator();
            while (!(!iterator.hasNext() || (hasProperties |= !(block = (Block)(blockHolder = (Holder)iterator.next()).value()).getStateDefinition().getProperties().isEmpty()) && (hasEntity |= block.defaultBlockState().hasBlockEntity()))) {
            }
            if (hasProperties) {
                builder.suggest(String.valueOf('['));
            }
            if (hasEntity) {
                builder.suggest(String.valueOf('{'));
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            if (!this.definition.getProperties().isEmpty()) {
                builder.suggest(String.valueOf('['));
            }
            if (this.state.hasBlockEntity()) {
                builder.suggest(String.valueOf('{'));
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), builder, String.valueOf('#'));
    }

    private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::identifier), builder);
    }

    private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder builder) {
        this.suggestTag(builder);
        this.suggestItem(builder);
        return builder.buildFuture();
    }

    private void readBlock() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        this.id = Identifier.read(this.reader);
        Block block = this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
            this.reader.setCursor(start);
            return ERROR_UNKNOWN_BLOCK.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString());
        }).value();
        this.definition = block.getStateDefinition();
        this.state = block.defaultBlockState();
    }

    private void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw ERROR_NO_TAGS_ALLOWED.createWithContext((ImmutableStringReader)this.reader);
        }
        int start = this.reader.getCursor();
        this.reader.expect('#');
        this.suggestions = this::suggestTag;
        Identifier id = Identifier.read(this.reader);
        this.tag = this.blocks.get(TagKey.create(Registries.BLOCK, id)).orElseThrow(() -> {
            this.reader.setCursor(start);
            return ERROR_UNKNOWN_TAG.createWithContext((ImmutableStringReader)this.reader, (Object)id.toString());
        });
    }

    private void readProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestPropertyNameOrEnd;
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int keyStart = this.reader.getCursor();
            String key = this.reader.readString();
            Property<?> property = this.definition.getProperty(key);
            if (property == null) {
                this.reader.setCursor(keyStart);
                throw ERROR_UNKNOWN_PROPERTY.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)key);
            }
            if (this.properties.containsKey(property)) {
                this.reader.setCursor(keyStart);
                throw ERROR_DUPLICATE_PROPERTY.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)key);
            }
            this.reader.skipWhitespace();
            this.suggestions = this::suggestEquals;
            if (!this.reader.canRead() || this.reader.peek() != '=') {
                throw ERROR_EXPECTED_VALUE.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)key);
            }
            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggestions = builder -> BlockStateParser.addSuggestions(builder, property).buildFuture();
            int start = this.reader.getCursor();
            this.setValue(property, this.reader.readString(), start);
            this.suggestions = this::suggestNextPropertyOrEnd;
            this.reader.skipWhitespace();
            if (!this.reader.canRead()) continue;
            if (this.reader.peek() == ',') {
                this.reader.skip();
                this.suggestions = this::suggestPropertyName;
                continue;
            }
            if (this.reader.peek() == ']') break;
            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext((ImmutableStringReader)this.reader);
        }
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext((ImmutableStringReader)this.reader);
        }
        this.reader.skip();
    }

    private void readVagueProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestVaguePropertyNameOrEnd;
        int valueStart = -1;
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int keyStart = this.reader.getCursor();
            String key = this.reader.readString();
            if (this.vagueProperties.containsKey(key)) {
                this.reader.setCursor(keyStart);
                throw ERROR_DUPLICATE_PROPERTY.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)key);
            }
            this.reader.skipWhitespace();
            if (!this.reader.canRead() || this.reader.peek() != '=') {
                this.reader.setCursor(keyStart);
                throw ERROR_EXPECTED_VALUE.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)key);
            }
            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggestions = builder -> this.suggestVaguePropertyValue((SuggestionsBuilder)builder, key);
            valueStart = this.reader.getCursor();
            String value = this.reader.readString();
            this.vagueProperties.put(key, value);
            this.reader.skipWhitespace();
            if (!this.reader.canRead()) continue;
            valueStart = -1;
            if (this.reader.peek() == ',') {
                this.reader.skip();
                this.suggestions = this::suggestVaguePropertyName;
                continue;
            }
            if (this.reader.peek() == ']') break;
            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext((ImmutableStringReader)this.reader);
        }
        if (!this.reader.canRead()) {
            if (valueStart >= 0) {
                this.reader.setCursor(valueStart);
            }
            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext((ImmutableStringReader)this.reader);
        }
        this.reader.skip();
    }

    private void readNbt() throws CommandSyntaxException {
        this.nbt = TagParser.parseCompoundAsArgument(this.reader);
    }

    private <T extends Comparable<T>> void setValue(Property<T> property, String raw, int start) throws CommandSyntaxException {
        Optional<T> value = property.getValue(raw);
        if (!value.isPresent()) {
            this.reader.setCursor(start);
            throw ERROR_INVALID_VALUE.createWithContext((ImmutableStringReader)this.reader, (Object)this.id.toString(), (Object)property.getName(), (Object)raw);
        }
        this.state = (BlockState)this.state.setValue(property, (Comparable)value.get());
        this.properties.put(property, (Comparable)value.get());
    }

    public static String serialize(BlockState state) {
        final StringBuilder result = new StringBuilder(state.typeHolder().unwrapKey().map(r -> r.identifier().toString()).orElse("air"));
        if (!state.isSingletonState()) {
            result.append('[');
            state.getValues().forEach(new Consumer<Property.Value<?>>(){
                private boolean separate = false;

                @Override
                public void accept(Property.Value<?> value) {
                    if (this.separate) {
                        result.append(',');
                    }
                    result.append(value.property().getName());
                    result.append('=');
                    result.append(value.valueName());
                    this.separate = true;
                }
            });
            result.append(']');
        }
        return result.toString();
    }

    public record BlockResult(BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
    }

    public record TagResult(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
    }
}

