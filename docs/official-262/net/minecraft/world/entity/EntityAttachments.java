/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityAttachments {
    private final Map<EntityAttachment, List<Vec3>> attachments;

    private EntityAttachments(Map<EntityAttachment, List<Vec3>> attachments) {
        this.attachments = attachments;
    }

    public static EntityAttachments createDefault(float width, float height) {
        return EntityAttachments.builder().build(width, height);
    }

    public static Builder builder() {
        return new Builder();
    }

    public EntityAttachments scale(float x, float y, float z) {
        return new EntityAttachments(Util.makeEnumMap(EntityAttachment.class, attachment -> {
            ArrayList<Vec3> list = new ArrayList<Vec3>();
            for (Vec3 vec3 : this.attachments.get(attachment)) {
                list.add(vec3.multiply(x, y, z));
            }
            return list;
        }));
    }

    public @Nullable Vec3 getNullable(EntityAttachment attachment, int index, float rotY) {
        List<Vec3> points = this.attachments.get((Object)attachment);
        if (index < 0 || index >= points.size()) {
            return null;
        }
        return EntityAttachments.transformPoint(points.get(index), rotY);
    }

    public Vec3 get(EntityAttachment attachment, int index, float rotY) {
        Vec3 point = this.getNullable(attachment, index, rotY);
        if (point == null) {
            throw new IllegalStateException("Had no attachment point of type: " + String.valueOf((Object)attachment) + " for index: " + index);
        }
        return point;
    }

    public Vec3 getAverage(EntityAttachment attachment) {
        List<Vec3> points = this.attachments.get((Object)attachment);
        if (points == null || points.isEmpty()) {
            throw new IllegalStateException("No attachment points of type: PASSENGER");
        }
        Vec3 sum = Vec3.ZERO;
        for (Vec3 point : points) {
            sum = sum.add(point);
        }
        return sum.scale(1.0f / (float)points.size());
    }

    public Vec3 getClamped(EntityAttachment attachment, int index, float rotY) {
        List<Vec3> points = this.attachments.get((Object)attachment);
        if (points.isEmpty()) {
            throw new IllegalStateException("Had no attachment points of type: " + String.valueOf((Object)attachment));
        }
        Vec3 point = points.get(Mth.clamp(index, 0, points.size() - 1));
        return EntityAttachments.transformPoint(point, rotY);
    }

    private static Vec3 transformPoint(Vec3 point, float rotY) {
        return point.yRot(-rotY * ((float)Math.PI / 180));
    }

    public static class Builder {
        private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<EntityAttachment, List<Vec3>>(EntityAttachment.class);

        private Builder() {
        }

        public Builder attach(EntityAttachment attachment, float x, float y, float z) {
            return this.attach(attachment, new Vec3(x, y, z));
        }

        public Builder attach(EntityAttachment attachment, Vec3 point) {
            this.attachments.computeIfAbsent(attachment, a -> new ArrayList(1)).add(point);
            return this;
        }

        public EntityAttachments build(float width, float height) {
            Map<EntityAttachment, List<Vec3>> attachments = Util.makeEnumMap(EntityAttachment.class, attachment -> {
                List<Vec3> points = this.attachments.get(attachment);
                return points == null ? attachment.createFallbackPoints(width, height) : List.copyOf(points);
            });
            return new EntityAttachments(attachments);
        }
    }
}

