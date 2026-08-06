/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;

@Environment(value=EnvType.CLIENT)
public class BubbleColumnAmbientSoundHandler
implements AmbientSoundHandler {
    private final LocalPlayer player;
    private boolean wasInBubbleColumn;
    private boolean firstTick = true;

    public BubbleColumnAmbientSoundHandler(LocalPlayer player) {
        this.player = player;
    }

    @Override
    public void tick() {
        Level level = this.player.level();
        BlockState state = level.getBlockStatesIfLoaded(this.player.getBoundingBox().inflate(0.0, -0.4f, 0.0).deflate(1.0E-6)).filter(s -> s.is(Blocks.BUBBLE_COLUMN)).findFirst().orElse(null);
        if (state != null) {
            if (!this.wasInBubbleColumn && !this.firstTick && state.is(Blocks.BUBBLE_COLUMN) && !this.player.isSpectator()) {
                boolean dragDown = state.getValue(BubbleColumnBlock.DRAG_DOWN);
                if (dragDown) {
                    this.player.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 1.0f);
                } else {
                    this.player.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.0f, 1.0f);
                }
            }
            this.wasInBubbleColumn = true;
        } else {
            this.wasInBubbleColumn = false;
        }
        this.firstTick = false;
    }
}

