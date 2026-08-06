/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 *  oshi.SystemInfo
 *  oshi.hardware.CentralProcessor
 *  oshi.hardware.CentralProcessor$ProcessorIdentifier
 *  oshi.hardware.GlobalMemory
 *  oshi.hardware.GraphicsCard
 *  oshi.hardware.HardwareAbstractionLayer
 *  oshi.hardware.PhysicalMemory
 *  oshi.hardware.VirtualMemory
 *  oshi.software.os.OSProcess
 *  oshi.software.os.OperatingSystem
 */
package net.minecraft;

import com.mojang.logging.LogUtils;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class SystemReport {
    public static final long BYTES_PER_MEBIBYTE = 0x100000L;
    private static final long ONE_GIGA = 1000000000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OPERATING_SYSTEM = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version");
    private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
    private static final String JAVA_VM_VERSION = System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor");
    private final List<CrashReportCategory.Entry> entries = new ArrayList<CrashReportCategory.Entry>();

    public SystemReport() {
        this.setDetail("Minecraft Version", () -> SharedConstants.getCurrentVersion().name());
        this.setDetail("Minecraft Version ID", () -> SharedConstants.getCurrentVersion().id());
        this.setDetail("Operating System", OPERATING_SYSTEM);
        this.setDetail("Java Version", JAVA_VERSION);
        this.setDetail("Java VM Version", JAVA_VM_VERSION);
        this.setDetail("Memory", () -> {
            Runtime runtime = Runtime.getRuntime();
            long max = runtime.maxMemory();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long maxMb = max / 0x100000L;
            long totalMb = total / 0x100000L;
            long freeMb = free / 0x100000L;
            return free + " bytes (" + freeMb + " MiB) / " + total + " bytes (" + totalMb + " MiB) up to " + max + " bytes (" + maxMb + " MiB)";
        });
        this.setDetail("Memory (heap)", () -> SystemReport.printMemoryUsage(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()));
        this.setDetail("Memory (non-head)", () -> SystemReport.printMemoryUsage(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()));
        this.setDetail("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
        this.ignoreErrors("hardware", () -> this.putHardware(new SystemInfo()));
        this.ignoreErrors("software", () -> this.putSoftware(new SystemInfo()));
        this.setDetail("JVM Flags", () -> SystemReport.printJvmFlags(arg -> arg.startsWith("-X")));
        this.setDetail("Debug Flags", () -> SystemReport.printJvmFlags(arg -> arg.startsWith("-DMC_DEBUG_")));
    }

    private static String printMemoryUsage(MemoryUsage memoryUsage) {
        return String.format(Locale.ROOT, "init: %03dMiB, used: %03dMiB, committed: %03dMiB, max: %03dMiB", memoryUsage.getInit() / 0x100000L, memoryUsage.getUsed() / 0x100000L, memoryUsage.getCommitted() / 0x100000L, memoryUsage.getMax() / 0x100000L);
    }

    private static String printJvmFlags(Predicate<String> selector) {
        List<String> allArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> selectedArguments = allArguments.stream().filter(selector).toList();
        return String.format(Locale.ROOT, "%d total; %s", selectedArguments.size(), String.join((CharSequence)" ", selectedArguments));
    }

    public void setDetail(String key, String value) {
        this.entries.add(new CrashReportCategory.Entry(key, value));
    }

    public void setDetail(String key, CrashReportDetail<Object> valueSupplier) {
        try {
            this.entries.add(new CrashReportCategory.Entry(key, valueSupplier.call()));
        }
        catch (Throwable t) {
            LOGGER.warn("Failed to get system info for {}", (Object)key, (Object)t);
            this.entries.add(new CrashReportCategory.Entry(key, t));
        }
    }

    private void putHardware(SystemInfo systemInfo) {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        this.ignoreErrors("processor", () -> this.putProcessor(hardware.getProcessor()));
        this.ignoreErrors("graphics", () -> this.putGraphics(hardware.getGraphicsCards()));
        this.ignoreErrors("memory", () -> this.putMemory(hardware.getMemory()));
        this.ignoreErrors("storage", this::putStorage);
    }

    private void putSoftware(SystemInfo systemInfo) {
        OperatingSystem os = systemInfo.getOperatingSystem();
        this.setDetail("Operating System Version", () -> ((OperatingSystem)os).toString());
        this.setDetail("Process Elevated", () -> ((OperatingSystem)os).isElevated());
        this.ignoreErrors("process", () -> this.putProcessDetails(os.getCurrentProcess()));
    }

    private void ignoreErrors(String group, Runnable action) {
        try {
            action.run();
        }
        catch (Throwable t) {
            LOGGER.warn("Failed retrieving info for group {}", (Object)group, (Object)t);
            this.entries.add(new CrashReportCategory.Entry(group, t));
        }
    }

    public static float sizeInMiB(long bytes) {
        return (float)bytes / 1048576.0f;
    }

    private void putPhysicalMemory(List<PhysicalMemory> memoryPackages) {
        int memorySlot = 0;
        for (PhysicalMemory physicalMemory : memoryPackages) {
            String prefix = String.format(Locale.ROOT, "Memory slot #%d ", memorySlot++);
            this.setDetail(prefix + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(physicalMemory.getCapacity()))));
            this.setDetail(prefix + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf((float)physicalMemory.getClockSpeed() / 1.0E9f)));
            this.setDetail(prefix + "type", () -> ((PhysicalMemory)physicalMemory).getMemoryType());
        }
    }

    private void putVirtualMemory(VirtualMemory virtualMemory) {
        this.setDetail("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(virtualMemory.getVirtualMax()))));
        this.setDetail("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(virtualMemory.getVirtualInUse()))));
        this.setDetail("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(virtualMemory.getSwapTotal()))));
        this.setDetail("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(virtualMemory.getSwapUsed()))));
    }

    private void putMemory(GlobalMemory memory) {
        this.ignoreErrors("physical memory", () -> this.putPhysicalMemory(memory.getPhysicalMemory()));
        this.ignoreErrors("virtual memory", () -> this.putVirtualMemory(memory.getVirtualMemory()));
    }

    private void putGraphics(List<GraphicsCard> graphicsCards) {
        int gpuIndex = 0;
        for (GraphicsCard graphicsCard : graphicsCards) {
            String prefix = String.format(Locale.ROOT, "Graphics card #%d ", gpuIndex++);
            this.setDetail(prefix + "name", () -> ((GraphicsCard)graphicsCard).getName());
            this.setDetail(prefix + "vendor", () -> ((GraphicsCard)graphicsCard).getVendor());
            this.setDetail(prefix + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(graphicsCard.getVRam()))));
            this.setDetail(prefix + "deviceId", () -> ((GraphicsCard)graphicsCard).getDeviceId());
            this.setDetail(prefix + "versionInfo", () -> ((GraphicsCard)graphicsCard).getVersionInfo());
        }
    }

    private void putProcessor(CentralProcessor processor) {
        CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
        this.setDetail("Processor Vendor", () -> ((CentralProcessor.ProcessorIdentifier)processorIdentifier).getVendor());
        this.setDetail("Processor Name", () -> ((CentralProcessor.ProcessorIdentifier)processorIdentifier).getName());
        this.setDetail("Identifier", () -> ((CentralProcessor.ProcessorIdentifier)processorIdentifier).getIdentifier());
        this.setDetail("Microarchitecture", () -> ((CentralProcessor.ProcessorIdentifier)processorIdentifier).getMicroarchitecture());
        this.setDetail("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf((float)processorIdentifier.getVendorFreq() / 1.0E9f)));
        this.setDetail("Number of physical packages", () -> String.valueOf(processor.getPhysicalPackageCount()));
        this.setDetail("Number of physical CPUs", () -> String.valueOf(processor.getPhysicalProcessorCount()));
        this.setDetail("Number of logical CPUs", () -> String.valueOf(processor.getLogicalProcessorCount()));
    }

    private void putStorage() {
        this.putSpaceForProperty("jna.tmpdir");
        this.putSpaceForProperty("org.lwjgl.system.SharedLibraryExtractPath");
        this.putSpaceForProperty("io.netty.native.workdir");
        this.putSpaceForProperty("java.io.tmpdir");
        this.putSpaceForPath("workdir", () -> "");
    }

    private void putProcessDetails(OSProcess process) {
        this.setDetail("Process Loads", () -> {
            double userTimeSeconds = (double)process.getUserTime() / 1000.0;
            double kernelTimeSeconds = (double)process.getKernelTime() / 1000.0;
            double upTimeSeconds = (double)process.getUpTime() / 1000.0;
            double totalTimeSeconds = kernelTimeSeconds + userTimeSeconds;
            return String.format(Locale.ROOT, "Uptime: %.0fs, user: %.0fs (%.2f%%), kernel: %.0fs (%.2f%%), total: %.0fs (%.2f%%)", upTimeSeconds, userTimeSeconds, userTimeSeconds / upTimeSeconds * 100.0, kernelTimeSeconds, kernelTimeSeconds / upTimeSeconds * 100.0, totalTimeSeconds, totalTimeSeconds / upTimeSeconds * 100.0);
        });
        this.setDetail("Process Virtual Size (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(process.getVirtualSize()))));
        this.setDetail("Process Resident Size (MiB)", () -> String.format(Locale.ROOT, "%.2f", Float.valueOf(SystemReport.sizeInMiB(process.getResidentSetSize()))));
    }

    private void putSpaceForProperty(String env) {
        this.putSpaceForPath(env, () -> System.getProperty(env));
    }

    private void putSpaceForPath(String id, Supplier<@Nullable String> pathSupplier) {
        String key = "Space in storage for " + id + " (MiB)";
        try {
            String path = pathSupplier.get();
            if (path == null) {
                this.setDetail(key, "<path not set>");
                return;
            }
            FileStore store = Files.getFileStore(Path.of(path, new String[0]));
            this.setDetail(key, () -> String.format(Locale.ROOT, "available: %.2f, total: %.2f", Float.valueOf(SystemReport.sizeInMiB(store.getUsableSpace())), Float.valueOf(SystemReport.sizeInMiB(store.getTotalSpace()))));
        }
        catch (InvalidPathException e) {
            LOGGER.warn("{} is not a path", (Object)id, (Object)e);
            this.setDetail(key, "<invalid path>");
        }
        catch (FileNotFoundException | NoSuchFileException e) {
            this.setDetail(key, "<no such file>");
        }
        catch (Exception e) {
            LOGGER.warn("Failed retrieving storage space for {}", (Object)id, (Object)e);
            this.setDetail(key, "ERR");
        }
    }

    public void appendToCrashReportString(StringBuilder sb) {
        sb.append("-- ").append("System Details").append(" --\n");
        sb.append("Details:");
        this.entries.forEach(entry -> {
            sb.append("\n\t");
            sb.append(entry.key());
            sb.append(": ");
            sb.append(entry.value());
        });
    }

    public String toLineSeparatedString() {
        return this.entries.stream().map(e -> e.key() + ": " + e.value()).collect(Collectors.joining(System.lineSeparator()));
    }
}

