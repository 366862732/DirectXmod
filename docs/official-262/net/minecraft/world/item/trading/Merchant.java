/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.trading;

import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

public interface Merchant {
    public void setTradingPlayer(@Nullable Player var1);

    public @Nullable Player getTradingPlayer();

    public MerchantOffers getOffers();

    public void overrideOffers(MerchantOffers var1);

    public void notifyTrade(MerchantOffer var1);

    public void notifyTradeUpdated(ItemStack var1);

    public int getVillagerXp();

    public void overrideXp(int var1);

    public boolean showProgressBar();

    public SoundEvent getNotifyTradeSound();

    default public boolean canRestock() {
        return false;
    }

    default public void openTradingScreen(Player player, Component title, int level) {
        MerchantOffers offers;
        OptionalInt containerId = player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MerchantMenu(id, inventory, this), title));
        if (containerId.isPresent() && !(offers = this.getOffers()).isEmpty()) {
            player.sendMerchantOffers(containerId.getAsInt(), offers, level, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
        }
    }

    public boolean isClientSide();

    public boolean stillValid(Player var1);
}

