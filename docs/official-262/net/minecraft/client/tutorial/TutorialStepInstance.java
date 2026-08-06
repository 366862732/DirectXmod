/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.tutorial;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

@Environment(value=EnvType.CLIENT)
public interface TutorialStepInstance {
    default public void clear() {
    }

    default public void tick() {
    }

    default public void onInput(ClientInput input) {
    }

    default public void onMouse(double xd, double yd) {
    }

    default public void onLookAt(ClientLevel level, HitResult hit) {
    }

    default public void onDestroyBlock(ClientLevel level, BlockPos pos, BlockState state, float percent) {
    }

    default public void onOpenInventory() {
    }

    default public void onGetItem(ItemStack itemStack) {
    }
}

