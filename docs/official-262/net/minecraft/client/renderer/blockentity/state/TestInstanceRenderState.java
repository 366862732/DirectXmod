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
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;

@Environment(value=EnvType.CLIENT)
public class TestInstanceRenderState
extends BlockEntityRenderState {
    public BeaconRenderState beaconRenderState;
    public BlockEntityWithBoundingBoxRenderState blockEntityWithBoundingBoxRenderState;
    public final List<TestInstanceBlockEntity.ErrorMarker> errorMarkers = new ArrayList<TestInstanceBlockEntity.ErrorMarker>();
}

