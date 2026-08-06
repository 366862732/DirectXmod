/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.DyeColor;

@Environment(value=EnvType.CLIENT)
public class SheepRenderState
extends LivingEntityRenderState {
    public float headEatPositionScale;
    public float headEatAngleScale;
    public boolean isSheared;
    public DyeColor woolColor = DyeColor.WHITE;
    public boolean isJebSheep;

    public int getWoolColor() {
        if (this.isJebSheep) {
            return ColorLerper.getLerpedColor(ColorLerper.Type.SHEEP, this.ageInTicks);
        }
        return ColorLerper.Type.SHEEP.getColor(this.woolColor);
    }
}

