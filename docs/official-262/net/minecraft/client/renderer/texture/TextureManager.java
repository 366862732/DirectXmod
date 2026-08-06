/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TextureManager
implements PreparableReloadListener,
AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
    private final Map<Identifier, AbstractTexture> byPath = new HashMap<Identifier, AbstractTexture>();
    private final Set<TickableTexture> tickableTextures = new HashSet<TickableTexture>();
    private final ResourceManager resourceManager;

    public TextureManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        NativeImage checkerboard = MissingTextureAtlasSprite.generateMissingImage();
        this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", checkerboard));
    }

    public void registerAndLoad(Identifier textureId, ReloadableTexture texture) {
        try {
            texture.apply(this.loadContentsSafe(textureId, texture));
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Uploading texture");
            CrashReportCategory category = report.addCategory("Uploaded texture");
            category.setDetail("Resource location", texture.resourceId());
            category.setDetail("Texture id", textureId);
            throw new ReportedException(report);
        }
        this.register(textureId, texture);
    }

    private TextureContents loadContentsSafe(Identifier textureId, ReloadableTexture texture) {
        try {
            return TextureManager.loadContents(this.resourceManager, textureId, texture);
        }
        catch (Exception e) {
            LOGGER.error("Failed to load texture {} into slot {}", new Object[]{texture.resourceId(), textureId, e});
            return TextureContents.createMissing();
        }
    }

    public void registerForNextReload(Identifier location) {
        this.register(location, new SimpleTexture(location));
    }

    public void register(Identifier location, AbstractTexture texture) {
        AbstractTexture prev = this.byPath.put(location, texture);
        if (prev != texture) {
            if (prev != null) {
                this.safeClose(location, prev);
            }
            if (texture instanceof TickableTexture) {
                TickableTexture tickableTexture = (TickableTexture)((Object)texture);
                this.tickableTextures.add(tickableTexture);
            }
        }
    }

    private void safeClose(Identifier id, AbstractTexture texture) {
        this.tickableTextures.remove(texture);
        try {
            texture.close();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to close texture {}", (Object)id, (Object)e);
        }
    }

    public AbstractTexture getTexture(Identifier location) {
        AbstractTexture textureObject = this.byPath.get(location);
        if (textureObject != null) {
            return textureObject;
        }
        SimpleTexture texture = new SimpleTexture(location);
        this.registerAndLoad(location, texture);
        return texture;
    }

    public void tick() {
        for (TickableTexture tickableTexture : this.tickableTextures) {
            tickableTexture.tick();
        }
    }

    public void release(Identifier location) {
        AbstractTexture texture = this.byPath.remove(location);
        if (texture != null) {
            this.safeClose(location, texture);
        }
    }

    @Override
    public void close() {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        ResourceManager manager = currentReload.resourceManager();
        ArrayList reloads = new ArrayList();
        this.byPath.forEach((id, texture) -> {
            if (texture instanceof ReloadableTexture) {
                ReloadableTexture reloadableTexture = (ReloadableTexture)texture;
                reloads.add(TextureManager.scheduleLoad(manager, id, reloadableTexture, taskExecutor));
            }
        });
        return ((CompletableFuture)CompletableFuture.allOf((CompletableFuture[])reloads.stream().map(PendingReload::newContents).toArray(CompletableFuture[]::new)).thenCompose(preparationBarrier::wait)).thenAcceptAsync(unused -> {
            AddRealmPopupScreen.updateCarouselImages(this.resourceManager);
            for (PendingReload reload : reloads) {
                reload.texture.apply(reload.newContents.join());
            }
        }, reloadExecutor);
    }

    public void dumpAllSheets(Path targetDir) {
        try {
            Files.createDirectories(targetDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to create directory {}", (Object)targetDir, (Object)e);
            return;
        }
        this.byPath.forEach((location, texture) -> {
            if (texture instanceof Dumpable) {
                Dumpable dumpable = (Dumpable)((Object)texture);
                try {
                    dumpable.dumpContents((Identifier)location, targetDir);
                }
                catch (Exception e) {
                    LOGGER.error("Failed to dump texture {}", location, (Object)e);
                }
            }
        });
    }

    private static TextureContents loadContents(ResourceManager manager, Identifier location, ReloadableTexture texture) throws IOException {
        try {
            return texture.loadContents(manager);
        }
        catch (FileNotFoundException e) {
            if (location != INTENTIONAL_MISSING_TEXTURE) {
                LOGGER.warn("Missing resource {} referenced from {}", (Object)texture.resourceId(), (Object)location);
            }
            return TextureContents.createMissing();
        }
    }

    private static PendingReload scheduleLoad(ResourceManager manager, Identifier location, ReloadableTexture texture, Executor executor) {
        return new PendingReload(texture, CompletableFuture.supplyAsync(() -> {
            try {
                return TextureManager.loadContents(manager, location, texture);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor));
    }

    @Environment(value=EnvType.CLIENT)
    private record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
    }
}

