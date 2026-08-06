/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  joptsimple.OptionParser
 *  joptsimple.OptionSet
 *  joptsimple.OptionSpec
 *  org.apache.commons.io.FileUtils
 *  org.slf4j.Logger
 */
package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SuppressForbidden;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class GameTestMainUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_UNIVERSE_DIR = "gametestserver";
    private static final String LEVEL_NAME = "gametestworld";
    private static final OptionParser parser = new OptionParser();
    private static final OptionSpec<String> universe = parser.accepts("universe", "The path to where the test server world will be created. Any existing folder will be replaced.").withRequiredArg().defaultsTo((Object)"gametestserver", (Object[])new String[0]);
    private static final OptionSpec<File> report = parser.accepts("report", "Exports results in a junit-like XML report at the given path.").withRequiredArg().ofType(File.class);
    private static final OptionSpec<String> tests = parser.accepts("tests", "Which test(s) to run (namespaced ID selector using wildcards). Empty means run all.").withRequiredArg();
    private static final OptionSpec<Boolean> verify = parser.accepts("verify", "Runs the tests specified with `test` or `testNamespace` 100 times for each 90 degree rotation step").withRequiredArg().ofType(Boolean.class).defaultsTo((Object)false, (Object[])new Boolean[0]);
    private static final OptionSpec<Integer> repeatCount = parser.accepts("repeatCount", "Runs each of the specified tests this many times").withRequiredArg().ofType(Integer.class).defaultsTo((Object)1, (Object[])new Integer[0]);
    private static final OptionSpec<String> packs = parser.accepts("packs", "A folder of datapacks to include in the world").withRequiredArg();
    private static final OptionSpec<Void> help = parser.accepts("help").forHelp();

    @SuppressForbidden(reason="Using System.err due to no bootstrap")
    public static void runGameTestServer(String[] args, Consumer<String> onUniverseCreated) throws Exception {
        parser.allowsUnrecognizedOptions();
        OptionSet options = parser.parse(args);
        if (options.has(help)) {
            parser.printHelpOn((OutputStream)System.err);
            return;
        }
        if (((Boolean)options.valueOf(verify)).booleanValue() && !options.has(tests)) {
            LOGGER.error("Please specify a test selection to run the verify option. For example: --verify --tests example:test_something_*");
            System.exit(-1);
        }
        if (((Boolean)options.valueOf(verify)).booleanValue() && options.has(repeatCount)) {
            LOGGER.info("Flag --verify is true, the --repeatCount value will be ignored");
        }
        LOGGER.info("Running GameTestMain with cwd '{}', universe path '{}'", (Object)System.getProperty("user.dir"), options.valueOf(universe));
        if (options.has(report)) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter((File)report.value(options)));
        }
        Bootstrap.bootStrap();
        Util.startTimerHackThread();
        String universePath = (String)options.valueOf(universe);
        GameTestMainUtil.createOrResetDir(universePath);
        onUniverseCreated.accept(universePath);
        if (options.has(packs)) {
            String packFolder = (String)options.valueOf(packs);
            GameTestMainUtil.copyPacks(universePath, packFolder);
        }
        LevelStorageSource.LevelStorageAccess levelStorageSource = LevelStorageSource.createDefault(Paths.get(universePath, new String[0])).createAccess(LEVEL_NAME);
        PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorageSource);
        MinecraftServer.spin(thread -> GameTestServer.create(thread, levelStorageSource, packRepository, GameTestMainUtil.optionalFromOption(options, tests), (Boolean)options.valueOf(verify), (Integer)options.valueOf(repeatCount)));
    }

    private static Optional<String> optionalFromOption(OptionSet options, OptionSpec<String> option) {
        return options.has(option) ? Optional.of((String)options.valueOf(option)) : Optional.empty();
    }

    private static void createOrResetDir(String universePath) throws IOException {
        Path universeDir = Paths.get(universePath, new String[0]);
        if (Files.exists(universeDir, new LinkOption[0])) {
            FileUtils.deleteDirectory((File)universeDir.toFile());
        }
        Files.createDirectories(universeDir, new FileAttribute[0]);
    }

    private static void copyPacks(String serverPath, String packSourcePath) throws IOException {
        Path sourceFolder;
        Path worldPackFolder = Paths.get(serverPath, new String[0]).resolve(LEVEL_NAME).resolve("datapacks");
        if (!Files.exists(worldPackFolder, new LinkOption[0])) {
            Files.createDirectories(worldPackFolder, new FileAttribute[0]);
        }
        if (Files.exists(sourceFolder = Paths.get(packSourcePath, new String[0]), new LinkOption[0])) {
            try (Stream<Path> list = Files.list(sourceFolder);){
                for (Path path : list.toList()) {
                    Path destination = worldPackFolder.resolve(path.getFileName());
                    if (Files.isDirectory(path, new LinkOption[0])) {
                        if (!Files.isRegularFile(path.resolve("pack.mcmeta"), new LinkOption[0])) continue;
                        FileUtils.copyDirectory((File)path.toFile(), (File)destination.toFile());
                        LOGGER.info("Included folder pack {}", (Object)path.getFileName());
                        continue;
                    }
                    if (!path.toString().endsWith(".zip")) continue;
                    Files.copy(path, destination, new CopyOption[0]);
                    LOGGER.info("Included zip pack {}", (Object)path.getFileName());
                }
            }
        }
    }
}

