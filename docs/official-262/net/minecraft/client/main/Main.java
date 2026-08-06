/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.base.Ticker
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.logging.LogUtils
 *  com.mojang.util.UndashedUuid
 *  joptsimple.ArgumentAcceptingOptionSpec
 *  joptsimple.NonOptionArgumentSpec
 *  joptsimple.OptionParser
 *  joptsimple.OptionSet
 *  joptsimple.OptionSpec
 *  joptsimple.OptionSpecBuilder
 *  joptsimple.ValueConverter
 *  joptsimple.util.EnumConverter
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringEscapeUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.main;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.blaze3d.TracyBootstrap;
import com.mojang.blaze3d.platform.ClientShutdownWatchdog;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import com.mojang.util.UndashedUuid;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import joptsimple.ValueConverter;
import joptsimple.util.EnumConverter;
import net.fabricmc.api.EnvType;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Optionull;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PreferredGraphicsApi;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@net.fabricmc.api.Environment(value=EnvType.CLIENT)
public class Main {
    public static void main(String[] args) {
        CrashReport report;
        GameConfig gameConfig;
        Logger logger;
        OptionSet optionSet;
        OptionParser parser;
        try {
            SharedConstants.tryDetectVersion();
        }
        catch (Throwable t) {
            Main.logEarlyException(t);
            System.exit(-7);
            return;
        }
        try {
            parser = new OptionParser();
        }
        catch (Throwable t) {
            Main.logEarlyException(t);
            System.exit(-4);
            return;
        }
        parser.allowsUnrecognizedOptions();
        parser.accepts("demo");
        parser.accepts("disableMultiplayer");
        parser.accepts("disableChat");
        parser.accepts("fullscreen");
        parser.accepts("checkGlErrors");
        OptionSpecBuilder renderDebugLabelsOption = parser.accepts("renderDebugLabels");
        OptionSpecBuilder vulkanValidationOption = parser.accepts("vulkanValidation");
        ArgumentAcceptingOptionSpec graphicsBackendOption = parser.accepts("graphicsBackend").withRequiredArg().withValuesConvertedBy((ValueConverter)new EnumConverter<PreferredGraphicsApi>(PreferredGraphicsApi.class){});
        OptionSpecBuilder jfrProfilingOption = parser.accepts("jfrProfile");
        OptionSpecBuilder tracyProfilingOption = parser.accepts("tracy");
        OptionSpecBuilder tracyNoImageOption = parser.accepts("tracyNoImages");
        ArgumentAcceptingOptionSpec quickPlayPathOption = parser.accepts("quickPlayPath").withRequiredArg();
        ArgumentAcceptingOptionSpec quickPlaySingleplayerOption = parser.accepts("quickPlaySingleplayer").withOptionalArg();
        ArgumentAcceptingOptionSpec quickPlayMultiplayerOption = parser.accepts("quickPlayMultiplayer").withRequiredArg();
        ArgumentAcceptingOptionSpec quickPlayRealmsOption = parser.accepts("quickPlayRealms").withRequiredArg();
        ArgumentAcceptingOptionSpec gameDirOption = parser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo((Object)new File("."), (Object[])new File[0]);
        ArgumentAcceptingOptionSpec assetsDirOption = parser.accepts("assetsDir").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec resourcePackDirOption = parser.accepts("resourcePackDir").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec proxyHostOption = parser.accepts("proxyHost").withRequiredArg();
        ArgumentAcceptingOptionSpec proxyPortOption = parser.accepts("proxyPort").withRequiredArg().defaultsTo((Object)"8080", (Object[])new String[0]).ofType(Integer.class);
        ArgumentAcceptingOptionSpec proxyUserOption = parser.accepts("proxyUser").withRequiredArg();
        ArgumentAcceptingOptionSpec proxyPassOption = parser.accepts("proxyPass").withRequiredArg();
        ArgumentAcceptingOptionSpec usernameOption = parser.accepts("username").withRequiredArg().defaultsTo((Object)("Player" + System.currentTimeMillis() % 1000L), (Object[])new String[0]);
        OptionSpecBuilder offlineDeveloperMode = parser.accepts("offlineDeveloperMode");
        ArgumentAcceptingOptionSpec uuidOption = parser.accepts("uuid").withRequiredArg();
        ArgumentAcceptingOptionSpec xuidOption = parser.accepts("xuid").withOptionalArg().defaultsTo((Object)"", (Object[])new String[0]);
        ArgumentAcceptingOptionSpec clientIdOption = parser.accepts("clientId").withOptionalArg().defaultsTo((Object)"", (Object[])new String[0]);
        ArgumentAcceptingOptionSpec accessTokenOption = parser.accepts("accessToken").withRequiredArg().required();
        ArgumentAcceptingOptionSpec versionOption = parser.accepts("version").withRequiredArg().required();
        ArgumentAcceptingOptionSpec widthOption = parser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo((Object)854, (Object[])new Integer[0]);
        ArgumentAcceptingOptionSpec heightOption = parser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo((Object)480, (Object[])new Integer[0]);
        ArgumentAcceptingOptionSpec fullscreenWidthOption = parser.accepts("fullscreenWidth").withRequiredArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec fullscreenHeightOption = parser.accepts("fullscreenHeight").withRequiredArg().ofType(Integer.class);
        ArgumentAcceptingOptionSpec assetIndexOption = parser.accepts("assetIndex").withRequiredArg();
        ArgumentAcceptingOptionSpec versionTypeString = parser.accepts("versionType").withRequiredArg().defaultsTo((Object)"release", (Object[])new String[0]);
        NonOptionArgumentSpec nonOption = parser.nonOptions();
        try {
            optionSet = parser.parse(args);
        }
        catch (Throwable t) {
            Main.logEarlyException(t);
            System.exit(-5);
            return;
        }
        File gameDir = (File)Main.parseArgument(optionSet, gameDirOption);
        String launchedVersion = (String)Main.parseArgument(optionSet, versionOption);
        try {
            NativeLibrariesBootstrap.loadLibraries();
        }
        catch (Throwable t) {
            CrashReport report2 = CrashReport.forThrowable(t, "Loading native libraries");
            CrashReportCategory initialization = report2.addCategory("Initialization");
            NativeModuleLister.addCrashSection(initialization);
            Minecraft.fillReport(null, null, launchedVersion, null, report2);
            Minecraft.crash(null, gameDir, report2, -3);
            return;
        }
        String stage = "Pre-bootstrap";
        try {
            if (optionSet.has((OptionSpec)jfrProfilingOption)) {
                JvmProfiler.INSTANCE.start(Environment.CLIENT);
            }
            if (optionSet.has((OptionSpec)tracyProfilingOption)) {
                TracyBootstrap.setup();
            }
            Stopwatch totalTimePreClassLoadTimer = Stopwatch.createStarted((Ticker)Ticker.systemTicker());
            Stopwatch preWindowPreClassLoadTimer = Stopwatch.createStarted((Ticker)Ticker.systemTicker());
            GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS, totalTimePreClassLoadTimer);
            GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS, preWindowPreClassLoadTimer);
            TracyClient.reportAppInfo((String)("Minecraft Java Edition " + SharedConstants.getCurrentVersion().name()));
            CompletableFuture<?> dataFixerOptimization = DataFixers.optimize(DataFixTypes.TYPES_FOR_LEVEL_LIST);
            CrashReport.preload();
            logger = LogUtils.getLogger();
            stage = "Bootstrap";
            Bootstrap.bootStrap();
            ClientBootstrap.bootstrap();
            GameLoadTimesEvent.INSTANCE.setBootstrapTime(Bootstrap.bootstrapDuration.get());
            Bootstrap.validate();
            stage = "Argument parsing";
            List leftoverArgs = optionSet.valuesOf((OptionSpec)nonOption);
            if (!leftoverArgs.isEmpty()) {
                logger.info("Completely ignored arguments: {}", (Object)leftoverArgs);
            }
            String hostName = (String)Main.parseArgument(optionSet, proxyHostOption);
            Proxy proxy = Proxy.NO_PROXY;
            if (hostName != null) {
                try {
                    proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, (int)((Integer)Main.parseArgument(optionSet, proxyPortOption))));
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            final String proxyUser = (String)Main.parseArgument(optionSet, proxyUserOption);
            final String proxyPass = (String)Main.parseArgument(optionSet, proxyPassOption);
            if (!proxy.equals(Proxy.NO_PROXY) && Main.stringHasValue(proxyUser) && Main.stringHasValue(proxyPass)) {
                Authenticator.setDefault(new Authenticator(){

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                    }
                });
            }
            int width = (Integer)Main.parseArgument(optionSet, widthOption);
            int height = (Integer)Main.parseArgument(optionSet, heightOption);
            OptionalInt fullscreenWidth = Main.ofNullable((Integer)Main.parseArgument(optionSet, fullscreenWidthOption));
            OptionalInt fullscreenHeight = Main.ofNullable((Integer)Main.parseArgument(optionSet, fullscreenHeightOption));
            boolean isFullscreen = optionSet.has("fullscreen");
            boolean isDemo = optionSet.has("demo");
            boolean disableMultiplayer = optionSet.has("disableMultiplayer");
            boolean disableChat = optionSet.has("disableChat");
            boolean captureTracyImages = !optionSet.has((OptionSpec)tracyNoImageOption);
            boolean renderDebugLabels = optionSet.has((OptionSpec)renderDebugLabelsOption);
            String versionType = (String)Main.parseArgument(optionSet, versionTypeString);
            boolean vulkanValidation = optionSet.has((OptionSpec)vulkanValidationOption);
            PreferredGraphicsApi forcedGraphicsApi = (PreferredGraphicsApi)Main.parseArgument(optionSet, graphicsBackendOption);
            File assetsDir = optionSet.has((OptionSpec)assetsDirOption) ? (File)Main.parseArgument(optionSet, assetsDirOption) : new File(gameDir, "assets/");
            File resourcePackDir = optionSet.has((OptionSpec)resourcePackDirOption) ? (File)Main.parseArgument(optionSet, resourcePackDirOption) : new File(gameDir, "resourcepacks/");
            UUID uuid = Main.hasValidUuid((OptionSpec<String>)uuidOption, optionSet, logger) ? UndashedUuid.fromStringLenient((String)((String)uuidOption.value(optionSet))) : UUIDUtil.createOfflinePlayerUUID((String)usernameOption.value(optionSet));
            String assetIndex = optionSet.has((OptionSpec)assetIndexOption) ? (String)assetIndexOption.value(optionSet) : null;
            String xuid = (String)optionSet.valueOf((OptionSpec)xuidOption);
            String clientId = (String)optionSet.valueOf((OptionSpec)clientIdOption);
            String quickPlayLogPath = (String)Main.parseArgument(optionSet, quickPlayPathOption);
            GameConfig.QuickPlayVariant quickPlayVariant = Main.getQuickPlayVariant(optionSet, (OptionSpec<String>)quickPlaySingleplayerOption, (OptionSpec<String>)quickPlayMultiplayerOption, (OptionSpec<String>)quickPlayRealmsOption);
            User user = new User((String)usernameOption.value(optionSet), uuid, (String)accessTokenOption.value(optionSet), Main.emptyStringToEmptyOptional(xuid), Main.emptyStringToEmptyOptional(clientId));
            gameConfig = new GameConfig(new GameConfig.UserData(user, proxy), new DisplayData(width, height, fullscreenWidth, fullscreenHeight, isFullscreen), new GameConfig.FolderData(gameDir, resourcePackDir, assetsDir, assetIndex), new GameConfig.GameData(isDemo, launchedVersion, versionType, disableMultiplayer, disableChat, captureTracyImages, vulkanValidation, renderDebugLabels, forcedGraphicsApi, optionSet.has((OptionSpec)offlineDeveloperMode)), new GameConfig.QuickPlayData(quickPlayLogPath, quickPlayVariant));
            Util.startTimerHackThread();
            dataFixerOptimization.join();
        }
        catch (Throwable t) {
            CrashReport report3 = CrashReport.forThrowable(t, stage);
            CrashReportCategory initialization = report3.addCategory("Initialization");
            NativeModuleLister.addCrashSection(initialization);
            Minecraft.fillReport(null, null, launchedVersion, null, report3);
            Minecraft.crash(null, gameDir, report3, -1);
            return;
        }
        Thread shutdownThread = new Thread("Client Shutdown Thread"){

            @Override
            public void run() {
                Minecraft instance = Minecraft.getInstance();
                if (instance == null) {
                    return;
                }
                IntegratedServer server = instance.getSingleplayerServer();
                if (server != null) {
                    server.halt(true);
                }
            }
        };
        shutdownThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(logger));
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        Minecraft minecraft = null;
        try {
            Thread.currentThread().setName("Render thread");
            RenderSystem.initRenderThread();
            minecraft = new Minecraft(gameConfig);
        }
        catch (SilentInitException e) {
            Util.shutdownExecutors();
            logger.warn("Failed to create window: ", (Throwable)e);
            return;
        }
        catch (Throwable t) {
            report = CrashReport.forThrowable(t, "Initializing game");
            CrashReportCategory initialization = report.addCategory("Initialization");
            NativeModuleLister.addCrashSection(initialization);
            Minecraft.fillReport(minecraft, null, gameConfig.game.launchVersion, null, report);
            Minecraft.crash(minecraft, gameConfig.location.gameDirectory, report, -1);
            return;
        }
        minecraft.run();
        try {
            minecraft.exitWorldAndClose();
        }
        catch (Throwable t) {
            report = CrashReport.forThrowable(t, "Game shutdown");
            Minecraft.fillReport(null, null, launchedVersion, null, report);
            Minecraft.crash(null, gameDir, report, -6);
            return;
        }
        ClientShutdownWatchdog.startShutdownWatchdog("post-main", true, null, gameConfig, Thread.currentThread().threadId());
    }

    @SuppressForbidden(reason="Logging not available yet")
    private static void logEarlyException(Throwable t) {
        t.printStackTrace();
    }

    private static GameConfig.QuickPlayVariant getQuickPlayVariant(OptionSet optionSet, OptionSpec<String> quickPlaySingleplayerOption, OptionSpec<String> quickPlayMultiplayerOption, OptionSpec<String> quickPlayRealmsOption) {
        long enabledOptions = Stream.of(quickPlaySingleplayerOption, quickPlayMultiplayerOption, quickPlayRealmsOption).filter(arg_0 -> ((OptionSet)optionSet).has(arg_0)).count();
        if (enabledOptions == 0L) {
            return GameConfig.QuickPlayVariant.DISABLED;
        }
        if (enabledOptions > 1L) {
            throw new IllegalArgumentException("Only one quick play option can be specified");
        }
        if (optionSet.has(quickPlaySingleplayerOption)) {
            String worldId = Main.unescapeJavaArgument(Main.parseArgument(optionSet, quickPlaySingleplayerOption));
            return new GameConfig.QuickPlaySinglePlayerData(worldId);
        }
        if (optionSet.has(quickPlayMultiplayerOption)) {
            String serverAddress = Main.unescapeJavaArgument(Main.parseArgument(optionSet, quickPlayMultiplayerOption));
            return Optionull.mapOrDefault(serverAddress, GameConfig.QuickPlayMultiplayerData::new, GameConfig.QuickPlayVariant.DISABLED);
        }
        if (optionSet.has(quickPlayRealmsOption)) {
            String realmId = Main.unescapeJavaArgument(Main.parseArgument(optionSet, quickPlayRealmsOption));
            return Optionull.mapOrDefault(realmId, GameConfig.QuickPlayRealmsData::new, GameConfig.QuickPlayVariant.DISABLED);
        }
        return GameConfig.QuickPlayVariant.DISABLED;
    }

    private static @Nullable String unescapeJavaArgument(@Nullable String arg) {
        if (arg == null) {
            return null;
        }
        return StringEscapeUtils.unescapeJava((String)arg);
    }

    private static Optional<String> emptyStringToEmptyOptional(String xuid) {
        return xuid.isEmpty() ? Optional.empty() : Optional.of(xuid);
    }

    private static OptionalInt ofNullable(@Nullable Integer value) {
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private static <T> @Nullable T parseArgument(OptionSet optionSet, OptionSpec<T> optionSpec) {
        try {
            return (T)optionSet.valueOf(optionSpec);
        }
        catch (Throwable t) {
            ArgumentAcceptingOptionSpec options;
            List defaultValues;
            if (optionSpec instanceof ArgumentAcceptingOptionSpec && !(defaultValues = (options = (ArgumentAcceptingOptionSpec)optionSpec).defaultValues()).isEmpty()) {
                return (T)defaultValues.get(0);
            }
            throw t;
        }
    }

    private static boolean stringHasValue(@Nullable String string) {
        return string != null && !string.isEmpty();
    }

    private static boolean hasValidUuid(OptionSpec<String> uuidOption, OptionSet optionSet, Logger logger) {
        return optionSet.has(uuidOption) && Main.isUuidValid(uuidOption, optionSet, logger);
    }

    private static boolean isUuidValid(OptionSpec<String> uuidOption, OptionSet optionSet, Logger logger) {
        try {
            UndashedUuid.fromStringLenient((String)((String)uuidOption.value(optionSet)));
        }
        catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID: '{}", uuidOption.value(optionSet));
            return false;
        }
        return true;
    }

    static {
        System.setProperty("java.awt.headless", "true");
    }
}

