/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record DeviceInfo(String name, String vendorName, String driverInfo, boolean isZZeroToOne, String backendName, float timestampPeriod, DeviceLimits limits, DeviceFeatures features, Set<String> underlyingExtensions, HintsAndWorkarounds hintsAndWorkarounds, DeviceType type) {
}

