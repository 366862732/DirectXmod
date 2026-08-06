/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LazyLoadedImage {
    private final Identifier id;
    private final Resource resource;
    private final AtomicReference<@Nullable NativeImage> image = new AtomicReference();
    private final AtomicInteger referenceCount;

    public LazyLoadedImage(Identifier id, Resource resource, int count) {
        this.id = id;
        this.resource = resource;
        this.referenceCount = new AtomicInteger(count);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public NativeImage get() throws IOException {
        NativeImage nativeImage = this.image.get();
        if (nativeImage == null) {
            LazyLoadedImage lazyLoadedImage = this;
            synchronized (lazyLoadedImage) {
                nativeImage = this.image.get();
                if (nativeImage == null) {
                    try (InputStream stream = this.resource.open();){
                        nativeImage = NativeImage.read(stream);
                        this.image.set(nativeImage);
                    }
                    catch (IOException e) {
                        throw new IOException("Failed to load image " + String.valueOf(this.id), e);
                    }
                }
            }
        }
        return nativeImage;
    }

    public void release() {
        NativeImage nativeImage;
        int references = this.referenceCount.decrementAndGet();
        if (references <= 0 && (nativeImage = (NativeImage)this.image.getAndSet(null)) != null) {
            nativeImage.close();
        }
    }
}

