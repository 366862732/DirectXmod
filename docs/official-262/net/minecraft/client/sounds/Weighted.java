/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public interface Weighted<T> {
    public int getWeight();

    public T getSound(RandomSource var1);

    public void preloadIfRequired(SoundEngine var1);
}

