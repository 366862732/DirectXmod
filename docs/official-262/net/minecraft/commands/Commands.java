/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.CommandContextBuilder
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.tree.ArgumentCommandNode
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.RootCommandNode
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.server.commands.AttributeCommand;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.DamageCommand;
import net.minecraft.server.commands.DataPackCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.DebugCommand;
import net.minecraft.server.commands.DebugConfigCommand;
import net.minecraft.server.commands.DebugMobSpawningCommand;
import net.minecraft.server.commands.DebugPathCommand;
import net.minecraft.server.commands.DefaultGameModeCommands;
import net.minecraft.server.commands.DialogCommand;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.server.commands.FetchProfileCommand;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.commands.HelpCommand;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.JfrCommand;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.RaidCommand;
import net.minecraft.server.commands.RandomCommand;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.commands.ReturnCommand;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.server.commands.RotateCommand;
import net.minecraft.server.commands.SaveAllCommand;
import net.minecraft.server.commands.SaveOffCommand;
import net.minecraft.server.commands.SaveOnCommand;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.commands.ScheduleCommand;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.commands.SeedCommand;
import net.minecraft.server.commands.ServerPackCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.SetPlayerIdleTimeoutCommand;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.commands.SetWorldSpawnCommand;
import net.minecraft.server.commands.SpawnArmorTrimsCommand;
import net.minecraft.server.commands.SpectateCommand;
import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.commands.StopCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.server.commands.StopwatchCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.SwingCommand;
import net.minecraft.server.commands.TagCommand;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.commands.TickCommand;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.commands.TransferCommand;
import net.minecraft.server.commands.TriggerCommand;
import net.minecraft.server.commands.UnpublishCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.server.commands.WardenSpawnTrackerCommand;
import net.minecraft.server.commands.WaypointCommand;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Commands {
    public static final String COMMAND_PREFIX = "/";
    private static final ThreadLocal<@Nullable ExecutionContext<CommandSourceStack>> CURRENT_EXECUTION_CONTEXT = new ThreadLocal();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final PermissionCheck LEVEL_ALL = PermissionCheck.AlwaysPass.INSTANCE;
    public static final PermissionCheck LEVEL_MODERATORS = new PermissionCheck.Require(Permissions.COMMANDS_MODERATOR);
    public static final PermissionCheck LEVEL_GAMEMASTERS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
    public static final PermissionCheck LEVEL_ADMINS = new PermissionCheck.Require(Permissions.COMMANDS_ADMIN);
    public static final PermissionCheck LEVEL_OWNERS = new PermissionCheck.Require(Permissions.COMMANDS_OWNER);
    private static final ClientboundCommandsPacket.NodeInspector<CommandSourceStack> COMMAND_NODE_INSPECTOR = new ClientboundCommandsPacket.NodeInspector<CommandSourceStack>(){
        private final CommandSourceStack noPermissionSource = Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS);

        @Override
        public @Nullable Identifier suggestionId(ArgumentCommandNode<CommandSourceStack, ?> node) {
            SuggestionProvider suggestionProvider = node.getCustomSuggestions();
            return suggestionProvider != null ? SuggestionProviders.getName(suggestionProvider) : null;
        }

        @Override
        public boolean isExecutable(CommandNode<CommandSourceStack> node) {
            return node.getCommand() != null;
        }

        @Override
        public boolean isRestricted(CommandNode<CommandSourceStack> node) {
            Predicate requirement = node.getRequirement();
            return !requirement.test(this.noPermissionSource);
        }
    };
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher();

    public Commands(CommandSelection commandSelection, CommandBuildContext context) {
        AdvancementCommands.register(this.dispatcher);
        AttributeCommand.register(this.dispatcher, context);
        ExecuteCommand.register(this.dispatcher, context);
        BossBarCommands.register(this.dispatcher, context);
        ClearInventoryCommands.register(this.dispatcher, context);
        CloneCommands.register(this.dispatcher, context);
        DamageCommand.register(this.dispatcher, context);
        DataCommands.register(this.dispatcher);
        DataPackCommand.register(this.dispatcher, context);
        DebugCommand.register(this.dispatcher);
        DefaultGameModeCommands.register(this.dispatcher);
        DialogCommand.register(this.dispatcher, context);
        DifficultyCommand.register(this.dispatcher);
        EffectCommands.register(this.dispatcher, context);
        EmoteCommands.register(this.dispatcher);
        EnchantCommand.register(this.dispatcher, context);
        ExperienceCommand.register(this.dispatcher);
        FillCommand.register(this.dispatcher, context);
        FillBiomeCommand.register(this.dispatcher, context);
        ForceLoadCommand.register(this.dispatcher);
        FunctionCommand.register(this.dispatcher);
        GameModeCommand.register(this.dispatcher);
        GameRuleCommand.register(this.dispatcher, context);
        GiveCommand.register(this.dispatcher, context);
        HelpCommand.register(this.dispatcher);
        ItemCommands.register(this.dispatcher, context);
        KickCommand.register(this.dispatcher);
        KillCommand.register(this.dispatcher);
        ListPlayersCommand.register(this.dispatcher);
        LocateCommand.register(this.dispatcher, context);
        LootCommand.register(this.dispatcher, context);
        MsgCommand.register(this.dispatcher);
        SwingCommand.register(this.dispatcher);
        ParticleCommand.register(this.dispatcher, context);
        PlaceCommand.register(this.dispatcher);
        PlaySoundCommand.register(this.dispatcher);
        RandomCommand.register(this.dispatcher);
        ReloadCommand.register(this.dispatcher);
        RecipeCommand.register(this.dispatcher);
        FetchProfileCommand.register(this.dispatcher);
        ReturnCommand.register(this.dispatcher);
        RideCommand.register(this.dispatcher);
        RotateCommand.register(this.dispatcher);
        SayCommand.register(this.dispatcher);
        ScheduleCommand.register(this.dispatcher);
        ScoreboardCommand.register(this.dispatcher, context);
        SeedCommand.register(this.dispatcher, commandSelection != CommandSelection.INTEGRATED);
        VersionCommand.register(this.dispatcher, commandSelection != CommandSelection.INTEGRATED);
        SetBlockCommand.register(this.dispatcher, context);
        SetSpawnCommand.register(this.dispatcher);
        SetWorldSpawnCommand.register(this.dispatcher);
        SpectateCommand.register(this.dispatcher);
        SpreadPlayersCommand.register(this.dispatcher);
        StopSoundCommand.register(this.dispatcher);
        StopwatchCommand.register(this.dispatcher);
        SummonCommand.register(this.dispatcher, context);
        TagCommand.register(this.dispatcher);
        TeamCommand.register(this.dispatcher, context);
        TeamMsgCommand.register(this.dispatcher);
        TeleportCommand.register(this.dispatcher);
        TellRawCommand.register(this.dispatcher, context);
        TestCommand.register(this.dispatcher, context);
        TickCommand.register(this.dispatcher);
        TimeCommand.register(this.dispatcher, context);
        TitleCommand.register(this.dispatcher, context);
        TriggerCommand.register(this.dispatcher);
        WaypointCommand.register(this.dispatcher, context);
        WeatherCommand.register(this.dispatcher);
        WorldBorderCommand.register(this.dispatcher);
        if (JvmProfiler.INSTANCE.isAvailable()) {
            JfrCommand.register(this.dispatcher);
        }
        if (SharedConstants.DEBUG_CHASE_COMMAND) {
            ChaseCommand.register(this.dispatcher);
        }
        if (SharedConstants.DEBUG_DEV_COMMANDS || SharedConstants.IS_RUNNING_IN_IDE) {
            RaidCommand.register(this.dispatcher, context);
            DebugPathCommand.register(this.dispatcher);
            DebugMobSpawningCommand.register(this.dispatcher);
            WardenSpawnTrackerCommand.register(this.dispatcher);
            SpawnArmorTrimsCommand.register(this.dispatcher);
            ServerPackCommand.register(this.dispatcher);
            if (commandSelection.includeDedicated) {
                DebugConfigCommand.register(this.dispatcher, context);
            }
        }
        if (commandSelection.includeDedicated) {
            BanIpCommands.register(this.dispatcher);
            BanListCommands.register(this.dispatcher);
            BanPlayerCommands.register(this.dispatcher);
            DeOpCommands.register(this.dispatcher);
            OpCommand.register(this.dispatcher);
            PardonCommand.register(this.dispatcher);
            PardonIpCommand.register(this.dispatcher);
            PerfCommand.register(this.dispatcher);
            SaveAllCommand.register(this.dispatcher);
            SaveOffCommand.register(this.dispatcher);
            SaveOnCommand.register(this.dispatcher);
            SetPlayerIdleTimeoutCommand.register(this.dispatcher);
            StopCommand.register(this.dispatcher);
            TransferCommand.register(this.dispatcher);
            WhitelistCommand.register(this.dispatcher);
        }
        if (commandSelection.includeIntegrated) {
            PublishCommand.register(this.dispatcher);
            UnpublishCommand.register(this.dispatcher);
        }
        this.dispatcher.setConsumer(ExecutionCommandSource.resultConsumer());
    }

    public static <S> ParseResults<S> mapSource(ParseResults<S> parse, UnaryOperator<S> sourceOperator) {
        CommandContextBuilder context = parse.getContext();
        CommandContextBuilder source = context.withSource(sourceOperator.apply(context.getSource()));
        return new ParseResults(source, parse.getReader(), parse.getExceptions());
    }

    public void performPrefixedCommand(CommandSourceStack sender, String command) {
        command = Commands.trimOptionalPrefix(command);
        this.performCommand((ParseResults<CommandSourceStack>)this.dispatcher.parse(command, (Object)sender), command);
    }

    public static String trimOptionalPrefix(String command) {
        return command.startsWith(COMMAND_PREFIX) ? command.substring(1) : command;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void performCommand(ParseResults<CommandSourceStack> command, String commandString) {
        CommandSourceStack sender = (CommandSourceStack)command.getContext().getSource();
        Profiler.get().push(() -> COMMAND_PREFIX + commandString);
        ContextChain<CommandSourceStack> commandChain = Commands.finishParsing(command, commandString, sender);
        try {
            if (commandChain != null) {
                Commands.executeCommandInContext(sender, executionContext -> ExecutionContext.queueInitialCommandExecution(executionContext, commandString, commandChain, sender, CommandResultCallback.EMPTY));
            }
        }
        catch (Exception e) {
            MutableComponent hover = Component.literal(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Command exception: /{}", (Object)commandString, (Object)e);
                StackTraceElement[] stackTrace = e.getStackTrace();
                for (int i = 0; i < Math.min(stackTrace.length, 3); ++i) {
                    hover.append("\n\n").append(stackTrace[i].getMethodName()).append("\n ").append(stackTrace[i].getFileName()).append(":").append(String.valueOf(stackTrace[i].getLineNumber()));
                }
            }
            sender.sendFailure(Component.translatable("command.failed").withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(hover))));
            if (SharedConstants.DEBUG_VERBOSE_COMMAND_ERRORS || SharedConstants.IS_RUNNING_IN_IDE) {
                sender.sendFailure(Component.literal(Util.describeError(e)));
                LOGGER.error("'/{}' threw an exception", (Object)commandString, (Object)e);
            }
        }
        finally {
            Profiler.get().pop();
        }
    }

    private static @Nullable ContextChain<CommandSourceStack> finishParsing(ParseResults<CommandSourceStack> command, String commandString, CommandSourceStack sender) {
        try {
            Commands.validateParseResults(command);
            return (ContextChain)ContextChain.tryFlatten((CommandContext)command.getContext().build(commandString)).orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(command.getReader()));
        }
        catch (CommandSyntaxException e) {
            sender.sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                int cursor = Math.min(e.getInput().length(), e.getCursor());
                MutableComponent context = Component.empty().withStyle(ChatFormatting.GRAY).withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand(COMMAND_PREFIX + commandString)));
                if (cursor > 10) {
                    context.append(CommonComponents.ELLIPSIS);
                }
                context.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    MutableComponent remaining = Component.literal(e.getInput().substring(cursor)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
                    context.append(remaining);
                }
                context.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                sender.sendFailure(context);
            }
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void executeCommandInContext(CommandSourceStack context, Consumer<ExecutionContext<CommandSourceStack>> config) {
        block9: {
            boolean isTopContext;
            ExecutionContext<CommandSourceStack> currentContext = CURRENT_EXECUTION_CONTEXT.get();
            boolean bl = isTopContext = currentContext == null;
            if (isTopContext) {
                GameRules gameRules = context.getLevel().getGameRules();
                int chainLimit = Math.max(1, gameRules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH));
                int forkLimit = gameRules.get(GameRules.MAX_COMMAND_FORKS);
                try (ExecutionContext executionContext = new ExecutionContext(chainLimit, forkLimit, Profiler.get());){
                    CURRENT_EXECUTION_CONTEXT.set(executionContext);
                    config.accept(executionContext);
                    executionContext.runCommandQueue();
                    break block9;
                }
                finally {
                    CURRENT_EXECUTION_CONTEXT.set(null);
                }
            }
            config.accept(currentContext);
        }
    }

    public void sendCommands(ServerPlayer player) {
        HashMap playerCommands = new HashMap();
        RootCommandNode root = new RootCommandNode();
        playerCommands.put((CommandNode)this.dispatcher.getRoot(), (CommandNode)root);
        Commands.fillUsableCommands(this.dispatcher.getRoot(), root, player.createCommandSourceStack(), playerCommands);
        player.connection.send(new ClientboundCommandsPacket(root, COMMAND_NODE_INSPECTOR));
    }

    private static <S> void fillUsableCommands(CommandNode<S> source, CommandNode<S> target, S commandFilter, Map<CommandNode<S>, CommandNode<S>> converted) {
        for (CommandNode child : source.getChildren()) {
            if (!child.canUse(commandFilter)) continue;
            ArgumentBuilder builder = child.createBuilder();
            if (builder.getRedirect() != null) {
                builder.redirect(converted.get(builder.getRedirect()));
            }
            CommandNode node = builder.build();
            converted.put(child, node);
            target.addChild(node);
            if (child.getChildren().isEmpty()) continue;
            Commands.fillUsableCommands(child, node, commandFilter, converted);
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String literal) {
        return LiteralArgumentBuilder.literal((String)literal);
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument((String)name, type);
    }

    public static Predicate<String> createValidator(ParseFunction parser) {
        return value -> {
            try {
                parser.parse(new StringReader(value));
                return true;
            }
            catch (CommandSyntaxException ignored) {
                return false;
            }
        };
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.dispatcher;
    }

    public static <S> void validateParseResults(ParseResults<S> command) throws CommandSyntaxException {
        CommandSyntaxException parseException = Commands.getParseException(command);
        if (parseException != null) {
            throw parseException;
        }
    }

    public static <S> @Nullable CommandSyntaxException getParseException(ParseResults<S> parse) {
        if (!parse.getReader().canRead()) {
            return null;
        }
        if (parse.getExceptions().size() == 1) {
            return (CommandSyntaxException)((Object)parse.getExceptions().values().iterator().next());
        }
        if (parse.getContext().getRange().isEmpty()) {
            return CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader());
        }
        return CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
    }

    public static CommandBuildContext createValidationContext(final HolderLookup.Provider registries) {
        return new CommandBuildContext(){

            @Override
            public FeatureFlagSet enabledFeatures() {
                return FeatureFlags.REGISTRY.allFlags();
            }

            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return registries.listRegistryKeys();
            }

            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                return registries.lookup(key).map(this::createLookup);
            }

            private <T> HolderLookup.RegistryLookup.Delegate<T> createLookup(final HolderLookup.RegistryLookup<T> original) {
                return new HolderLookup.RegistryLookup.Delegate<T>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }

                    @Override
                    public HolderLookup.RegistryLookup<T> parent() {
                        return original;
                    }

                    @Override
                    public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                        return Optional.of(this.getOrThrow(id));
                    }

                    @Override
                    public HolderSet.Named<T> getOrThrow(TagKey<T> id) {
                        Optional<HolderSet.Named<HolderSet.Named>> tag = this.parent().get(id);
                        return tag.orElseGet(() -> HolderSet.emptyNamed(this.parent(), id));
                    }
                };
            }
        };
    }

    public static void validate() {
        CommandBuildContext context = Commands.createValidationContext(VanillaRegistries.createLookup());
        CommandDispatcher<CommandSourceStack> dispatcher = new Commands(CommandSelection.ALL, context).getDispatcher();
        RootCommandNode root = dispatcher.getRoot();
        dispatcher.findAmbiguities((parent, child, sibling, ambiguities) -> LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", new Object[]{dispatcher.getPath(child), dispatcher.getPath(sibling), ambiguities}));
        Set<ArgumentType<?>> usedArgumentTypes = ArgumentUtils.findUsedArgumentTypes(root);
        Set unregisteredTypes = usedArgumentTypes.stream().filter(arg -> !ArgumentTypeInfos.isClassRecognized(arg.getClass())).collect(Collectors.toSet());
        if (!unregisteredTypes.isEmpty()) {
            LOGGER.warn("Missing type registration for following arguments:\n {}", (Object)unregisteredTypes.stream().map(arg -> "\t" + String.valueOf(arg)).collect(Collectors.joining(",\n")));
            throw new IllegalStateException("Unregistered argument types");
        }
    }

    public static <T extends PermissionSetSupplier> PermissionProviderCheck<T> hasPermission(PermissionCheck permission) {
        return new PermissionProviderCheck(permission);
    }

    public static CommandSourceStack createCompilationContext(PermissionSet compilationPermissions) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, compilationPermissions, "", CommonComponents.EMPTY, null, null);
    }

    public static enum CommandSelection {
        ALL(true, true),
        DEDICATED(false, true),
        INTEGRATED(true, false);

        private final boolean includeIntegrated;
        private final boolean includeDedicated;

        private CommandSelection(boolean includeIntegrated, boolean includeDedicated) {
            this.includeIntegrated = includeIntegrated;
            this.includeDedicated = includeDedicated;
        }
    }

    @FunctionalInterface
    public static interface ParseFunction {
        public void parse(StringReader var1) throws CommandSyntaxException;
    }
}

