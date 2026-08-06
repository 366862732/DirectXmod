/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.FoliageColor;

@Environment(value=EnvType.CLIENT)
public class FoliageColorReloadListener
extends SimplePreparableReloadListener<int[]> {
    private static final Identifier LOCATION = Identifier.withDefaultNamespace("textures/colormap/foliage.png");

    @Override
    protected int[] prepare(ResourceManager manager, ProfilerFiller profiler) {
        try {
            return LegacyStuffWrapper.getPixels(manager, LOCATION);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load foliage color texture", e);
        }
    }

    @Override
    protected void apply(int[] pixels, ResourceManager manager, ProfilerFiller profiler) {
        FoliageColor.init(pixels);
    }
}

