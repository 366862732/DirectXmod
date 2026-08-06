/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments.blocks;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockPredicateArgument
implements ArgumentType<Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
    private final HolderLookup<Block> blocks;

    public BlockPredicateArgument(CommandBuildContext context) {
        this.blocks = context.lookupOrThrow(Registries.BLOCK);
    }

    public static BlockPredicateArgument blockPredicate(CommandBuildContext context) {
        return new BlockPredicateArgument(context);
    }

    public Result parse(StringReader reader) throws CommandSyntaxException {
        return BlockPredicateArgument.parse(this.blocks, reader);
    }

    public static Result parse(HolderLookup<Block> blocks, StringReader reader) throws CommandSyntaxException {
        return (Result)BlockStateParser.parseForTesting(blocks, reader, true).map(block -> new BlockPredicate(block.blockState(), block.properties().keySet(), block.nbt()), tag -> new TagPredicate(tag.tag(), tag.vagueProperties(), tag.nbt()));
    }

    public static Predicate<BlockInWorld> getBlockPredicate(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return (Predicate)context.getArgument(name, Result.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return BlockStateParser.fillSuggestions(this.blocks, builder, true, true);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static interface Result
    extends Predicate<BlockInWorld> {
        public boolean requiresNbt();
    }

    private static class TagPredicate
    implements Result {
        private final HolderSet<Block> tag;
        private final @Nullable CompoundTag nbt;
        private final Map<String, String> vagueProperties;

        private TagPredicate(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
            this.tag = tag;
            this.vagueProperties = vagueProperties;
            this.nbt = nbt;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            BlockState state = blockInWorld.getState();
            if (!state.is(this.tag)) {
                return false;
            }
            for (Map.Entry<String, String> entry : this.vagueProperties.entrySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null) {
                    return false;
                }
                Comparable value = property.getValue(entry.getValue()).orElse(null);
                if (value == null) {
                    return false;
                }
                if (state.getValue(property) == value) continue;
                return false;
            }
            if (this.nbt != null) {
                BlockEntity entity = blockInWorld.getEntity();
                return entity != null && NbtUtils.compareNbt(this.nbt, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
            }
            return true;
        }

        @Override
        public boolean requiresNbt() {
            return this.nbt != null;
        }
    }

    private static class BlockPredicate
    implements Result {
        private final BlockState state;
        private final Set<Property<?>> properties;
        private final @Nullable CompoundTag nbt;

        public BlockPredicate(BlockState state, Set<Property<?>> properties, @Nullable CompoundTag nbt) {
            this.state = state;
            this.properties = properties;
            this.nbt = nbt;
        }

        @Override
        public boolean test(BlockInWorld blockInWorld) {
            BlockState state = blockInWorld.getState();
            if (!state.is(this.state.getBlock())) {
                return false;
            }
            for (Property<?> property : this.properties) {
                if (state.getValue(property) == this.state.getValue(property)) continue;
                return false;
            }
            if (this.nbt != null) {
                BlockEntity entity = blockInWorld.getEntity();
                return entity != null && NbtUtils.compareNbt(this.nbt, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
            }
            return true;
        }

        @Override
        public boolean requiresNbt() {
            return this.nbt != null;
        }
    }
}

