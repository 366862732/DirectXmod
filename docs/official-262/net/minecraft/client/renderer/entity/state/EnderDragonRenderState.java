/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class EnderDragonRenderState
extends EntityRenderState {
    public float flapTime;
    public float deathTime;
    public boolean hasRedOverlay;
    public @Nullable Vec3 beamOffset;
    public boolean isLandingOrTakingOff;
    public boolean isSitting;
    public double distanceToEgg;
    public float partialTicks;
    public final DragonFlightHistory flightHistory = new DragonFlightHistory();

    public DragonFlightHistory.Sample getHistoricalPos(int delay) {
        return this.flightHistory.get(delay, this.partialTicks);
    }

    public float getHeadPartYOffset(int part, DragonFlightHistory.Sample bodyPos, DragonFlightHistory.Sample partPos) {
        double result = this.isLandingOrTakingOff ? (double)part / Math.max(this.distanceToEgg / 4.0, 1.0) : (this.isSitting ? (double)part : (part == 6 ? 0.0 : partPos.y() - bodyPos.y()));
        return (float)result;
    }
}

