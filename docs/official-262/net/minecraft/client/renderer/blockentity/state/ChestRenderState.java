/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

@Environment(value=EnvType.CLIENT)
public class ChestRenderState
extends BlockEntityRenderState {
    public ChestType type = ChestType.SINGLE;
    public float open;
    public Direction facing = Direction.SOUTH;
    public ChestMaterialType material = ChestMaterialType.REGULAR;

    @Environment(value=EnvType.CLIENT)
    public static enum ChestMaterialType {
        ENDER_CHEST,
        CHRISTMAS,
        TRAPPED,
        COPPER_UNAFFECTED,
        COPPER_EXPOSED,
        COPPER_WEATHERED,
        COPPER_OXIDIZED,
        REGULAR;

    }
}

