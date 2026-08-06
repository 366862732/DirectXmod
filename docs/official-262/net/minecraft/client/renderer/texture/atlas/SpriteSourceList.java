/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SpriteSourceList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
    private final List<SpriteSource> sources;

    private SpriteSourceList(List<SpriteSource> sources) {
        this.sources = sources;
    }

    public List<SpriteSource.Loader> list(ResourceManager resourceManager) {
        final HashMap sprites = new HashMap();
        SpriteSource.Output output = new SpriteSource.Output(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public void add(Identifier id, SpriteSource.DiscardableLoader sprite) {
                SpriteSource.DiscardableLoader previous = sprites.put(id, sprite);
                if (previous != null) {
                    previous.discard();
                }
            }

            @Override
            public void removeAll(Predicate<Identifier> predicate) {
                Iterator it = sprites.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = it.next();
                    if (!predicate.test((Identifier)entry.getKey())) continue;
                    ((SpriteSource.DiscardableLoader)entry.getValue()).discard();
                    it.remove();
                }
            }
        };
        this.sources.forEach(s -> s.run(resourceManager, output));
        ImmutableList.Builder result = ImmutableList.builder();
        result.add(loader -> MissingTextureAtlasSprite.create());
        result.addAll(sprites.values());
        return result.build();
    }

    public static SpriteSourceList load(ResourceManager resourceManager, Identifier atlasId) {
        Identifier resourceId = ATLAS_INFO_CONVERTER.idToFile(atlasId);
        ArrayList<SpriteSource> loaders = new ArrayList<SpriteSource>();
        for (Resource entry : resourceManager.getResourceStack(resourceId)) {
            try {
                BufferedReader reader = entry.openAsReader();
                try {
                    Dynamic contents = new Dynamic((DynamicOps)JsonOps.INSTANCE, (Object)StrictJsonParser.parse(reader));
                    loaders.addAll((Collection)SpriteSources.FILE_CODEC.parse(contents).getOrThrow());
                }
                finally {
                    if (reader == null) continue;
                    reader.close();
                }
            }
            catch (Exception e) {
                LOGGER.error("Failed to parse atlas definition {} in pack {}", new Object[]{resourceId, entry.sourcePackId(), e});
            }
        }
        return new SpriteSourceList(loaders);
    }
}

