/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity.npc.villager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractVillager
extends AgeableMob
implements Npc,
Merchant,
InventoryCarrier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Integer> DATA_UNHAPPY_COUNTER = SynchedEntityData.defineId(AbstractVillager.class, EntityDataSerializers.INT);
    private @Nullable Player tradingPlayer;
    protected @Nullable MerchantOffers offers;
    private final SimpleContainer inventory = new SimpleContainer(8);

    public AbstractVillager(EntityType<? extends AbstractVillager> type, Level level) {
        super((EntityType<? extends AgeableMob>)type, level);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0f);
        this.setPathfindingMalus(PathType.FIRE, -1.0f);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public int getUnhappyCounter() {
        return this.entityData.get(DATA_UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int value) {
        this.entityData.set(DATA_UNHAPPY_COUNTER, value);
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_UNHAPPY_COUNTER, 0);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public MerchantOffers getOffers() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            throw new IllegalStateException("Cannot load Villager offers on the client");
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades(serverLevel);
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(@Nullable MerchantOffers offers) {
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.rewardTradeXp(offer);
        if (this.tradingPlayer instanceof ServerPlayer) {
            CriteriaTriggers.TRADE.trigger((ServerPlayer)this.tradingPlayer, this, offer.getResult());
        }
    }

    protected abstract void rewardTradeXp(MerchantOffer var1);

    @Override
    public boolean showProgressBar() {
        return true;
    }

    @Override
    public void notifyTradeUpdated(ItemStack itemStack) {
        if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.makeSound(this.getTradeUpdatedSound(!itemStack.isEmpty()));
        }
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return validTrade ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
    }

    public void playCelebrateSound() {
        this.makeSound(SoundEvents.VILLAGER_CELEBRATE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.level().isClientSide() && this.offers != null) {
            output.store("Offers", MerchantOffers.CODEC, this.offers);
        }
        this.writeInventoryToTag(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.offers = input.read("Offers", MerchantOffers.CODEC).orElse(null);
        this.readInventoryFromTag(input);
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        this.stopTrading();
        return super.teleport(transition);
    }

    protected void stopTrading() {
        this.setTradingPlayer(null);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.stopTrading();
    }

    protected void addParticlesAroundSelf(ParticleOptions particle) {
        for (int i = 0; i < 5; ++i) {
            double xa = this.random.nextGaussian() * 0.02;
            double ya = this.random.nextGaussian() * 0.02;
            double za = this.random.nextGaussian() * 0.02;
            this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 1.0, this.getRandomZ(1.0), xa, ya, za);
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        int inventorySlot = slot - 300;
        if (inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize()) {
            return this.inventory.getSlot(inventorySlot);
        }
        return super.getSlot(slot);
    }

    protected abstract void updateTrades(ServerLevel var1);

    protected void addOffersFromTradeSet(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> resourceKey) {
        Optional<TradeSet> tradeSetOpt = this.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(resourceKey);
        if (tradeSetOpt.isEmpty()) {
            LOGGER.debug("Missing expected trade set {}", resourceKey);
            return;
        }
        TradeSet tradeSet = tradeSetOpt.get();
        LootContext lootContext = new LootContext.Builder(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE).create(LootContextParamSets.VILLAGER_TRADE)).create(tradeSet.randomSequence());
        int numberOfOffers = tradeSet.calculateNumberOfTrades(lootContext);
        if (tradeSet.allowDuplicates()) {
            AbstractVillager.addOffersFromItemListings(lootContext, offers, tradeSet.getTrades(), numberOfOffers);
        } else {
            AbstractVillager.addOffersFromItemListingsWithoutDuplicates(lootContext, offers, tradeSet.getTrades(), numberOfOffers);
        }
    }

    @VisibleForTesting
    static void addOffersFromItemListings(LootContext lootContext, MerchantOffers merchantOffers, HolderSet<VillagerTrade> potentialOffers, int numberOfOffers) {
        ArrayList potentialOffersList = Lists.newArrayList(potentialOffers);
        int offersFound = 0;
        while (offersFound < numberOfOffers && !potentialOffersList.isEmpty()) {
            int roll = lootContext.getRandom().nextInt(potentialOffersList.size());
            Holder villagerTrade = (Holder)potentialOffersList.get(roll);
            MerchantOffer offer = ((VillagerTrade)villagerTrade.value()).getOffer(lootContext);
            if (offer == null) {
                potentialOffersList.remove(roll);
                continue;
            }
            merchantOffers.add(offer);
            ++offersFound;
        }
    }

    private static void addOffersFromItemListingsWithoutDuplicates(LootContext lootContext, MerchantOffers merchantOffers, HolderSet<VillagerTrade> potentialOffers, int numberOfOffers) {
        ArrayList leftoverOffers = Lists.newArrayList(potentialOffers);
        int offersFound = 0;
        while (offersFound < numberOfOffers && !leftoverOffers.isEmpty()) {
            Holder villagerTrade = (Holder)leftoverOffers.remove(lootContext.getRandom().nextInt(leftoverOffers.size()));
            MerchantOffer offer = ((VillagerTrade)villagerTrade.value()).getOffer(lootContext);
            if (offer == null) continue;
            merchantOffers.add(offer);
            ++offersFound;
        }
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTickTime) {
        float yRot = Mth.lerp(partialTickTime, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180);
        Vec3 offset = new Vec3(0.0, this.getBoundingBox().getYsize() - 1.0, 0.2);
        return this.getPosition(partialTickTime).add(offset.yRot(-yRot));
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.getTradingPlayer() == player && this.isAlive() && player.isWithinEntityInteractionRange(this, 4.0);
    }
}

