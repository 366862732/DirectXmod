/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Environment(value=EnvType.CLIENT)
public class RenderRegionCache {
    private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap();

    public RenderSectionRegion createRegion(ClientLevel level, long sectionNode) {
        int sectionX = SectionPos.x(sectionNode);
        int sectionY = SectionPos.y(sectionNode);
        int sectionZ = SectionPos.z(sectionNode);
        int minSectionX = sectionX - 1;
        int minSectionY = sectionY - 1;
        int minSectionZ = sectionZ - 1;
        int maxSectionX = sectionX + 1;
        int maxSectionY = sectionY + 1;
        int maxSectionZ = sectionZ + 1;
        SectionCopy[] regionSections = new SectionCopy[27];
        for (int regionSectionZ = minSectionZ; regionSectionZ <= maxSectionZ; ++regionSectionZ) {
            for (int regionSectionY = minSectionY; regionSectionY <= maxSectionY; ++regionSectionY) {
                for (int regionSectionX = minSectionX; regionSectionX <= maxSectionX; ++regionSectionX) {
                    int index = RenderSectionRegion.index(minSectionX, minSectionY, minSectionZ, regionSectionX, regionSectionY, regionSectionZ);
                    regionSections[index] = this.getSectionDataCopy(level, regionSectionX, regionSectionY, regionSectionZ);
                }
            }
        }
        return new RenderSectionRegion(level, minSectionX, minSectionY, minSectionZ, regionSections);
    }

    private SectionCopy getSectionDataCopy(Level level, int sectionX, int sectionY, int sectionZ) {
        return (SectionCopy)this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(sectionX, sectionY, sectionZ), k -> {
            LevelChunk chunk = level.getChunk(sectionX, sectionZ);
            return new SectionCopy(chunk, chunk.getSectionIndexFromSectionY(sectionY));
        });
    }
}

