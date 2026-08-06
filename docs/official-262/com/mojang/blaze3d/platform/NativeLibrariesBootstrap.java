/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.primitives.Ints
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.output.TeeOutputStream
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.Version
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.openal.ALC
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.stb.STBImage
 *  org.lwjgl.system.Configuration
 *  org.lwjgl.system.Library
 *  org.lwjgl.system.Platform
 *  org.lwjgl.util.freetype.FreeType
 *  org.lwjgl.util.shaderc.Shaderc
 *  org.lwjgl.util.spvc.Spvc
 *  org.lwjgl.util.tinyfd.TinyFileDialogs
 *  org.lwjgl.util.vma.Vma
 *  org.lwjgl.vulkan.VK
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.RandomSource;
import org.apache.commons.io.output.TeeOutputStream;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.ALC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;
import org.lwjgl.util.freetype.FreeType;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class NativeLibrariesBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
    private static boolean vulkanLoaderAvailable;

    public static void loadLibraries() throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        NativeLibrariesBootstrap.configureLWJGLLibraryPath();
        NativeLibrariesBootstrap.createAndCheckDirectory((String)Configuration.SHARED_LIBRARY_EXTRACT_PATH.get((Object)""));
        NativeLibrariesBootstrap.createAndCheckDirectory(System.getProperty("jna.tmpdir", ""));
        NativeLibrariesBootstrap.createAndCheckDirectory(System.getProperty("io.netty.native.workdir", ""));
        Boolean originalDebugLoader = (Boolean)Configuration.DEBUG_LOADER.get();
        Configuration.DEBUG_LOADER.set((Object)true);
        Supplier<String> stopCapturing = NativeLibrariesBootstrap.setupLWJGLCapture();
        int libraryIndex = -1;
        ArrayList<LibraryLoadEntry> entries = new ArrayList<LibraryLoadEntry>();
        try {
            if (SharedConstants.DEBUG_SIMULATE_LIBRARY_LOAD_FAILURE) {
                throw new UnsatisfiedLinkError("Simulated debug crash");
            }
            NativeLibrariesBootstrap.loadLibrary(stopCapturing, "LWJGL system", NativeLibrariesBootstrap::loadLWJGLSystem);
            vulkanLoaderAvailable = NativeLibrariesBootstrap.tryLoadingVulkan();
            entries.add(new LibraryLoadEntry("GLFW", NativeLibrariesBootstrap::loadGlfw));
            entries.add(new LibraryLoadEntry("OpenGL", NativeLibrariesBootstrap::loadOpenGL));
            entries.add(new LibraryLoadEntry("OpenAL", NativeLibrariesBootstrap::loadOpenAL));
            entries.add(new LibraryLoadEntry("STB", NativeLibrariesBootstrap::loadSTB));
            entries.add(new LibraryLoadEntry("tinyfd", NativeLibrariesBootstrap::loadTinyFD));
            entries.add(new LibraryLoadEntry("freetype", NativeLibrariesBootstrap::loadFreeType));
            if (vulkanLoaderAvailable) {
                entries.add(new LibraryLoadEntry("shaderc", NativeLibrariesBootstrap::loadShaderc));
                entries.add(new LibraryLoadEntry("spvc", NativeLibrariesBootstrap::loadSpvc));
                entries.add(new LibraryLoadEntry("vma", NativeLibrariesBootstrap::loadVma));
            }
            Collections.shuffle(entries);
            for (libraryIndex = 0; libraryIndex < entries.size(); ++libraryIndex) {
                LibraryLoadEntry e = (LibraryLoadEntry)entries.get(libraryIndex);
                NativeLibrariesBootstrap.loadLibrary(stopCapturing, e.name(), e.loader());
            }
        }
        catch (Throwable t) {
            CrashReport crashReport = CrashReport.forThrowable(t, "Loading libraries");
            CrashReportCategory librariesLoaded = crashReport.addCategory("Libraries loaded");
            librariesLoaded.setDetail("Loading order", () -> entries.stream().map(LibraryLoadEntry::name).collect(Collectors.joining(",")));
            librariesLoaded.setDetail("Loading index", Integer.toString(libraryIndex));
            throw new ReportedException(crashReport);
        }
        finally {
            stopCapturing.get();
            Configuration.DEBUG_LOADER.set((Object)originalDebugLoader);
        }
        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        LOGGER.debug("Library load time: {} ms", (Object)elapsed);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void createAndCheckDirectory(String libraryDir) throws IOException {
        if (libraryDir.isEmpty()) {
            return;
        }
        Path libraryDirPath = Path.of(libraryDir, new String[0]);
        Files.createDirectories(libraryDirPath, new FileAttribute[0]);
        RandomSource randomSource = RandomSource.createThreadLocalInstance();
        String trollFileName = System.mapLibraryName("VeryImportant" + randomSource.nextInt(9999));
        Path probeFile = libraryDirPath.resolve(trollFileName);
        byte[] expectedBytes = Ints.toByteArray((int)randomSource.nextInt());
        Files.write(probeFile, expectedBytes, new OpenOption[0]);
        try (FileChannel fc = FileChannel.open(probeFile, new OpenOption[0]);
             FileLock lock = NativeLibrariesBootstrap.tryLock(fc);){
            if (lock == null) {
                throw new IOException("Failed to lock " + String.valueOf(probeFile));
            }
            byte[] readBytes = Channels.newInputStream(fc).readAllBytes();
            if (!Arrays.equals(expectedBytes, readBytes)) {
                throw new IOException("Unexpected probe file contents, expected '" + HEX_FORMAT.formatHex(expectedBytes) + "', but got '" + HEX_FORMAT.formatHex(readBytes) + "'");
            }
        }
        finally {
            Files.delete(probeFile);
        }
    }

    private static @Nullable FileLock tryLock(FileChannel fc) throws IOException {
        for (int i = 0; i < 5; ++i) {
            FileLock lock = fc.tryLock(0L, Long.MAX_VALUE, true);
            if (lock != null) {
                return lock;
            }
            try {
                Thread.sleep(10L);
                continue;
            }
            catch (InterruptedException interruptedException) {
                break;
            }
        }
        return null;
    }

    private static void configureLWJGLLibraryPath() {
        String libraryPathString = (String)Configuration.SHARED_LIBRARY_EXTRACT_PATH.get();
        if (libraryPathString != null) {
            String version = Version.getVersion().replace(' ', '-');
            String arch = Platform.getArchitecture().name().toLowerCase(Locale.ROOT);
            Path newLibraryDir = Path.of(libraryPathString, new String[0]).resolve(version, arch);
            Configuration.SHARED_LIBRARY_EXTRACT_PATH.set((Object)newLibraryDir.toString());
        }
    }

    @SuppressForbidden(reason="System.out needed before bootstrap")
    private static Supplier<String> setupLWJGLCapture() {
        if (Configuration.DEBUG_STREAM.get() != null || ((Boolean)Configuration.DEBUG.get((Object)false)).booleanValue()) {
            return () -> "<LWJGL debug enabled, not capturing>";
        }
        CapturingPrintStream capturingPrintStream = new CapturingPrintStream(System.out);
        Configuration.DEBUG_STREAM.set((Object)capturingPrintStream);
        capturingPrintStream.startCapturing();
        return capturingPrintStream::stopCapturing;
    }

    private static void loadLibrary(Supplier<String> debugCapture, String name, Runnable loader) {
        try {
            LOGGER.debug("Loading {}", (Object)name);
            loader.run();
        }
        catch (Throwable t) {
            CrashReport crashReport = CrashReport.forThrowable(t, "Loading library " + name);
            CrashReportCategory libraryInfoCategory = crashReport.addCategory("Library directory contents");
            String systemPropertyDir = System.getProperty("java.library.path", "");
            String lwjglPropertyDir = (String)Configuration.LIBRARY_PATH.get((Object)"");
            if (systemPropertyDir.equals(lwjglPropertyDir)) {
                libraryInfoCategory.setDetail("Contents of shared library directory", () -> NativeLibrariesBootstrap.listLibrariesDirectory(systemPropertyDir));
            } else {
                libraryInfoCategory.setDetail("Contents of java.library.path ", () -> NativeLibrariesBootstrap.listLibrariesDirectory(systemPropertyDir));
                libraryInfoCategory.setDetail("Contents of org.lwjgl.librarypath", () -> NativeLibrariesBootstrap.listLibrariesDirectory(lwjglPropertyDir));
            }
            libraryInfoCategory.setDetail("LWJGL platform", () -> Platform.get().toString());
            libraryInfoCategory.setDetail("LWJGL architecture", () -> Platform.getArchitecture().toString());
            CrashReportCategory lwjglDebugLog = crashReport.addCategory("LWJGL debug log");
            try {
                lwjglDebugLog.setDetail("Log", debugCapture.get());
            }
            catch (Throwable e) {
                lwjglDebugLog.setDetail("Log", e);
            }
            throw new ReportedException(crashReport);
        }
    }

    private static String listLibrariesDirectory(@Nullable String libraryDirProperty) throws IOException {
        if (libraryDirProperty == null || libraryDirProperty.isEmpty()) {
            return "<not set>";
        }
        if (libraryDirProperty.contains(";")) {
            return "<multiple directories>";
        }
        Path libraryDirPath = Path.of(libraryDirProperty, new String[0]);
        if (!Files.isDirectory(libraryDirPath, new LinkOption[0])) {
            return "<not a directory>";
        }
        ArrayList<Pair> contents = new ArrayList<Pair>();
        try (DirectoryStream<Path> libraryDir = Files.newDirectoryStream(libraryDirPath);){
            for (Path dirEntry : libraryDir) {
                contents.add(Pair.of((Object)dirEntry, (Object)NativeLibrariesBootstrap.identifyFileContents(dirEntry)));
            }
        }
        if (contents.isEmpty()) {
            return "<empty>";
        }
        return "\n" + contents.stream().map(s -> "\t\t" + String.valueOf(((Path)s.getFirst()).getFileName()) + ": " + (String)s.getSecond()).collect(Collectors.joining("\n"));
    }

    private static String identifyFileContents(Path path) {
        try {
            if (Files.isRegularFile(path, new LinkOption[0])) {
                Optional<String> detailedModuleInfo;
                if (path.getFileName().toString().endsWith(".dll") && (detailedModuleInfo = NativeModuleLister.tryGetModuleVersion(path.toString()).map(NativeModuleLister.NativeModuleVersion::toString)).isPresent()) {
                    return "module: " + detailedModuleInfo.get();
                }
                return Objects.requireNonNullElse(Files.probeContentType(path), "unknown type");
            }
            return "not a file";
        }
        catch (Throwable e) {
            LOGGER.warn("Failed to get details of file {}", (Object)path, (Object)e);
            return "error: " + e.getMessage();
        }
    }

    private static void loadLWJGLSystem() {
        Library.initialize();
    }

    private static void loadGlfw() {
        Objects.requireNonNull(GLFW.getLibrary());
    }

    private static void loadOpenGL() {
        Objects.requireNonNull(GL.getFunctionProvider());
    }

    private static void loadOpenAL() {
        Objects.requireNonNull(ALC.getFunctionProvider());
    }

    private static void loadSTB() {
        String lastStbError = STBImage.stbi_failure_reason();
        if (lastStbError != null) {
            throw new IllegalStateException("No error expected, but got " + lastStbError);
        }
    }

    private static boolean tryLoadingVulkan() {
        if (Configuration.VULKAN_EXPLICIT_INIT.get() == null) {
            Configuration.VULKAN_EXPLICIT_INIT.set((Object)true);
        }
        try {
            VK.create();
            return true;
        }
        catch (Throwable t) {
            LOGGER.warn("Failed to load Vulkan loader", t);
            return false;
        }
    }

    public static boolean isVulkanLoaderAvailable() {
        return vulkanLoaderAvailable;
    }

    private static void loadShaderc() {
        Objects.requireNonNull(Shaderc.getLibrary());
    }

    private static void loadSpvc() {
        Objects.requireNonNull(Spvc.getLibrary());
    }

    private static void loadVma() {
        try {
            Vma.vmaDestroyAllocator((long)0L);
        }
        catch (NullPointerException nullPointerException) {
            // empty catch block
        }
    }

    private static void loadTinyFD() {
        Objects.requireNonNull(TinyFileDialogs.tinyfd_getGlobalChar((CharSequence)"tinyfd_version"));
    }

    private static void loadFreeType() {
        Objects.requireNonNull(FreeType.getLibrary());
    }

    @Environment(value=EnvType.CLIENT)
    private record LibraryLoadEntry(String name, Runnable loader) {
    }

    @Environment(value=EnvType.CLIENT)
    private static class CapturingPrintStream
    extends PrintStream {
        private final CapturingStream collector;

        public CapturingPrintStream(OutputStream out) {
            CapturingStream logStream = new CapturingStream();
            super((OutputStream)new TeeOutputStream(out, (OutputStream)logStream), false, StandardCharsets.UTF_8);
            this.collector = logStream;
        }

        public synchronized void startCapturing() {
            this.collector.buffer = new ByteArrayOutputStream();
        }

        public synchronized String stopCapturing() {
            ByteArrayOutputStream buffer = this.collector.buffer;
            this.collector.buffer = null;
            if (buffer != null) {
                return buffer.toString(StandardCharsets.UTF_8);
            }
            return "";
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class CapturingStream
    extends OutputStream {
        private @Nullable ByteArrayOutputStream buffer;

        private CapturingStream() {
        }

        @Override
        public void write(byte[] b, int off, int len) {
            if (this.buffer != null) {
                this.buffer.write(b, off, len);
            }
        }

        @Override
        public void write(int b) {
            if (this.buffer != null) {
                this.buffer.write(b);
            }
        }
    }
}

