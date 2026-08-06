/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.zombie;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractZombieModel<S extends ZombieRenderState>
extends HumanoidModel<S> {
    protected AbstractZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(S state) {
        super.setupAnim(state);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, ((ZombieRenderState)state).isAggressive, state);
    }
}

