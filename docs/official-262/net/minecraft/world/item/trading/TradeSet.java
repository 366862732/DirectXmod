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
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class TradeSet {
    public static final Codec<TradeSet> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.VILLAGER_TRADE).fieldOf("trades").forGetter(tradeSet -> tradeSet.trades), (App)NumberProviders.CODEC.fieldOf("amount").forGetter(tradeSet -> tradeSet.amount), (App)Codec.BOOL.optionalFieldOf("allow_duplicates", (Object)false).forGetter(tradeSet -> tradeSet.allowDuplicates), (App)Identifier.CODEC.optionalFieldOf("random_sequence").forGetter(t -> t.randomSequence)).apply((Applicative)i, TradeSet::new));
    private final HolderSet<VillagerTrade> trades;
    private final NumberProvider amount;
    private final boolean allowDuplicates;
    private final Optional<Identifier> randomSequence;

    public TradeSet(HolderSet<VillagerTrade> trades, NumberProvider amount, boolean allowDuplicates, Optional<Identifier> randomSequence) {
        this.trades = trades;
        this.amount = amount;
        this.allowDuplicates = allowDuplicates;
        this.randomSequence = randomSequence;
    }

    public HolderSet<VillagerTrade> getTrades() {
        return this.trades;
    }

    public int calculateNumberOfTrades(LootContext lootContext) {
        return this.amount.getInt(lootContext);
    }

    public boolean allowDuplicates() {
        return this.allowDuplicates;
    }

    public Optional<Identifier> randomSequence() {
        return this.randomSequence;
    }
}

