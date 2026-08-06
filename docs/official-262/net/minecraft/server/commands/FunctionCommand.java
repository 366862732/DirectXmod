/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.brigadier.Command
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.jspecify.annotations.Nullable;

public class FunctionCommand {
    private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(type -> Component.translatableEscape("commands.function.error.argument_not_compound", type));
    private static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType(name -> Component.translatableEscape("commands.function.scheduled.no_functions", name));
    @VisibleForTesting
    public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType((id, reason) -> Component.translatableEscape("commands.function.instantiationFailure", id, reason));
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (c, p) -> {
        ServerFunctionManager manager = ((CommandSourceStack)c.getSource()).getServer().getFunctions();
        SharedSuggestionProvider.suggestResource(manager.getTagNames(), p, "#");
        return SharedSuggestionProvider.suggestResource(manager.getFunctionNames(), p);
    };
    private static final Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new Callbacks<CommandSourceStack>(){

        @Override
        public void signalResult(CommandSourceStack originalSource, Identifier id, int newValue) {
            originalSource.sendSuccess(() -> Component.translatable("commands.function.result", Component.translationArg(id), newValue), true);
        }
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> sources = Commands.literal("with");
        for (final DataCommands.DataProvider provider : DataCommands.SOURCE_PROVIDERS) {
            provider.wrap((ArgumentBuilder<CommandSourceStack, ?>)sources, p -> p.executes((Command)new FunctionCustomExecutor(){

                @Override
                protected CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return provider.access(context).getData();
                }
            }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes((Command)new FunctionCustomExecutor(){

                @Override
                protected CompoundTag arguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
                    return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(context, "path"), provider.access(context));
                }
            })));
        }
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("function").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", FunctionArgument.functions()).suggests(SUGGEST_FUNCTION).executes((Command)new FunctionCustomExecutor(){

            @Override
            protected @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> context) {
                return null;
            }
        })).then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes((Command)new FunctionCustomExecutor(){

            @Override
            protected CompoundTag arguments(CommandContext<CommandSourceStack> context) {
                return CompoundTagArgument.getCompoundTag(context, "arguments");
            }
        }))).then(sources)));
    }

    private static CompoundTag getArgumentTag(NbtPathArgument.NbtPath path, DataAccessor accessor) throws CommandSyntaxException {
        Tag tag = DataCommands.getSingleTag(path, accessor);
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)tag;
            return compoundTag;
        }
        throw ERROR_ARGUMENT_NOT_COMPOUND.create((Object)tag.getType().getName());
    }

    public static CommandSourceStack modifySenderForExecution(CommandSourceStack sender) {
        return sender.withSuppressedOutput().withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
    }

    public static <T extends ExecutionCommandSource<T>> void queueFunctions(Collection<CommandFunction<T>> functions, @Nullable CompoundTag arguments, T originalSource, T functionSource, ExecutionControl<T> output, Callbacks<T> callbacks, ChainModifiers modifiers) throws CommandSyntaxException {
        if (modifiers.isReturn()) {
            FunctionCommand.queueFunctionsAsReturn(functions, arguments, originalSource, functionSource, output, callbacks);
        } else {
            FunctionCommand.queueFunctionsNoReturn(functions, arguments, originalSource, functionSource, output, callbacks);
        }
    }

    private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(@Nullable CompoundTag arguments, ExecutionControl<T> output, CommandDispatcher<T> dispatcher, T noCallbackSource, CommandFunction<T> function, Identifier id, CommandResultCallback functionResultCollector, boolean returnParentFrame) throws CommandSyntaxException {
        try {
            InstantiatedFunction<T> instantiatedFunction = function.instantiate(arguments, dispatcher);
            output.queueNext(new CallFunction<T>(instantiatedFunction, functionResultCollector, returnParentFrame).bind(noCallbackSource));
        }
        catch (FunctionInstantiationException exception) {
            throw ERROR_FUNCTION_INSTANTATION_FAILURE.create((Object)id, (Object)exception.messageComponent());
        }
    }

    private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(T originalSource, Callbacks<T> callbacks, Identifier id, CommandResultCallback callback) {
        if (originalSource.isSilent()) {
            return callback;
        }
        return (success, result) -> {
            callbacks.signalResult(originalSource, id, result);
            callback.onResult(success, result);
        };
    }

    private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(Collection<CommandFunction<T>> functions, @Nullable CompoundTag arguments, T originalSource, T functionSource, ExecutionControl<T> output, Callbacks<T> callbacks) throws CommandSyntaxException {
        CommandDispatcher<T> dispatcher = originalSource.dispatcher();
        T noCallbackSource = functionSource.clearCallbacks();
        CommandResultCallback functionCommandOutputCallback = CommandResultCallback.chain(originalSource.callback(), output.currentFrame().returnValueConsumer());
        for (CommandFunction<T> function : functions) {
            Identifier id = function.id();
            CommandResultCallback functionResultCollector = FunctionCommand.decorateOutputIfNeeded(originalSource, callbacks, id, functionCommandOutputCallback);
            FunctionCommand.instantiateAndQueueFunctions(arguments, output, dispatcher, noCallbackSource, function, id, functionResultCollector, true);
        }
        output.queueNext(FallthroughTask.instance());
    }

    private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(Collection<CommandFunction<T>> functions, @Nullable CompoundTag arguments, T originalSource, T functionSource, ExecutionControl<T> output, Callbacks<T> callbacks) throws CommandSyntaxException {
        CommandDispatcher<T> dispatcher = originalSource.dispatcher();
        T noCallbackSource = functionSource.clearCallbacks();
        CommandResultCallback originalCallback = originalSource.callback();
        if (functions.isEmpty()) {
            return;
        }
        if (functions.size() == 1) {
            CommandFunction<T> function = functions.iterator().next();
            Identifier id = function.id();
            CommandResultCallback functionResultCollector = FunctionCommand.decorateOutputIfNeeded(originalSource, callbacks, id, originalCallback);
            FunctionCommand.instantiateAndQueueFunctions(arguments, output, dispatcher, noCallbackSource, function, id, functionResultCollector, false);
        } else if (originalCallback == CommandResultCallback.EMPTY) {
            for (CommandFunction<T> function : functions) {
                Identifier id = function.id();
                CommandResultCallback functionResultCollector = FunctionCommand.decorateOutputIfNeeded(originalSource, callbacks, id, originalCallback);
                FunctionCommand.instantiateAndQueueFunctions(arguments, output, dispatcher, noCallbackSource, function, id, functionResultCollector, false);
            }
        } else {
            class Accumulator {
                private boolean anyResult;
                private int sum;

                Accumulator() {
                }

                public void add(int result) {
                    this.anyResult = true;
                    this.sum += result;
                }
            }
            Accumulator accumulator = new Accumulator();
            CommandResultCallback partialResultCallback = (success, result) -> accumulator.add(result);
            for (CommandFunction<T> function : functions) {
                Identifier id = function.id();
                CommandResultCallback functionResultCollector = FunctionCommand.decorateOutputIfNeeded(originalSource, callbacks, id, partialResultCallback);
                FunctionCommand.instantiateAndQueueFunctions(arguments, output, dispatcher, noCallbackSource, function, id, functionResultCollector, false);
            }
            output.queueNext((context, frame) -> {
                if (accumulator.anyResult) {
                    originalCallback.onSuccess(accumulator.sum);
                }
            });
        }
    }

    public static interface Callbacks<T> {
        public void signalResult(T var1, Identifier var2, int var3);
    }

    private static abstract class FunctionCustomExecutor
    extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
    implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        private FunctionCustomExecutor() {
        }

        protected abstract @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

        @Override
        public void runGuarded(CommandSourceStack sender, ContextChain<CommandSourceStack> currentStep, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> output) throws CommandSyntaxException {
            CommandContext currentContext = currentStep.getTopContext().copyFor((Object)sender);
            Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> nameAndFunctions = FunctionArgument.getFunctionCollection((CommandContext<CommandSourceStack>)currentContext, "name");
            Collection functions = (Collection)nameAndFunctions.getSecond();
            if (functions.isEmpty()) {
                throw ERROR_NO_FUNCTIONS.create((Object)Component.translationArg((Identifier)nameAndFunctions.getFirst()));
            }
            CompoundTag arguments = this.arguments((CommandContext<CommandSourceStack>)currentContext);
            CommandSourceStack commonFunctionContext = FunctionCommand.modifySenderForExecution(sender);
            if (functions.size() == 1) {
                sender.sendSuccess(() -> Component.translatable("commands.function.scheduled.single", Component.translationArg(((CommandFunction)functions.iterator().next()).id())), true);
            } else {
                sender.sendSuccess(() -> Component.translatable("commands.function.scheduled.multiple", ComponentUtils.formatList(functions.stream().map(CommandFunction::id).toList(), Component::translationArg)), true);
            }
            FunctionCommand.queueFunctions(functions, arguments, sender, commonFunctionContext, output, FULL_CONTEXT_CALLBACKS, modifiers);
        }
    }
}

