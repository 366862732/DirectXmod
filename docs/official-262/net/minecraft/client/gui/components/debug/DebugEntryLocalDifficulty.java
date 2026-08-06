/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryLocalDifficulty
implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity == null || serverChunk == null || !(serverOrClientLevel instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)serverOrClientLevel;
        BlockPos feetPos = entity.blockPosition();
        if (serverLevel.isInsideBuildHeight(feetPos.getY())) {
            float moonBrightness = serverLevel.getMoonBrightness(feetPos);
            long localTime = serverChunk.getInhabitedTime();
            DifficultyInstance localDifficulty = new DifficultyInstance(serverLevel.getDifficulty(), serverLevel.getOverworldClockTime(), localTime, moonBrightness);
            displayer.addLine(String.format(Locale.ROOT, "Local Difficulty: %.2f // %.2f", Float.valueOf(localDifficulty.getEffectiveDifficulty()), Float.valueOf(localDifficulty.getSpecialMultiplier())));
        }
    }
}

