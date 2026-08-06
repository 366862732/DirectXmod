/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.saveddata.maps;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapId;

public class MapIndex
extends SavedData {
    private static final int NO_MAP_ID = -1;
    public static final Codec<MapIndex> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.optionalFieldOf("map", (Object)-1).forGetter(m -> m.lastMapId)).apply((Applicative)i, MapIndex::new));
    public static final SavedDataType<MapIndex> TYPE = new SavedDataType<MapIndex>(Identifier.withDefaultNamespace("maps/last_id"), MapIndex::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX);
    private int lastMapId;

    public MapIndex() {
        this(-1);
    }

    public MapIndex(int lastMapId) {
        this.lastMapId = lastMapId;
    }

    public MapId getNextMapId() {
        MapId id = new MapId(++this.lastMapId);
        this.setDirty();
        return id;
    }
}

