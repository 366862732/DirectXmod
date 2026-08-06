/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EntityDimensions(float width, float height, float eyeHeight, EntityAttachments attachments, boolean fixed) {
    private EntityDimensions(float width, float height, boolean fixed) {
        this(width, height, EntityDimensions.defaultEyeHeight(height), EntityAttachments.createDefault(width, height), fixed);
    }

    private static float defaultEyeHeight(float height) {
        return height * 0.85f;
    }

    public AABB makeBoundingBox(Vec3 pos) {
        return this.makeBoundingBox(pos.x, pos.y, pos.z);
    }

    public AABB makeBoundingBox(double x, double y, double z) {
        float w = this.width / 2.0f;
        float h = this.height;
        return new AABB(x - (double)w, y, z - (double)w, x + (double)w, y + (double)h, z + (double)w);
    }

    public EntityDimensions scale(float scaleFactor) {
        return this.scale(scaleFactor, scaleFactor);
    }

    public EntityDimensions scale(float widthScaleFactor, float heightScaleFactor) {
        if (this.fixed || widthScaleFactor == 1.0f && heightScaleFactor == 1.0f) {
            return this;
        }
        return new EntityDimensions(this.width * widthScaleFactor, this.height * heightScaleFactor, this.eyeHeight * heightScaleFactor, this.attachments.scale(widthScaleFactor, heightScaleFactor, widthScaleFactor), false);
    }

    public static EntityDimensions scalable(float width, float height) {
        return new EntityDimensions(width, height, false);
    }

    public static EntityDimensions fixed(float width, float height) {
        return new EntityDimensions(width, height, true);
    }

    public EntityDimensions withEyeHeight(float eyeHeight) {
        return new EntityDimensions(this.width, this.height, eyeHeight, this.attachments, this.fixed);
    }

    public EntityDimensions withAttachments(EntityAttachments.Builder attachments) {
        return new EntityDimensions(this.width, this.height, this.eyeHeight, attachments.build(this.width, this.height), this.fixed);
    }
}

