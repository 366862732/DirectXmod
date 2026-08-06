/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4f
 */
package net.minecraft.client.renderer.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4f;

@Environment(value=EnvType.CLIENT)
public class FogData {
    public float environmentalStart;
    public float renderDistanceStart;
    public float environmentalEnd;
    public float renderDistanceEnd;
    public float skyEnd;
    public float cloudEnd;
    public Vector4f color = new Vector4f();
}

