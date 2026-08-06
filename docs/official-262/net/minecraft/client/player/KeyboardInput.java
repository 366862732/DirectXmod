/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

@Environment(value=EnvType.CLIENT)
public class KeyboardInput
extends ClientInput {
    private final Options options;

    public KeyboardInput(Options options) {
        this.options = options;
    }

    private static float calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }

    @Override
    public void tick() {
        this.keyPresses = new Input(this.options.keyUp.isDown(), this.options.keyDown.isDown(), this.options.keyLeft.isDown(), this.options.keyRight.isDown(), this.options.keyJump.isDown(), this.options.keyShift.isDown(), this.options.keySprint.isDown());
        float forwardImpulse = KeyboardInput.calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        float leftImpulse = KeyboardInput.calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
        this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();
    }
}

