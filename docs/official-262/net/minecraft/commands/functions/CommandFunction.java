/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.functions.FunctionBuilder;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface CommandFunction<T> {
    public Identifier id();

    public InstantiatedFunction<T> instantiate(@Nullable CompoundTag var1, CommandDispatcher<T> var2) throws FunctionInstantiationException;

    private static boolean shouldConcatenateNextLine(CharSequence line) {
        int length = line.length();
        return length > 0 && line.charAt(length - 1) == '\\';
    }

    public static <T extends ExecutionCommandSource<T>> CommandFunction<T> fromLines(Identifier id, CommandDispatcher<T> dispatcher, T compilationContext, List<String> lines) {
        FunctionBuilder<T> functionBuilder = new FunctionBuilder<T>();
        for (int i = 0; i < lines.size(); ++i) {
            String line;
            int lineNumber = i + 1;
            String inputLine = lines.get(i).trim();
            if (CommandFunction.shouldConcatenateNextLine(inputLine)) {
                StringBuilder builder = new StringBuilder(inputLine);
                do {
                    if (++i == lines.size()) {
                        throw new IllegalArgumentException("Line continuation at end of file");
                    }
                    builder.deleteCharAt(builder.length() - 1);
                    String innerLine = lines.get(i).trim();
                    builder.append(innerLine);
                    CommandFunction.checkCommandLineLength(builder);
                } while (CommandFunction.shouldConcatenateNextLine(builder));
                line = builder.toString();
            } else {
                line = inputLine;
            }
            CommandFunction.checkCommandLineLength(line);
            StringReader input = new StringReader(line);
            if (!input.canRead() || input.peek() == '#') continue;
            if (input.peek() == '/') {
                input.skip();
                if (input.peek() == '/') {
                    throw new IllegalArgumentException("Unknown or invalid command '" + line + "' on line " + lineNumber + " (if you intended to make a comment, use '#' not '//')");
                }
                String name = input.readUnquotedString();
                throw new IllegalArgumentException("Unknown or invalid command '" + line + "' on line " + lineNumber + " (did you mean '" + name + "'? Do not use a preceding forwards slash.)");
            }
            if (input.peek() == '$') {
                functionBuilder.addMacro(line.substring(1), lineNumber, compilationContext);
                continue;
            }
            try {
                functionBuilder.addCommand(CommandFunction.parseCommand(dispatcher, compilationContext, input));
                continue;
            }
            catch (CommandSyntaxException e) {
                throw new IllegalArgumentException("Whilst parsing command on line " + lineNumber + ": " + e.getMessage());
            }
        }
        return functionBuilder.build(id);
    }

    public static void checkCommandLineLength(CharSequence line) {
        if (line.length() > 2000000) {
            CharSequence truncated = line.subSequence(0, Math.min(512, 2000000));
            throw new IllegalStateException("Command too long: " + line.length() + " characters, contents: " + String.valueOf(truncated) + "...");
        }
    }

    public static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> parseCommand(CommandDispatcher<T> dispatcher, T compilationContext, StringReader input) throws CommandSyntaxException {
        ParseResults parse = dispatcher.parse(input, compilationContext);
        Commands.validateParseResults(parse);
        Optional commandChain = ContextChain.tryFlatten((CommandContext)parse.getContext().build(input.getString()));
        if (commandChain.isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader());
        }
        return new BuildContexts.Unbound(input.getString(), (ContextChain)commandChain.get());
    }
}

