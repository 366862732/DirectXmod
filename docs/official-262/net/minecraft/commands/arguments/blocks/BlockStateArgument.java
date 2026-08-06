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
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class BlockStateArgument
implements ArgumentType<BlockInput> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
    private final HolderLookup<Block> blocks;

    public BlockStateArgument(CommandBuildContext context) {
        this.blocks = context.lookupOrThrow(Registries.BLOCK);
    }

    public static BlockStateArgument block(CommandBuildContext context) {
        return new BlockStateArgument(context);
    }

    public BlockInput parse(StringReader reader) throws CommandSyntaxException {
        BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(this.blocks, reader, true);
        return new BlockInput(result.blockState(), result.properties().keySet(), result.nbt());
    }

    public static BlockInput getBlock(CommandContext<CommandSourceStack> context, String name) {
        return (BlockInput)context.getArgument(name, BlockInput.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return BlockStateParser.fillSuggestions(this.blocks, builder, false, true);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

