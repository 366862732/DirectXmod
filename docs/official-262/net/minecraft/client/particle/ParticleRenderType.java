/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ParticleRenderType(String name, String shorthand) {
    public static final ParticleRenderType SINGLE_QUADS = new ParticleRenderType("SINGLE_QUADS", "SQ");
    public static final ParticleRenderType ITEM_PICKUP = new ParticleRenderType("ITEM_PICKUP", "IP");
    public static final ParticleRenderType ELDER_GUARDIANS = new ParticleRenderType("ELDER_GUARDIANS", "EG");
    public static final ParticleRenderType NO_RENDER = new ParticleRenderType("NO_RENDER", "NR");
}

