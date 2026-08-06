/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.logging.LogUtils
 *  com.sun.jna.Memory
 *  com.sun.jna.Native
 *  com.sun.jna.Platform
 *  com.sun.jna.Pointer
 *  com.sun.jna.platform.win32.Kernel32
 *  com.sun.jna.platform.win32.Kernel32Util
 *  com.sun.jna.platform.win32.Tlhelp32$MODULEENTRY32W
 *  com.sun.jna.platform.win32.Version
 *  com.sun.jna.platform.win32.Win32Exception
 *  com.sun.jna.ptr.IntByReference
 *  com.sun.jna.ptr.PointerByReference
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class NativeModuleLister {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LANG_MASK = 65535;
    private static final int DEFAULT_LANG = 1033;
    private static final int CODEPAGE_MASK = -65536;
    private static final int DEFAULT_CODEPAGE = 0x4B00000;

    public static List<NativeModuleInfo> listModules() {
        if (!Platform.isWindows()) {
            return ImmutableList.of();
        }
        int selfHandle = Kernel32.INSTANCE.GetCurrentProcessId();
        ImmutableList.Builder result = ImmutableList.builder();
        List modules = Kernel32Util.getModules((int)selfHandle);
        for (Tlhelp32.MODULEENTRY32W module : modules) {
            String name = module.szModule();
            Optional<NativeModuleVersion> versionInfo = NativeModuleLister.tryGetModuleVersion(module.szExePath());
            result.add((Object)new NativeModuleInfo(name, versionInfo));
        }
        return result.build();
    }

    public static Optional<NativeModuleVersion> tryGetModuleVersion(String path) {
        if (!Platform.isWindows()) {
            return Optional.empty();
        }
        try {
            IntByReference dwDummy = new IntByReference();
            int versionLength = Version.INSTANCE.GetFileVersionInfoSize(path, dwDummy);
            if (versionLength == 0) {
                int lastError = Native.getLastError();
                if (lastError == 1813 || lastError == 1812) {
                    return Optional.empty();
                }
                throw new Win32Exception(lastError);
            }
            Memory lpData = new Memory((long)versionLength);
            if (!Version.INSTANCE.GetFileVersionInfo(path, 0, versionLength, (Pointer)lpData)) {
                throw new Win32Exception(Native.getLastError());
            }
            IntByReference size = new IntByReference();
            Pointer translationsBuffer = NativeModuleLister.queryVersionValue((Pointer)lpData, "\\VarFileInfo\\Translation", size);
            int[] langsAndCodepages = translationsBuffer.getIntArray(0L, size.getValue() / 4);
            OptionalInt maybeLangAndCodepage = NativeModuleLister.findLangAndCodepage(langsAndCodepages);
            if (maybeLangAndCodepage.isEmpty()) {
                return Optional.empty();
            }
            int langAndCodepage = maybeLangAndCodepage.getAsInt();
            int lang = langAndCodepage & 0xFFFF;
            int codepage = (langAndCodepage & 0xFFFF0000) >> 16;
            String description = NativeModuleLister.queryVersionString((Pointer)lpData, NativeModuleLister.langTableKey("FileDescription", lang, codepage), size);
            String companyName = NativeModuleLister.queryVersionString((Pointer)lpData, NativeModuleLister.langTableKey("CompanyName", lang, codepage), size);
            String fileVersion = NativeModuleLister.queryVersionString((Pointer)lpData, NativeModuleLister.langTableKey("FileVersion", lang, codepage), size);
            return Optional.of(new NativeModuleVersion(description, fileVersion, companyName));
        }
        catch (Exception e) {
            LOGGER.info("Failed to find module info for {}", (Object)path, (Object)e);
            return Optional.empty();
        }
    }

    private static String langTableKey(String key, int lang, int codepage) {
        return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", lang, codepage, key);
    }

    private static OptionalInt findLangAndCodepage(int[] langsAndCodepages) {
        OptionalInt bestSoFar = OptionalInt.empty();
        for (int langAndCodepage : langsAndCodepages) {
            if ((langAndCodepage & 0xFFFF0000) == 0x4B00000 && (langAndCodepage & 0xFFFF) == 1033) {
                return OptionalInt.of(langAndCodepage);
            }
            bestSoFar = OptionalInt.of(langAndCodepage);
        }
        return bestSoFar;
    }

    private static Pointer queryVersionValue(Pointer lpData, String key, IntByReference outSize) {
        PointerByReference lplpBuffer = new PointerByReference();
        if (!Version.INSTANCE.VerQueryValue(lpData, key, lplpBuffer, outSize)) {
            throw new UnsupportedOperationException("Can't get version value " + key);
        }
        return lplpBuffer.getValue();
    }

    private static String queryVersionString(Pointer lpData, String key, IntByReference outSize) {
        try {
            Pointer ptr = NativeModuleLister.queryVersionValue(lpData, key, outSize);
            byte[] result = ptr.getByteArray(0L, (outSize.getValue() - 1) * 2);
            return NativeModuleLister.sanitize(new String(result, StandardCharsets.UTF_16LE));
        }
        catch (Exception e) {
            return "";
        }
    }

    private static String sanitize(String input) {
        return Util.CONTROL_CHARACTER_ESCAPER.escape(input);
    }

    public static void addCrashSection(CrashReportCategory category) {
        category.setDetail("Modules", () -> NativeModuleLister.listModules().stream().sorted(Comparator.comparing(module -> module.name)).map(e -> "\n\t\t" + String.valueOf(e)).collect(Collectors.joining()));
    }

    public record NativeModuleInfo(String name, Optional<NativeModuleVersion> version) {
        @Override
        public String toString() {
            return this.version.map(v -> this.name + ":" + String.valueOf(v)).orElse(this.name);
        }
    }

    public record NativeModuleVersion(String description, String version, String company) {
        @Override
        public String toString() {
            return this.description + ":" + this.version + ":" + this.company;
        }
    }
}

