/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.ArrayUtils
 */
package com.mojang.blaze3d.platform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.lang3.ArrayUtils;

@Environment(value=EnvType.CLIENT)
public enum IconSet {
    RELEASE("icons"),
    SNAPSHOT("icons", "snapshot");

    private final String[] path;

    private IconSet(String ... path) {
        this.path = path;
    }

    public List<IoSupplier<InputStream>> getStandardIcons(PackResources resources) throws IOException {
        return List.of(this.getFile(resources, "icon_16x16.png"), this.getFile(resources, "icon_32x32.png"), this.getFile(resources, "icon_48x48.png"), this.getFile(resources, "icon_128x128.png"), this.getFile(resources, "icon_256x256.png"));
    }

    public IoSupplier<InputStream> getMacIcon(PackResources resources) throws IOException {
        return this.getFile(resources, "minecraft.icns");
    }

    private IoSupplier<InputStream> getFile(PackResources resources, String fileName) throws IOException {
        CharSequence[] fullPath = (String[])ArrayUtils.add((Object[])this.path, (Object)fileName);
        IoSupplier<InputStream> resource = resources.getRootResource((String[])fullPath);
        if (resource == null) {
            throw new FileNotFoundException(String.join((CharSequence)"/", fullPath));
        }
        return resource;
    }
}

