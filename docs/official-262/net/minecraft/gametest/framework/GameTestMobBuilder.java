/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestEntityBuilder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class GameTestMobBuilder<E extends Mob>
extends GameTestEntityBuilder<E> {
    private boolean freeWill = true;

    public GameTestMobBuilder(GameTestHelper testHelper, EntityType<E> entityType, Vec3 position) {
        super(testHelper, entityType, position);
    }

    public GameTestMobBuilder<E> withNoFreeWill() {
        this.freeWill = false;
        return this;
    }

    @Override
    public E spawn() {
        Mob entity = (Mob)super.spawn();
        if (!this.freeWill) {
            entity.removeFreeWill();
        }
        return (E)entity;
    }
}

