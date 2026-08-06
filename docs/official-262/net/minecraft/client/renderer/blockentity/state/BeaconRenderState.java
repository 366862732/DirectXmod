/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity.state;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Environment(value=EnvType.CLIENT)
public class BeaconRenderState
extends BlockEntityRenderState {
    public float animationTime;
    public float beamRadiusScale;
    public List<Section> sections = new ArrayList<Section>();

    @Environment(value=EnvType.CLIENT)
    public record Section(int color, int height) {
    }
}

