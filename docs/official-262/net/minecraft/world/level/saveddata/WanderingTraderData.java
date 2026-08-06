/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.saveddata;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class WanderingTraderData
extends SavedData {
    public static final Codec<WanderingTraderData> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.optionalFieldOf("spawn_delay", (Object)24000).forGetter(data -> data.spawnDelay), (App)Codec.INT.optionalFieldOf("spawn_chance", (Object)25).forGetter(data -> data.spawnChance)).apply((Applicative)i, WanderingTraderData::new));
    public static final SavedDataType<WanderingTraderData> TYPE = new SavedDataType<WanderingTraderData>(Identifier.withDefaultNamespace("wandering_trader"), WanderingTraderData::new, CODEC, DataFixTypes.SAVED_DATA_WANDERING_TRADER);
    private int spawnDelay;
    private int spawnChance;

    public WanderingTraderData() {
        this(24000, 25);
    }

    public WanderingTraderData(int spawnDelay, int spawnChance) {
        this.spawnDelay = spawnDelay;
        this.spawnChance = spawnChance;
    }

    public int spawnDelay() {
        return this.spawnDelay;
    }

    public void setSpawnDelay(int spawnDelay) {
        if (this.spawnDelay != spawnDelay) {
            this.spawnDelay = spawnDelay;
            this.setDirty(true);
        }
    }

    public int spawnChance() {
        return this.spawnChance;
    }

    public void setSpawnChance(int spawnChance) {
        if (this.spawnChance != spawnChance) {
            this.spawnChance = spawnChance;
            this.setDirty(true);
        }
    }
}

