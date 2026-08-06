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

public final class WeatherData
extends SavedData {
    public static final Codec<WeatherData> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("clear_weather_time").forGetter(WeatherData::getClearWeatherTime), (App)Codec.INT.fieldOf("rain_time").forGetter(WeatherData::getRainTime), (App)Codec.INT.fieldOf("thunder_time").forGetter(WeatherData::getThunderTime), (App)Codec.BOOL.fieldOf("raining").forGetter(WeatherData::isRaining), (App)Codec.BOOL.fieldOf("thundering").forGetter(WeatherData::isThundering)).apply((Applicative)i, WeatherData::new));
    public static final SavedDataType<WeatherData> TYPE = new SavedDataType<WeatherData>(Identifier.withDefaultNamespace("weather"), WeatherData::new, CODEC, DataFixTypes.SAVED_DATA_WEATHER);
    private int clearWeatherTime;
    private int rainTime;
    private int thunderTime;
    private boolean raining;
    private boolean thundering;

    public WeatherData() {
    }

    public WeatherData(int clearWeatherTime, int rainTime, int thunderTime, boolean raining, boolean thundering) {
        this.clearWeatherTime = clearWeatherTime;
        this.rainTime = rainTime;
        this.thunderTime = thunderTime;
        this.raining = raining;
        this.thundering = thundering;
    }

    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    public void setClearWeatherTime(int clearWeatherTime) {
        this.clearWeatherTime = clearWeatherTime;
        this.setDirty();
    }

    public boolean isThundering() {
        return this.thundering;
    }

    public void setThundering(boolean thundering) {
        this.thundering = thundering;
        this.setDirty();
    }

    public int getThunderTime() {
        return this.thunderTime;
    }

    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
        this.setDirty();
    }

    public boolean isRaining() {
        return this.raining;
    }

    public void setRaining(boolean raining) {
        this.raining = raining;
        this.setDirty();
    }

    public int getRainTime() {
        return this.rainTime;
    }

    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
        this.setDirty();
    }
}

