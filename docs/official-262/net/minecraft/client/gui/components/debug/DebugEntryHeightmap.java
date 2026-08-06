/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryHeightmap
implements DebugScreenEntry {
    private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Maps.newEnumMap(Map.of(Heightmap.Types.WORLD_SURFACE_WG, "SW", Heightmap.Types.WORLD_SURFACE, "S", Heightmap.Types.OCEAN_FLOOR_WG, "OW", Heightmap.Types.OCEAN_FLOOR, "O", Heightmap.Types.MOTION_BLOCKING, "M", Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, "ML"));
    private static final Identifier GROUP = Identifier.withDefaultNamespace("heightmaps");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity == null || minecraft.level == null || clientChunk == null) {
            return;
        }
        BlockPos feetPos = entity.blockPosition();
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder heightmaps = new StringBuilder("CH");
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (!type.sendToClient()) continue;
            heightmaps.append(" ").append(HEIGHTMAP_NAMES.get(type)).append(": ").append(clientChunk.getHeight(type, feetPos.getX(), feetPos.getZ()));
        }
        result.add(heightmaps.toString());
        heightmaps.setLength(0);
        heightmaps.append("SH");
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (!type.keepAfterWorldgen()) continue;
            heightmaps.append(" ").append(HEIGHTMAP_NAMES.get(type)).append(": ");
            if (serverChunk != null) {
                heightmaps.append(serverChunk.getHeight(type, feetPos.getX(), feetPos.getZ()));
                continue;
            }
            heightmaps.append("??");
        }
        result.add(heightmaps.toString());
        displayer.addToGroup(GROUP, result);
    }
}

