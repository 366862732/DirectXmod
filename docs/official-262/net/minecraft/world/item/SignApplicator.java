/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public interface SignApplicator {
    public boolean tryApplyToSign(Level var1, SignBlockEntity var2, boolean var3, ItemStack var4, Player var5);

    default public boolean canApplyToSign(SignText text, ItemStack item, Player player) {
        return text.hasMessage(player);
    }
}

