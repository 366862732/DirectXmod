/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2fc
 *  org.joml.Vector2f
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record ScreenRectangle(ScreenPosition position, int width, int height) {
    private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

    public ScreenRectangle(int x, int y, int width, int height) {
        this(new ScreenPosition(x, y), width, height);
    }

    public static ScreenRectangle empty() {
        return EMPTY;
    }

    public static ScreenRectangle of(ScreenAxis primaryAxis, int primaryIndex, int secondaryIndex, int primaryLength, int secondaryLength) {
        return switch (primaryAxis) {
            default -> throw new MatchException(null, null);
            case ScreenAxis.HORIZONTAL -> new ScreenRectangle(primaryIndex, secondaryIndex, primaryLength, secondaryLength);
            case ScreenAxis.VERTICAL -> new ScreenRectangle(secondaryIndex, primaryIndex, secondaryLength, primaryLength);
        };
    }

    public ScreenRectangle step(ScreenDirection direction) {
        return new ScreenRectangle(this.position.step(direction), this.width, this.height);
    }

    public int getLength(ScreenAxis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case ScreenAxis.HORIZONTAL -> this.width;
            case ScreenAxis.VERTICAL -> this.height;
        };
    }

    public int getBoundInDirection(ScreenDirection direction) {
        ScreenAxis axis = direction.getAxis();
        if (direction.isPositive()) {
            return this.position.getCoordinate(axis) + this.getLength(axis) - 1;
        }
        return this.position.getCoordinate(axis);
    }

    public ScreenRectangle getBorder(ScreenDirection direction) {
        int startFirst = this.getBoundInDirection(direction);
        ScreenAxis orthogonalAxis = direction.getAxis().orthogonal();
        int startSecond = this.getBoundInDirection(orthogonalAxis.getNegative());
        int length = this.getLength(orthogonalAxis);
        return ScreenRectangle.of(direction.getAxis(), startFirst, startSecond, 1, length).step(direction);
    }

    public boolean overlaps(ScreenRectangle other) {
        return this.overlapsInAxis(other, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(other, ScreenAxis.VERTICAL);
    }

    public boolean overlapsInAxis(ScreenRectangle other, ScreenAxis axis) {
        int thisLower = this.getBoundInDirection(axis.getNegative());
        int otherLower = other.getBoundInDirection(axis.getNegative());
        int thisHigher = this.getBoundInDirection(axis.getPositive());
        int otherHigher = other.getBoundInDirection(axis.getPositive());
        return Math.max(thisLower, otherLower) <= Math.min(thisHigher, otherHigher);
    }

    public int getCenterInAxis(ScreenAxis axis) {
        return (this.getBoundInDirection(axis.getPositive()) + this.getBoundInDirection(axis.getNegative())) / 2;
    }

    public @Nullable ScreenRectangle intersection(ScreenRectangle other) {
        int left = Math.max(this.left(), other.left());
        int top = Math.max(this.top(), other.top());
        int right = Math.min(this.right(), other.right());
        int bottom = Math.min(this.bottom(), other.bottom());
        if (left >= right || top >= bottom) {
            return null;
        }
        return new ScreenRectangle(left, top, right - left, bottom - top);
    }

    public boolean intersects(ScreenRectangle other) {
        return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
    }

    public boolean encompasses(ScreenRectangle other) {
        return other.left() >= this.left() && other.top() >= this.top() && other.right() <= this.right() && other.bottom() <= this.bottom();
    }

    public int top() {
        return this.position.y();
    }

    public int bottom() {
        return this.position.y() + this.height;
    }

    public int left() {
        return this.position.x();
    }

    public int right() {
        return this.position.x() + this.width;
    }

    public boolean containsPoint(int x, int y) {
        return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
    }

    public ScreenRectangle transformAxisAligned(Matrix3x2fc matrix) {
        Vector2f topLeft = matrix.transformPosition((float)this.left(), (float)this.top(), new Vector2f());
        Vector2f bottomRight = matrix.transformPosition((float)this.right(), (float)this.bottom(), new Vector2f());
        return new ScreenRectangle(Mth.floor(topLeft.x), Mth.floor(topLeft.y), Mth.floor(bottomRight.x - topLeft.x), Mth.floor(bottomRight.y - topLeft.y));
    }

    public ScreenRectangle transformMaxBounds(Matrix3x2fc matrix) {
        Vector2f topLeft = matrix.transformPosition((float)this.left(), (float)this.top(), new Vector2f());
        Vector2f topRight = matrix.transformPosition((float)this.right(), (float)this.top(), new Vector2f());
        Vector2f bottomLeft = matrix.transformPosition((float)this.left(), (float)this.bottom(), new Vector2f());
        Vector2f bottomRight = matrix.transformPosition((float)this.right(), (float)this.bottom(), new Vector2f());
        float minX = Math.min(Math.min(topLeft.x(), bottomLeft.x()), Math.min(topRight.x(), bottomRight.x()));
        float maxX = Math.max(Math.max(topLeft.x(), bottomLeft.x()), Math.max(topRight.x(), bottomRight.x()));
        float minY = Math.min(Math.min(topLeft.y(), bottomLeft.y()), Math.min(topRight.y(), bottomRight.y()));
        float maxY = Math.max(Math.max(topLeft.y(), bottomLeft.y()), Math.max(topRight.y(), bottomRight.y()));
        return new ScreenRectangle(Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX - minX), Mth.ceil(maxY - minY));
    }
}

