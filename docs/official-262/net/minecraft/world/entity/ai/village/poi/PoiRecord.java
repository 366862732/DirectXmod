/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class PoiRecord {
    private final BlockPos pos;
    private final Holder<PoiType> poiType;
    private int freeTickets;
    private final Runnable setDirty;

    private PoiRecord(BlockPos pos, Holder<PoiType> poiType, int freeTickets, Runnable setDirty) {
        this.pos = pos.immutable();
        this.poiType = poiType;
        this.freeTickets = freeTickets;
        this.setDirty = setDirty;
    }

    public PoiRecord(BlockPos pos, Holder<PoiType> poiType, Runnable setDirty) {
        this(pos, poiType, poiType.value().maxTickets(), setDirty);
    }

    public Packed pack() {
        return new Packed(this.pos, this.poiType, this.freeTickets);
    }

    @Deprecated
    @VisibleForDebug
    public int getFreeTickets() {
        return this.freeTickets;
    }

    protected boolean acquireTicket() {
        if (this.freeTickets <= 0) {
            return false;
        }
        --this.freeTickets;
        this.setDirty.run();
        return true;
    }

    protected boolean releaseTicket() {
        if (this.freeTickets >= this.poiType.value().maxTickets()) {
            return false;
        }
        ++this.freeTickets;
        this.setDirty.run();
        return true;
    }

    public boolean hasSpace() {
        return this.freeTickets > 0;
    }

    public boolean isOccupied() {
        return this.freeTickets != this.poiType.value().maxTickets();
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Holder<PoiType> getPoiType() {
        return this.poiType;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.pos, ((PoiRecord)o).pos);
    }

    public int hashCode() {
        return this.pos.hashCode();
    }

    public record Packed(BlockPos pos, Holder<PoiType> poiType, int freeTickets) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockPos.CODEC.fieldOf("pos").forGetter(Packed::pos), (App)RegistryFixedCodec.create(Registries.POINT_OF_INTEREST_TYPE).fieldOf("type").forGetter(Packed::poiType), (App)Codec.INT.optionalFieldOf("free_tickets", (Object)0).forGetter(Packed::freeTickets)).apply((Applicative)i, Packed::new));

        public PoiRecord unpack(Runnable setDirty) {
            return new PoiRecord(this.pos, this.poiType, this.freeTickets, setDirty);
        }
    }
}

