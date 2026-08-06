/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.openal.AL10
 */
package com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.ListenerTransform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;

@Environment(value=EnvType.CLIENT)
public class Listener {
    private ListenerTransform transform = ListenerTransform.INITIAL;

    public void setTransform(ListenerTransform transform) {
        this.transform = transform;
        Vec3 position = transform.position();
        Vec3 forward = transform.forward();
        Vec3 up = transform.up();
        AL10.alListener3f((int)4100, (float)((float)position.x), (float)((float)position.y), (float)((float)position.z));
        AL10.alListenerfv((int)4111, (float[])new float[]{(float)forward.x, (float)forward.y, (float)forward.z, (float)up.x(), (float)up.y(), (float)up.z()});
    }

    public void reset() {
        this.setTransform(ListenerTransform.INITIAL);
    }

    public ListenerTransform getTransform() {
        return this.transform;
    }
}

