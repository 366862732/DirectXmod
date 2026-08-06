/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.Command
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class DebugCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType((Message)Component.translatable("commands.debug.notRunning"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType((Message)Component.translatable("commands.debug.alreadyRunning"));
    private static final SimpleCommandExceptionType NO_RECURSIVE_TRACES = new SimpleCommandExceptionType((Message)Component.translatable("commands.debug.function.noRecursion"));
    private static final SimpleCommandExceptionType NO_RETURN_RUN = new SimpleCommandExceptionType((Message)Component.translatable("commands.debug.function.noReturnRun"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(Commands.literal("start").executes(c -> DebugCommand.start((CommandSourceStack)c.getSource())))).then(Commands.literal("stop").executes(c -> DebugCommand.stop((CommandSourceStack)c.getSource())))).then(((LiteralArgumentBuilder)Commands.literal("function").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(Commands.argument("name", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).executes((Command)new TraceCustomExecutor()))));
    }

    private static int start(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        if (server.isTimeProfilerRunning()) {
            throw ERROR_ALREADY_RUNNING.create();
        }
        server.startTimeProfiler();
        source.sendSuccess(() -> Component.translatable("commands.debug.started"), true);
        return 0;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        if (!server.isTimeProfilerRunning()) {
            throw ERROR_NOT_RUNNING.create();
        }
        ProfileResults results = server.stopTimeProfiler();
        double seconds = (double)results.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
        double tps = (double)results.getTickDuration() / seconds;
        source.sendSuccess(() -> Component.translatable("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", seconds), results.getTickDuration(), String.format(Locale.ROOT, "%.2f", tps)), true);
        return (int)tps;
    }

    private static class TraceCustomExecutor
    extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
    implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        private TraceCustomExecutor() {
        }

        @Override
        public void runGuarded(CommandSourceStack source, ContextChain<CommandSourceStack> currentStep, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> context) throws CommandSyntaxException {
            if (modifiers.isReturn()) {
                throw NO_RETURN_RUN.create();
            }
            if (context.tracer() != null) {
                throw NO_RECURSIVE_TRACES.create();
            }
            CommandContext currentContext = currentStep.getTopContext();
            Collection<CommandFunction<CommandSourceStack>> functions = FunctionArgument.getFunctions((CommandContext<CommandSourceStack>)currentContext, "name");
            MinecraftServer server = source.getServer();
            String outputName = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";
            CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getFunctions().getDispatcher();
            int commandCount = 0;
            try {
                Path dirPath = server.getFile("debug");
                Files.createDirectories(dirPath, new FileAttribute[0]);
                final PrintWriter output = new PrintWriter(Files.newBufferedWriter(dirPath.resolve(outputName), StandardCharsets.UTF_8, new OpenOption[0]));
                Tracer tracer = new Tracer(output);
                context.tracer(tracer);
                for (final CommandFunction<CommandSourceStack> function : functions) {
                    try {
                        CommandSourceStack functionSource = source.withSource(tracer).withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
                        InstantiatedFunction<CommandSourceStack> instantiatedFunction = function.instantiate(null, dispatcher);
                        context.queueNext(new CallFunction<CommandSourceStack>(this, instantiatedFunction, CommandResultCallback.EMPTY, false){
                            {
                                Objects.requireNonNull(this$0);
                                super(function2, resultCallback, returnParentFrame);
                            }

                            @Override
                            public void execute(CommandSourceStack sender, ExecutionContext<CommandSourceStack> context, Frame frame) {
                                output.println(function.id());
                                super.execute(sender, context, frame);
                            }
                        }.bind(functionSource));
                        commandCount += instantiatedFunction.entries().size();
                    }
                    catch (FunctionInstantiationException exception) {
                        source.sendFailure(exception.messageComponent());
                    }
                }
            }
            catch (IOException | UncheckedIOException e) {
                LOGGER.warn("Tracing failed", (Throwable)e);
                source.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
            }
            int finalCommandCount = commandCount;
            context.queueNext((c, frame) -> {
                if (functions.size() == 1) {
                    source.sendSuccess(() -> Component.translatable("commands.debug.function.success.single", finalCommandCount, Component.translationArg(((CommandFunction)functions.iterator().next()).id()), outputName), true);
                } else {
                    source.sendSuccess(() -> Component.translatable("commands.debug.function.success.multiple", finalCommandCount, functions.size(), outputName), true);
                }
            });
        }
    }

    private static class Tracer
    implements CommandSource,
    TraceCallbacks {
        public static final int INDENT_OFFSET = 1;
        private final PrintWriter output;
        private int lastIndent;
        private boolean waitingForResult;

        private Tracer(PrintWriter output) {
            this.output = output;
        }

        private void indentAndSave(int value) {
            this.printIndent(value);
            this.lastIndent = value;
        }

        private void printIndent(int value) {
            for (int i = 0; i < value + 1; ++i) {
                this.output.write("    ");
            }
        }

        private void newLine() {
            if (this.waitingForResult) {
                this.output.println();
                this.waitingForResult = false;
            }
        }

        @Override
        public void onCommand(int depth, String command) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[C] ");
            this.output.print(command);
            this.waitingForResult = true;
        }

        @Override
        public void onReturn(int depth, String command, int result) {
            if (this.waitingForResult) {
                this.output.print(" -> ");
                this.output.println(result);
                this.waitingForResult = false;
            } else {
                this.indentAndSave(depth);
                this.output.print("[R = ");
                this.output.print(result);
                this.output.print("] ");
                this.output.println(command);
            }
        }

        @Override
        public void onCall(int depth, Identifier function, int size) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[F] ");
            this.output.print(function);
            this.output.print(" size=");
            this.output.println(size);
        }

        @Override
        public void onError(String message) {
            this.newLine();
            this.indentAndSave(this.lastIndent + 1);
            this.output.print("[E] ");
            this.output.print(message);
        }

        @Override
        public void sendSystemMessage(Component message) {
            this.newLine();
            this.printIndent(this.lastIndent + 1);
            this.output.print("[M] ");
            this.output.println(message.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        @Override
        public boolean alwaysAccepts() {
            return true;
        }

        @Override
        public void close() {
            IOUtils.closeQuietly((Writer)this.output);
        }
    }
}

