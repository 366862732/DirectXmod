/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.texture;

import java.util.Collection;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.Stitcher;

@Environment(value=EnvType.CLIENT)
public class StitcherException
extends RuntimeException {
    private final Collection<Stitcher.Entry> allSprites;

    public StitcherException(Stitcher.Entry sprite, Collection<Stitcher.Entry> allSprites) {
        super(String.format(Locale.ROOT, "Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", sprite.name(), sprite.width(), sprite.height()));
        this.allSprites = allSprites;
    }

    public Collection<Stitcher.Entry> getAllSprites() {
        return this.allSprites;
    }
}

