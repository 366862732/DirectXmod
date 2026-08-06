/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntrySystemSpecs
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("system");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        DeviceInfo deviceInfo = RenderSystem.getDevice().getDeviceInfo();
        displayer.addToGroup(GROUP, List.of(String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")), String.format(Locale.ROOT, "CPU: %s", GLX._getCpuInfo()), String.format(Locale.ROOT, "Display: %dx%d (%s)", Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), deviceInfo.vendorName()), String.format(Locale.ROOT, "%s%s", deviceInfo.name(), this.typeName(deviceInfo.type())), String.format(Locale.ROOT, "%s %s", deviceInfo.backendName(), this.firstLine(deviceInfo.driverInfo()))));
    }

    private String firstLine(String value) {
        return value.lines().findFirst().orElse(value);
    }

    private String typeName(DeviceType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case DeviceType.OTHER -> "";
            case DeviceType.INTEGRATED -> " (iGPU)";
            case DeviceType.DISCRETE -> " (dGPU)";
            case DeviceType.VIRTUAL -> " (vGPU)";
            case DeviceType.CPU -> " (software)";
        };
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }
}

