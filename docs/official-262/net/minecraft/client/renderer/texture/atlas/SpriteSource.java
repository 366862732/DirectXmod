/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface SpriteSource {
    public static final FileToIdConverter TEXTURE_ID_CONVERTER = new FileToIdConverter("textures", ".png");

    public void run(ResourceManager var1, Output var2);

    public MapCodec<? extends SpriteSource> codec();

    @Environment(value=EnvType.CLIENT)
    public static interface DiscardableLoader
    extends Loader {
        default public void discard() {
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Loader {
        public @Nullable SpriteContents get(SpriteResourceLoader var1);
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Output {
        default public void add(Identifier id, Resource resource) {
            this.add(id, loader -> loader.loadSprite(id, resource));
        }

        public void add(Identifier var1, DiscardableLoader var2);

        public void removeAll(Predicate<Identifier> var1);
    }
}

