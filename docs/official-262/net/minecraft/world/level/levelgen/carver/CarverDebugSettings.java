/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.carver;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CarverDebugSettings {
    public static final CarverDebugSettings DEFAULT = new CarverDebugSettings(false, Blocks.ACACIA_BUTTON.defaultBlockState(), Blocks.CANDLE.defaultBlockState(), Blocks.STAINED_GLASS.orange().defaultBlockState(), Blocks.GLASS.defaultBlockState());
    public static final Codec<CarverDebugSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("debug_mode", (Object)false).forGetter(CarverDebugSettings::isDebugMode), (App)BlockState.CODEC.optionalFieldOf("air_state", (Object)DEFAULT.getAirState()).forGetter(CarverDebugSettings::getAirState), (App)BlockState.CODEC.optionalFieldOf("water_state", (Object)DEFAULT.getAirState()).forGetter(CarverDebugSettings::getWaterState), (App)BlockState.CODEC.optionalFieldOf("lava_state", (Object)DEFAULT.getAirState()).forGetter(CarverDebugSettings::getLavaState), (App)BlockState.CODEC.optionalFieldOf("barrier_state", (Object)DEFAULT.getAirState()).forGetter(CarverDebugSettings::getBarrierState)).apply((Applicative)i, CarverDebugSettings::new));
    private final boolean debugMode;
    private final BlockState airState;
    private final BlockState waterState;
    private final BlockState lavaState;
    private final BlockState barrierState;

    public static CarverDebugSettings of(boolean enabled, BlockState airState, BlockState waterState, BlockState lavaState, BlockState barrierState) {
        return new CarverDebugSettings(enabled, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(BlockState airState, BlockState waterState, BlockState lavaState, BlockState barrierState) {
        return new CarverDebugSettings(false, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(boolean debugMode, BlockState airState) {
        return new CarverDebugSettings(debugMode, airState, DEFAULT.getWaterState(), DEFAULT.getLavaState(), DEFAULT.getBarrierState());
    }

    private CarverDebugSettings(boolean debugMode, BlockState airState, BlockState waterState, BlockState lavaState, BlockState barrierState) {
        this.debugMode = debugMode;
        this.airState = airState;
        this.waterState = waterState;
        this.lavaState = lavaState;
        this.barrierState = barrierState;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public BlockState getAirState() {
        return this.airState;
    }

    public BlockState getWaterState() {
        return this.waterState;
    }

    public BlockState getLavaState() {
        return this.lavaState;
    }

    public BlockState getBarrierState() {
        return this.barrierState;
    }
}

