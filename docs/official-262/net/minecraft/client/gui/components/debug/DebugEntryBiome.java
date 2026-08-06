/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryBiome
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("biome");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity == null || minecraft.level == null) {
            return;
        }
        BlockPos feetPos = entity.blockPosition();
        if (minecraft.level.isInsideBuildHeight(feetPos.getY())) {
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && serverOrClientLevel instanceof ServerLevel) {
                displayer.addToGroup(GROUP, List.of("Biome: " + DebugEntryBiome.printBiome(minecraft.level.getBiome(feetPos)), "Server Biome: " + DebugEntryBiome.printBiome(serverOrClientLevel.getBiome(feetPos))));
            } else {
                displayer.addLine("Biome: " + DebugEntryBiome.printBiome(minecraft.level.getBiome(feetPos)));
            }
        }
    }

    private static String printBiome(Holder<Biome> biome) {
        return (String)biome.unwrap().map(key -> key.identifier().toString(), l -> "[unregistered " + String.valueOf(l) + "]");
    }
}

