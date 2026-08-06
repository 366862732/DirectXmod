/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class JukeboxTicksSinceSongStartedFix
extends NamedEntityFix {
    public JukeboxTicksSinceSongStartedFix(Schema outputSchema) {
        super(outputSchema, false, "JukeboxTicksSinceSongStartedFix", References.BLOCK_ENTITY, "minecraft:jukebox");
    }

    public Dynamic<?> fixTag(Dynamic<?> input) {
        long ticksSinceSongStarted = input.get("TickCount").asLong(0L) - input.get("RecordStartTick").asLong(0L);
        Dynamic result = input.remove("IsPlaying").remove("TickCount").remove("RecordStartTick");
        if (ticksSinceSongStarted > 0L) {
            return result.set("ticks_since_song_started", input.createLong(ticksSinceSongStarted));
        }
        return result;
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fixTag);
    }
}

