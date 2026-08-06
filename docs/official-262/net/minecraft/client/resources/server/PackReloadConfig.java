/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface PackReloadConfig {
    public void scheduleReload(Callbacks var1);

    @Environment(value=EnvType.CLIENT)
    public static interface Callbacks {
        public void onSuccess();

        public void onFailure(boolean var1);

        public List<IdAndPath> packsToLoad();
    }

    @Environment(value=EnvType.CLIENT)
    public record IdAndPath(UUID id, Path path) {
    }
}

