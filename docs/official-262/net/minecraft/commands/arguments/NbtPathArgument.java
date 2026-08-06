/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableBoolean
 */
package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NbtPathArgument
implements ArgumentType<NbtPath> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
    public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType((Message)Component.translatable("arguments.nbtpath.node.invalid"));
    public static final SimpleCommandExceptionType ERROR_DATA_TOO_DEEP = new SimpleCommandExceptionType((Message)Component.translatable("arguments.nbtpath.too_deep"));
    public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType(path -> Component.translatableEscape("arguments.nbtpath.nothing_found", path));
    private static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType(node -> Component.translatableEscape("commands.data.modify.expected_list", node));
    private static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType(node -> Component.translatableEscape("commands.data.modify.invalid_index", node));
    private static final char INDEX_MATCH_START = '[';
    private static final char INDEX_MATCH_END = ']';
    private static final char KEY_MATCH_START = '{';
    private static final char KEY_MATCH_END = '}';
    private static final char QUOTED_KEY_START = '\"';
    private static final char SINGLE_QUOTED_KEY_START = '\'';

    public static NbtPathArgument nbtPath() {
        return new NbtPathArgument();
    }

    public static NbtPath getPath(CommandContext<CommandSourceStack> context, String name) {
        return (NbtPath)context.getArgument(name, NbtPath.class);
    }

    public NbtPath parse(StringReader reader) throws CommandSyntaxException {
        ArrayList nodes = Lists.newArrayList();
        int start = reader.getCursor();
        Object2IntOpenHashMap nodeToOriginalPosition = new Object2IntOpenHashMap();
        boolean firstNode = true;
        while (reader.canRead() && reader.peek() != ' ') {
            char next;
            Node node = NbtPathArgument.parseNode(reader, firstNode);
            nodes.add(node);
            nodeToOriginalPosition.put((Object)node, reader.getCursor() - start);
            firstNode = false;
            if (!reader.canRead() || (next = reader.peek()) == ' ' || next == '[' || next == '{') continue;
            reader.expect('.');
        }
        return new NbtPath(reader.getString().substring(start, reader.getCursor()), nodes.toArray(new Node[0]), (Object2IntMap<Node>)nodeToOriginalPosition);
    }

    private static Node parseNode(StringReader reader, boolean firstNode) throws CommandSyntaxException {
        return switch (reader.peek()) {
            case '{' -> {
                if (!firstNode) {
                    throw ERROR_INVALID_NODE.createWithContext((ImmutableStringReader)reader);
                }
                CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
                yield new MatchRootObjectNode(pattern);
            }
            case '[' -> {
                reader.skip();
                char next = reader.peek();
                if (next == '{') {
                    CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
                    reader.expect(']');
                    yield new MatchElementNode(pattern);
                }
                if (next == ']') {
                    reader.skip();
                    yield AllElementsNode.INSTANCE;
                }
                int index = reader.readInt();
                reader.expect(']');
                yield new IndexedElementNode(index);
            }
            case '\"', '\'' -> NbtPathArgument.readObjectNode(reader, reader.readString());
            default -> NbtPathArgument.readObjectNode(reader, NbtPathArgument.readUnquotedName(reader));
        };
    }

    private static Node readObjectNode(StringReader reader, String name) throws CommandSyntaxException {
        if (name.isEmpty()) {
            throw ERROR_INVALID_NODE.createWithContext((ImmutableStringReader)reader);
        }
        if (reader.canRead() && reader.peek() == '{') {
            CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
            return new MatchObjectNode(name, pattern);
        }
        return new CompoundChildNode(name);
    }

    private static String readUnquotedName(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && NbtPathArgument.isAllowedInUnquotedName(reader.peek())) {
            reader.skip();
        }
        if (reader.getCursor() == start) {
            throw ERROR_INVALID_NODE.createWithContext((ImmutableStringReader)reader);
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static boolean isAllowedInUnquotedName(char c) {
        return c != ' ' && c != '\"' && c != '\'' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
    }

    private static Predicate<Tag> createTagPredicate(CompoundTag pattern) {
        return tag -> NbtUtils.compareNbt(pattern, tag, true);
    }

    public static class NbtPath {
        private final String original;
        private final Object2IntMap<Node> nodeToOriginalPosition;
        private final Node[] nodes;
        public static final Codec<NbtPath> CODEC = Codec.STRING.comapFlatMap(string -> {
            try {
                NbtPath parsed = new NbtPathArgument().parse(new StringReader(string));
                return DataResult.success((Object)parsed);
            }
            catch (CommandSyntaxException e) {
                return DataResult.error(() -> "Failed to parse path " + string + ": " + e.getMessage());
            }
        }, NbtPath::asString);

        public static NbtPath of(String string) throws CommandSyntaxException {
            return new NbtPathArgument().parse(new StringReader(string));
        }

        public NbtPath(String original, Node[] nodes, Object2IntMap<Node> nodeToOriginalPosition) {
            this.original = original;
            this.nodes = nodes;
            this.nodeToOriginalPosition = nodeToOriginalPosition;
        }

        public List<Tag> get(Tag tag) throws CommandSyntaxException {
            List<Tag> result = Collections.singletonList(tag);
            for (Node node : this.nodes) {
                if (!(result = node.get(result)).isEmpty()) continue;
                throw this.createNotFoundException(node);
            }
            return result;
        }

        public int countMatching(Tag tag) {
            List<Tag> result = Collections.singletonList(tag);
            for (Node node : this.nodes) {
                if (!(result = node.get(result)).isEmpty()) continue;
                return 0;
            }
            return result.size();
        }

        private List<Tag> getOrCreateParents(Tag tag) throws CommandSyntaxException {
            List<Tag> result = Collections.singletonList(tag);
            for (int i = 0; i < this.nodes.length - 1; ++i) {
                Node node = this.nodes[i];
                int next = i + 1;
                if (!(result = node.getOrCreate(result, this.nodes[next]::createPreferredParentTag)).isEmpty()) continue;
                throw this.createNotFoundException(node);
            }
            return result;
        }

        public List<Tag> getOrCreate(Tag tag, Supplier<Tag> newTagValue) throws CommandSyntaxException {
            List<Tag> result = this.getOrCreateParents(tag);
            Node lastNode = this.nodes[this.nodes.length - 1];
            return lastNode.getOrCreate(result, newTagValue);
        }

        private static int apply(List<Tag> targets, Function<Tag, Integer> operation) {
            return targets.stream().map(operation).reduce(0, (a, b) -> a + b);
        }

        public static boolean isTooDeep(Tag tag, int depth) {
            block4: {
                block3: {
                    if (depth >= 512) {
                        return true;
                    }
                    if (!(tag instanceof CompoundTag)) break block3;
                    CompoundTag compound = (CompoundTag)tag;
                    for (Tag child : compound.values()) {
                        if (!NbtPath.isTooDeep(child, depth + 1)) continue;
                        return true;
                    }
                    break block4;
                }
                if (!(tag instanceof ListTag)) break block4;
                ListTag list = (ListTag)tag;
                for (Tag listEntry : list) {
                    if (!NbtPath.isTooDeep(listEntry, depth + 1)) continue;
                    return true;
                }
            }
            return false;
        }

        public int set(Tag tag, Tag toAdd) throws CommandSyntaxException {
            if (NbtPath.isTooDeep(toAdd, this.estimatePathDepth())) {
                throw ERROR_DATA_TOO_DEEP.create();
            }
            Tag firstCopy = toAdd.copy();
            List<Tag> result = this.getOrCreateParents(tag);
            if (result.isEmpty()) {
                return 0;
            }
            Node lastNode = this.nodes[this.nodes.length - 1];
            MutableBoolean usedFirstCopy = new MutableBoolean(false);
            return NbtPath.apply(result, t -> lastNode.setTag((Tag)t, () -> {
                if (usedFirstCopy.isFalse()) {
                    usedFirstCopy.setTrue();
                    return firstCopy;
                }
                return firstCopy.copy();
            }));
        }

        private int estimatePathDepth() {
            return this.nodes.length;
        }

        public int insert(int index, CompoundTag target, List<Tag> toInsert) throws CommandSyntaxException {
            ArrayList<Tag> toInsertCopy = new ArrayList<Tag>(toInsert.size());
            for (Tag tag : toInsert) {
                Tag copy = tag.copy();
                toInsertCopy.add(copy);
                if (!NbtPath.isTooDeep(copy, this.estimatePathDepth())) continue;
                throw ERROR_DATA_TOO_DEEP.create();
            }
            List<Tag> targets = this.getOrCreate(target, ListTag::new);
            int modifiedCount = 0;
            boolean usedFirst = false;
            for (Tag targetTag : targets) {
                if (!(targetTag instanceof CollectionTag)) {
                    throw ERROR_EXPECTED_LIST.create((Object)targetTag);
                }
                CollectionTag targetList = (CollectionTag)targetTag;
                boolean modified = false;
                int actualIndex = index < 0 ? targetList.size() + index + 1 : index;
                for (Tag sourceTag : toInsertCopy) {
                    try {
                        if (!targetList.addTag(actualIndex, usedFirst ? sourceTag.copy() : sourceTag)) continue;
                        ++actualIndex;
                        modified = true;
                    }
                    catch (IndexOutOfBoundsException e) {
                        throw ERROR_INVALID_INDEX.create((Object)actualIndex);
                    }
                }
                usedFirst = true;
                modifiedCount += modified ? 1 : 0;
            }
            return modifiedCount;
        }

        public int remove(Tag tag) {
            List<Tag> result = Collections.singletonList(tag);
            for (int i = 0; i < this.nodes.length - 1; ++i) {
                result = this.nodes[i].get(result);
            }
            Node lastNode = this.nodes[this.nodes.length - 1];
            return NbtPath.apply(result, lastNode::removeTag);
        }

        private CommandSyntaxException createNotFoundException(Node node) {
            int index = this.nodeToOriginalPosition.getInt((Object)node);
            return ERROR_NOTHING_FOUND.create((Object)this.original.substring(0, index));
        }

        public String toString() {
            return this.original;
        }

        public String asString() {
            return this.original;
        }
    }

    private static interface Node {
        public void getTag(Tag var1, List<Tag> var2);

        public void getOrCreateTag(Tag var1, Supplier<Tag> var2, List<Tag> var3);

        public Tag createPreferredParentTag();

        public int setTag(Tag var1, Supplier<Tag> var2);

        public int removeTag(Tag var1);

        default public List<Tag> get(List<Tag> tags) {
            return this.collect(tags, this::getTag);
        }

        default public List<Tag> getOrCreate(List<Tag> tags, Supplier<Tag> child) {
            return this.collect(tags, (tag, output) -> this.getOrCreateTag((Tag)tag, child, (List<Tag>)output));
        }

        default public List<Tag> collect(List<Tag> tags, BiConsumer<Tag, List<Tag>> collector) {
            ArrayList result = Lists.newArrayList();
            for (Tag tag : tags) {
                collector.accept(tag, result);
            }
            return result;
        }
    }

    private static class MatchRootObjectNode
    implements Node {
        private final Predicate<Tag> predicate;

        public MatchRootObjectNode(CompoundTag pattern) {
            this.predicate = NbtPathArgument.createTagPredicate(pattern);
        }

        @Override
        public void getTag(Tag self, List<Tag> output) {
            if (self instanceof CompoundTag && this.predicate.test(self)) {
                output.add(self);
            }
        }

        @Override
        public void getOrCreateTag(Tag self, Supplier<Tag> child, List<Tag> output) {
            this.getTag(self, output);
        }

        @Override
        public Tag createPreferredParentTag() {
            return new CompoundTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            return 0;
        }

        @Override
        public int removeTag(Tag parent) {
            return 0;
        }
    }

    private static class MatchElementNode
    implements Node {
        private final CompoundTag pattern;
        private final Predicate<Tag> predicate;

        public MatchElementNode(CompoundTag pattern) {
            this.pattern = pattern;
            this.predicate = NbtPathArgument.createTagPredicate(pattern);
        }

        @Override
        public void getTag(Tag parent, List<Tag> output) {
            if (parent instanceof ListTag) {
                ListTag list = (ListTag)parent;
                list.stream().filter(this.predicate).forEach(output::add);
            }
        }

        @Override
        public void getOrCreateTag(Tag parent, Supplier<Tag> child, List<Tag> output) {
            MutableBoolean foundAnything = new MutableBoolean();
            if (parent instanceof ListTag) {
                ListTag list = (ListTag)parent;
                list.stream().filter(this.predicate).forEach(t -> {
                    output.add((Tag)t);
                    foundAnything.setTrue();
                });
                if (foundAnything.isFalse()) {
                    CompoundTag newTag = this.pattern.copy();
                    list.add(newTag);
                    output.add(newTag);
                }
            }
        }

        @Override
        public Tag createPreferredParentTag() {
            return new ListTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            int changedCount = 0;
            if (parent instanceof ListTag) {
                ListTag list = (ListTag)parent;
                int size = list.size();
                if (size == 0) {
                    list.add(toAdd.get());
                    ++changedCount;
                } else {
                    for (int i = 0; i < size; ++i) {
                        Tag newValue;
                        Tag currentValue = list.get(i);
                        if (!this.predicate.test(currentValue) || (newValue = toAdd.get()).equals(currentValue) || !list.setTag(i, newValue)) continue;
                        ++changedCount;
                    }
                }
            }
            return changedCount;
        }

        @Override
        public int removeTag(Tag parent) {
            int changedCount = 0;
            if (parent instanceof ListTag) {
                ListTag list = (ListTag)parent;
                for (int i = list.size() - 1; i >= 0; --i) {
                    if (!this.predicate.test(list.get(i))) continue;
                    list.remove(i);
                    ++changedCount;
                }
            }
            return changedCount;
        }
    }

    private static class AllElementsNode
    implements Node {
        public static final AllElementsNode INSTANCE = new AllElementsNode();

        private AllElementsNode() {
        }

        @Override
        public void getTag(Tag parent, List<Tag> output) {
            if (parent instanceof CollectionTag) {
                CollectionTag collection = (CollectionTag)parent;
                Iterables.addAll(output, (Iterable)collection);
            }
        }

        @Override
        public void getOrCreateTag(Tag parent, Supplier<Tag> child, List<Tag> output) {
            if (parent instanceof CollectionTag) {
                CollectionTag list = (CollectionTag)parent;
                if (list.isEmpty()) {
                    Tag result = child.get();
                    if (list.addTag(0, result)) {
                        output.add(result);
                    }
                } else {
                    Iterables.addAll(output, (Iterable)list);
                }
            }
        }

        @Override
        public Tag createPreferredParentTag() {
            return new ListTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            if (parent instanceof CollectionTag) {
                CollectionTag list = (CollectionTag)parent;
                int size = list.size();
                if (size == 0) {
                    list.addTag(0, toAdd.get());
                    return 1;
                }
                Tag newValue = toAdd.get();
                int changedCount = size - (int)list.stream().filter((Predicate<Tag>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, equals(java.lang.Object ), (Lnet/minecraft/nbt/Tag;)Z)((Tag)newValue)).count();
                if (changedCount == 0) {
                    return 0;
                }
                list.clear();
                if (!list.addTag(0, newValue)) {
                    return 0;
                }
                for (int i = 1; i < size; ++i) {
                    list.addTag(i, toAdd.get());
                }
                return changedCount;
            }
            return 0;
        }

        @Override
        public int removeTag(Tag parent) {
            CollectionTag list;
            int size;
            if (parent instanceof CollectionTag && (size = (list = (CollectionTag)parent).size()) > 0) {
                list.clear();
                return size;
            }
            return 0;
        }
    }

    private static class IndexedElementNode
    implements Node {
        private final int index;

        public IndexedElementNode(int index) {
            this.index = index;
        }

        @Override
        public void getTag(Tag parent, List<Tag> output) {
            if (parent instanceof CollectionTag) {
                int actualIndex;
                CollectionTag list = (CollectionTag)parent;
                int size = list.size();
                int n = actualIndex = this.index < 0 ? size + this.index : this.index;
                if (0 <= actualIndex && actualIndex < size) {
                    output.add(list.get(actualIndex));
                }
            }
        }

        @Override
        public void getOrCreateTag(Tag parent, Supplier<Tag> child, List<Tag> output) {
            this.getTag(parent, output);
        }

        @Override
        public Tag createPreferredParentTag() {
            return new ListTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            if (parent instanceof CollectionTag) {
                int actualIndex;
                CollectionTag list = (CollectionTag)parent;
                int size = list.size();
                int n = actualIndex = this.index < 0 ? size + this.index : this.index;
                if (0 <= actualIndex && actualIndex < size) {
                    Tag previousValue = list.get(actualIndex);
                    Tag newValue = toAdd.get();
                    if (!newValue.equals(previousValue) && list.setTag(actualIndex, newValue)) {
                        return 1;
                    }
                }
            }
            return 0;
        }

        @Override
        public int removeTag(Tag parent) {
            if (parent instanceof CollectionTag) {
                int actualIndex;
                CollectionTag list = (CollectionTag)parent;
                int size = list.size();
                int n = actualIndex = this.index < 0 ? size + this.index : this.index;
                if (0 <= actualIndex && actualIndex < size) {
                    list.remove(actualIndex);
                    return 1;
                }
            }
            return 0;
        }
    }

    private static class MatchObjectNode
    implements Node {
        private final String name;
        private final CompoundTag pattern;
        private final Predicate<Tag> predicate;

        public MatchObjectNode(String name, CompoundTag pattern) {
            this.name = name;
            this.pattern = pattern;
            this.predicate = NbtPathArgument.createTagPredicate(pattern);
        }

        @Override
        public void getTag(Tag parent, List<Tag> output) {
            CompoundTag compoundTag;
            Tag result;
            if (parent instanceof CompoundTag && this.predicate.test(result = (compoundTag = (CompoundTag)parent).get(this.name))) {
                output.add(result);
            }
        }

        @Override
        public void getOrCreateTag(Tag parent, Supplier<Tag> child, List<Tag> output) {
            if (parent instanceof CompoundTag) {
                CompoundTag compound = (CompoundTag)parent;
                Tag result = compound.get(this.name);
                if (result == null) {
                    result = this.pattern.copy();
                    compound.put(this.name, result);
                    output.add(result);
                } else if (this.predicate.test(result)) {
                    output.add(result);
                }
            }
        }

        @Override
        public Tag createPreferredParentTag() {
            return new CompoundTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            Tag newValue;
            CompoundTag compound;
            Tag currentValue;
            if (parent instanceof CompoundTag && this.predicate.test(currentValue = (compound = (CompoundTag)parent).get(this.name)) && !(newValue = toAdd.get()).equals(currentValue)) {
                compound.put(this.name, newValue);
                return 1;
            }
            return 0;
        }

        @Override
        public int removeTag(Tag parent) {
            CompoundTag compound;
            Tag current;
            if (parent instanceof CompoundTag && this.predicate.test(current = (compound = (CompoundTag)parent).get(this.name))) {
                compound.remove(this.name);
                return 1;
            }
            return 0;
        }
    }

    private static class CompoundChildNode
    implements Node {
        private final String name;

        public CompoundChildNode(String name) {
            this.name = name;
        }

        @Override
        public void getTag(Tag parent, List<Tag> output) {
            CompoundTag compoundTag;
            Tag result;
            if (parent instanceof CompoundTag && (result = (compoundTag = (CompoundTag)parent).get(this.name)) != null) {
                output.add(result);
            }
        }

        @Override
        public void getOrCreateTag(Tag parent, Supplier<Tag> child, List<Tag> output) {
            if (parent instanceof CompoundTag) {
                Tag result;
                CompoundTag compound = (CompoundTag)parent;
                if (compound.contains(this.name)) {
                    result = compound.get(this.name);
                } else {
                    result = child.get();
                    compound.put(this.name, result);
                }
                output.add(result);
            }
        }

        @Override
        public Tag createPreferredParentTag() {
            return new CompoundTag();
        }

        @Override
        public int setTag(Tag parent, Supplier<Tag> toAdd) {
            if (parent instanceof CompoundTag) {
                Tag previousValue;
                CompoundTag compound = (CompoundTag)parent;
                Tag newValue = toAdd.get();
                if (!newValue.equals(previousValue = compound.put(this.name, newValue))) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public int removeTag(Tag parent) {
            CompoundTag compound;
            if (parent instanceof CompoundTag && (compound = (CompoundTag)parent).contains(this.name)) {
                compound.remove(this.name);
                return 1;
            }
            return 0;
        }
    }
}

