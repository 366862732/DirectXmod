/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.trading;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

public class MerchantOffer {
    public static final Codec<MerchantOffer> CODEC = RecordCodecBuilder.create(i -> i.group((App)ItemCost.CODEC.fieldOf("buy").forGetter(o -> o.baseCostA), (App)ItemCost.CODEC.lenientOptionalFieldOf("buyB").forGetter(o -> o.costB), (App)ItemStack.CODEC.fieldOf("sell").forGetter(o -> o.result), (App)Codec.INT.lenientOptionalFieldOf("uses", (Object)0).forGetter(o -> o.uses), (App)Codec.INT.lenientOptionalFieldOf("maxUses", (Object)4).forGetter(o -> o.maxUses), (App)Codec.BOOL.lenientOptionalFieldOf("rewardExp", (Object)true).forGetter(o -> o.rewardExp), (App)Codec.INT.lenientOptionalFieldOf("specialPrice", (Object)0).forGetter(o -> o.specialPriceDiff), (App)Codec.INT.lenientOptionalFieldOf("demand", (Object)0).forGetter(o -> o.demand), (App)Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", (Object)Float.valueOf(0.0f)).forGetter(o -> Float.valueOf(o.priceMultiplier)), (App)Codec.INT.lenientOptionalFieldOf("xp", (Object)1).forGetter(o -> o.xp)).apply((Applicative)i, MerchantOffer::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffer> STREAM_CODEC = StreamCodec.of(MerchantOffer::writeToStream, MerchantOffer::createFromStream);
    private final ItemCost baseCostA;
    private final Optional<ItemCost> costB;
    private final ItemStack result;
    private int uses;
    private final int maxUses;
    private final boolean rewardExp;
    private int specialPriceDiff;
    private int demand;
    private final float priceMultiplier;
    private final int xp;

    private MerchantOffer(ItemCost baseCostA, Optional<ItemCost> costB, ItemStack result, int uses, int maxUses, boolean rewardExp, int specialPriceDiff, int demand, float priceMultiplier, int xp) {
        this.baseCostA = baseCostA;
        this.costB = costB;
        this.result = result;
        this.uses = uses;
        this.maxUses = maxUses;
        this.rewardExp = rewardExp;
        this.specialPriceDiff = specialPriceDiff;
        this.demand = demand;
        this.priceMultiplier = priceMultiplier;
        this.xp = xp;
    }

    public MerchantOffer(ItemCost buy, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        this(buy, Optional.empty(), result, maxUses, xp, priceMultiplier);
    }

    public MerchantOffer(ItemCost baseCostA, Optional<ItemCost> costB, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        this(baseCostA, costB, result, 0, maxUses, xp, priceMultiplier);
    }

    public MerchantOffer(ItemCost baseCostA, Optional<ItemCost> costB, ItemStack result, int uses, int maxUses, int xp, float priceMultiplier) {
        this(baseCostA, costB, result, uses, maxUses, xp, priceMultiplier, 0);
    }

    public MerchantOffer(ItemCost baseCostA, Optional<ItemCost> costB, ItemStack result, int uses, int maxUses, int xp, float priceMultiplier, int demand) {
        this(baseCostA, costB, result, uses, maxUses, true, 0, demand, priceMultiplier, xp);
    }

    private MerchantOffer(MerchantOffer offer) {
        this(offer.baseCostA, offer.costB, offer.result.copy(), offer.uses, offer.maxUses, offer.rewardExp, offer.specialPriceDiff, offer.demand, offer.priceMultiplier, offer.xp);
    }

    public ItemStack getBaseCostA() {
        return this.baseCostA.itemStack();
    }

    public ItemStack getCostA() {
        return this.baseCostA.itemStack().copyWithCount(this.getModifiedCostCount(this.baseCostA));
    }

    private int getModifiedCostCount(ItemCost cost) {
        int basePrice = cost.count();
        int demandDiff = Math.max(0, Mth.floor((float)(basePrice * this.demand) * this.priceMultiplier));
        return Mth.clamp(basePrice + demandDiff + this.specialPriceDiff, 1, cost.itemStack().getMaxStackSize());
    }

    public ItemStack getCostB() {
        return this.costB.map(ItemCost::itemStack).orElse(ItemStack.EMPTY);
    }

    public ItemCost getItemCostA() {
        return this.baseCostA;
    }

    public Optional<ItemCost> getItemCostB() {
        return this.costB;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public void updateDemand() {
        this.demand = this.demand + this.uses - (this.maxUses - this.uses);
    }

    public ItemStack assemble() {
        return this.result.copy();
    }

    public int getUses() {
        return this.uses;
    }

    public void resetUses() {
        this.uses = 0;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    public void increaseUses() {
        ++this.uses;
    }

    public int getDemand() {
        return this.demand;
    }

    public void addToSpecialPriceDiff(int add) {
        this.specialPriceDiff += add;
    }

    public void resetSpecialPriceDiff() {
        this.specialPriceDiff = 0;
    }

    public int getSpecialPriceDiff() {
        return this.specialPriceDiff;
    }

    public void setSpecialPriceDiff(int value) {
        this.specialPriceDiff = value;
    }

    public float getPriceMultiplier() {
        return this.priceMultiplier;
    }

    public int getXp() {
        return this.xp;
    }

    public boolean isOutOfStock() {
        return this.uses >= this.maxUses;
    }

    public void setToOutOfStock() {
        this.uses = this.maxUses;
    }

    public boolean needsRestock() {
        return this.uses > 0;
    }

    public boolean shouldRewardExp() {
        return this.rewardExp;
    }

    public boolean satisfiedBy(ItemStack buyA, ItemStack buyB) {
        if (!this.baseCostA.test(buyA) || buyA.getCount() < this.getModifiedCostCount(this.baseCostA)) {
            return false;
        }
        if (this.costB.isPresent()) {
            return this.costB.get().test(buyB) && buyB.getCount() >= this.costB.get().count();
        }
        return buyB.isEmpty();
    }

    public boolean take(ItemStack buyA, ItemStack buyB) {
        if (!this.satisfiedBy(buyA, buyB)) {
            return false;
        }
        buyA.shrink(this.getCostA().getCount());
        if (!this.getCostB().isEmpty()) {
            buyB.shrink(this.getCostB().getCount());
        }
        return true;
    }

    public MerchantOffer copy() {
        return new MerchantOffer(this);
    }

    private static void writeToStream(RegistryFriendlyByteBuf output, MerchantOffer offer) {
        ItemCost.STREAM_CODEC.encode(output, offer.getItemCostA());
        ItemStack.STREAM_CODEC.encode(output, offer.getResult());
        ItemCost.OPTIONAL_STREAM_CODEC.encode(output, offer.getItemCostB());
        output.writeBoolean(offer.isOutOfStock());
        output.writeInt(offer.getUses());
        output.writeInt(offer.getMaxUses());
        output.writeInt(offer.getXp());
        output.writeInt(offer.getSpecialPriceDiff());
        output.writeFloat(offer.getPriceMultiplier());
        output.writeInt(offer.getDemand());
    }

    public static MerchantOffer createFromStream(RegistryFriendlyByteBuf input) {
        ItemCost buy = (ItemCost)ItemCost.STREAM_CODEC.decode(input);
        ItemStack sell = (ItemStack)ItemStack.STREAM_CODEC.decode(input);
        Optional buyB = (Optional)ItemCost.OPTIONAL_STREAM_CODEC.decode(input);
        boolean isExhausted = input.readBoolean();
        int uses = input.readInt();
        int maxUses = input.readInt();
        int xp = input.readInt();
        int specialPriceDiff = input.readInt();
        float priceMultiplier = input.readFloat();
        int demand = input.readInt();
        MerchantOffer offer = new MerchantOffer(buy, buyB, sell, uses, maxUses, xp, priceMultiplier, demand);
        if (isExhausted) {
            offer.setToOutOfStock();
        }
        offer.setSpecialPriceDiff(specialPriceDiff);
        return offer;
    }
}

