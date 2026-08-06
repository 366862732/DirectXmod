/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface LookAt {
    public void perform(CommandSourceStack var1, Entity var2);

    public record LookAtPosition(Vec3 position) implements LookAt
    {
        @Override
        public void perform(CommandSourceStack source, Entity target) {
            target.lookAt(source.getAnchor(), this.position);
        }
    }

    public record LookAtEntity(Entity entity, EntityAnchorArgument.Anchor anchor) implements LookAt
    {
        @Override
        public void perform(CommandSourceStack source, Entity target) {
            if (target instanceof ServerPlayer) {
                ServerPlayer targetPlayer = (ServerPlayer)target;
                targetPlayer.lookAt(source.getAnchor(), this.entity, this.anchor);
            } else {
                target.lookAt(source.getAnchor(), this.anchor.apply(this.entity));
            }
        }
    }
}

