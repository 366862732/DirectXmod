/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ServerFunctionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier TICK_FUNCTION_TAG = Identifier.withDefaultNamespace("tick");
    private static final Identifier LOAD_FUNCTION_TAG = Identifier.withDefaultNamespace("load");
    private final MinecraftServer server;
    private List<CommandFunction<CommandSourceStack>> ticking = ImmutableList.of();
    private boolean postReload;
    private ServerFunctionLibrary library;

    public ServerFunctionManager(MinecraftServer server, ServerFunctionLibrary library) {
        this.server = server;
        this.library = library;
        this.postReload(library);
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.server.getCommands().getDispatcher();
    }

    public void tick() {
        if (!this.server.tickRateManager().runsNormally()) {
            return;
        }
        if (this.postReload) {
            this.postReload = false;
            List<CommandFunction<CommandSourceStack>> functions = this.library.getTag(LOAD_FUNCTION_TAG);
            this.executeTagFunctions(functions, LOAD_FUNCTION_TAG);
        }
        this.executeTagFunctions(this.ticking, TICK_FUNCTION_TAG);
    }

    private void executeTagFunctions(Collection<CommandFunction<CommandSourceStack>> functions, Identifier loadFunctionTag) {
        Profiler.get().push(loadFunctionTag::toString);
        for (CommandFunction<CommandSourceStack> function : functions) {
            this.execute(function, this.getGameLoopSender());
        }
        Profiler.get().pop();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void execute(CommandFunction<CommandSourceStack> functionIn, CommandSourceStack sender) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push(() -> "function " + String.valueOf(functionIn.id()));
        try {
            InstantiatedFunction<CommandSourceStack> function = functionIn.instantiate(null, this.getDispatcher());
            Commands.executeCommandInContext(sender, context -> ExecutionContext.queueInitialFunctionCall(context, function, sender, CommandResultCallback.EMPTY));
        }
        catch (FunctionInstantiationException function) {
        }
        catch (Exception e) {
            LOGGER.warn("Failed to execute function {}", (Object)functionIn.id(), (Object)e);
        }
        finally {
            profiler.pop();
        }
    }

    public void replaceLibrary(ServerFunctionLibrary library) {
        this.library = library;
        this.postReload(library);
    }

    private void postReload(ServerFunctionLibrary library) {
        this.ticking = List.copyOf(library.getTag(TICK_FUNCTION_TAG));
        this.postReload = true;
    }

    public CommandSourceStack getGameLoopSender() {
        return this.server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }

    public Optional<CommandFunction<CommandSourceStack>> get(Identifier id) {
        return this.library.getFunction(id);
    }

    public List<CommandFunction<CommandSourceStack>> getTag(Identifier id) {
        return this.library.getTag(id);
    }

    public Iterable<Identifier> getFunctionNames() {
        return this.library.getFunctions().keySet();
    }

    public Iterable<Identifier> getTagNames() {
        return this.library.getAvailableTags();
    }
}

