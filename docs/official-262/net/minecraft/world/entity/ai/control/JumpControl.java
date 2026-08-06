/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.Control;

public class JumpControl
implements Control {
    private final Mob mob;
    protected boolean jump;

    public JumpControl(Mob mob) {
        this.mob = mob;
    }

    public void jump() {
        this.jump = true;
    }

    public void tick() {
        this.mob.setJumping(this.jump);
        this.jump = false;
    }
}

