/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

public class MerchantOffers
extends ArrayList<MerchantOffer> {
    public static final Codec<MerchantOffers> CODEC = MerchantOffer.CODEC.listOf().optionalFieldOf("Recipes", List.of()).xmap(MerchantOffers::new, Function.identity()).codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffers> STREAM_CODEC = MerchantOffer.STREAM_CODEC.apply(ByteBufCodecs.collection(MerchantOffers::new));

    public MerchantOffers() {
    }

    private MerchantOffers(int initialCapacity) {
        super(initialCapacity);
    }

    private MerchantOffers(Collection<MerchantOffer> offers) {
        super(offers);
    }

    public @Nullable MerchantOffer getRecipeFor(ItemStack buyA, ItemStack buyB, int selectionHint) {
        if (selectionHint > 0 && selectionHint < this.size()) {
            MerchantOffer offer = (MerchantOffer)this.get(selectionHint);
            if (offer.satisfiedBy(buyA, buyB)) {
                return offer;
            }
            return null;
        }
        for (int i = 0; i < this.size(); ++i) {
            MerchantOffer offer = (MerchantOffer)this.get(i);
            if (!offer.satisfiedBy(buyA, buyB)) continue;
            return offer;
        }
        return null;
    }

    public MerchantOffers copy() {
        MerchantOffers offers = new MerchantOffers(this.size());
        for (MerchantOffer offer : this) {
            offers.add(offer.copy());
        }
        return offers;
    }
}

