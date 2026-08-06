/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.chunk;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public final class TranslucencyPointOfView {
    private int x;
    private int y;
    private int z;

    public static TranslucencyPointOfView of(Vec3 cameraPos, long sectionNode) {
        return new TranslucencyPointOfView().set(cameraPos, sectionNode);
    }

    public TranslucencyPointOfView set(Vec3 cameraPos, long sectionPos) {
        this.x = TranslucencyPointOfView.getCoordinate(cameraPos.x(), SectionPos.x(sectionPos));
        this.y = TranslucencyPointOfView.getCoordinate(cameraPos.y(), SectionPos.y(sectionPos));
        this.z = TranslucencyPointOfView.getCoordinate(cameraPos.z(), SectionPos.z(sectionPos));
        return this;
    }

    private static int getCoordinate(double cameraCoordinate, int section) {
        int relativeSection = SectionPos.blockToSectionCoord(cameraCoordinate) - section;
        return Mth.clamp(relativeSection, -1, 1);
    }

    public boolean isAxisAligned() {
        return this.x == 0 || this.y == 0 || this.z == 0;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof TranslucencyPointOfView) {
            TranslucencyPointOfView otherPerspective = (TranslucencyPointOfView)other;
            return this.x == otherPerspective.x && this.y == otherPerspective.y && this.z == otherPerspective.z;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}

