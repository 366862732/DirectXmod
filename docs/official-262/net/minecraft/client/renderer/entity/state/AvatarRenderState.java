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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class AvatarRenderState
extends HumanoidRenderState {
    public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
    public float capeFlap;
    public float capeLean;
    public float capeLean2;
    public int arrowCount;
    public int stingerCount;
    public boolean isSpectator;
    public boolean showHat = true;
    public boolean showJacket = true;
    public boolean showLeftPants = true;
    public boolean showRightPants = true;
    public boolean showLeftSleeve = true;
    public boolean showRightSleeve = true;
    public boolean showCape = true;
    public float fallFlyingTimeInTicks;
    public boolean shouldApplyFlyingYRot;
    public float flyingYRot;
    public  @Nullable Parrot.Variant parrotOnLeftShoulder;
    public  @Nullable Parrot.Variant parrotOnRightShoulder;
    public int id;
    public boolean showExtraEars = false;
    public final ItemStackRenderState heldOnHead = new ItemStackRenderState();

    public float fallFlyingScale() {
        return Mth.clamp(this.fallFlyingTimeInTicks * this.fallFlyingTimeInTicks / 100.0f, 0.0f, 1.0f);
    }
}

