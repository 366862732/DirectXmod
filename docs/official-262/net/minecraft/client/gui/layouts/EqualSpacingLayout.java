/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class EqualSpacingLayout
extends AbstractLayout {
    private final Orientation orientation;
    private final List<ChildContainer> children = new ArrayList<ChildContainer>();
    private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

    public EqualSpacingLayout(int width, int height, Orientation orientation) {
        this(0, 0, width, height, orientation);
    }

    public EqualSpacingLayout(int x, int y, int width, int height, Orientation orientation) {
        super(x, y, width, height);
        this.orientation = orientation;
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();
        if (this.children.isEmpty()) {
            return;
        }
        int totalChildPrimaryLength = 0;
        int maxChildSecondaryLength = this.orientation.getSecondaryLength(this);
        for (ChildContainer child : this.children) {
            totalChildPrimaryLength += this.orientation.getPrimaryLength(child);
            maxChildSecondaryLength = Math.max(maxChildSecondaryLength, this.orientation.getSecondaryLength(child));
        }
        int remainingSpace = this.orientation.getPrimaryLength(this) - totalChildPrimaryLength;
        int position = this.orientation.getPrimaryPosition(this);
        Iterator<ChildContainer> childIterator = this.children.iterator();
        ChildContainer firstChild = childIterator.next();
        this.orientation.setPrimaryPosition(firstChild, position);
        position += this.orientation.getPrimaryLength(firstChild);
        if (this.children.size() >= 2) {
            Divisor divisor = new Divisor(remainingSpace, this.children.size() - 1);
            while (divisor.hasNext()) {
                ChildContainer child = childIterator.next();
                this.orientation.setPrimaryPosition(child, position += divisor.nextInt());
                position += this.orientation.getPrimaryLength(child);
            }
        }
        int thisSecondaryPosition = this.orientation.getSecondaryPosition(this);
        for (ChildContainer child : this.children) {
            this.orientation.setSecondaryPosition(child, thisSecondaryPosition, maxChildSecondaryLength);
        }
        switch (this.orientation.ordinal()) {
            case 0: {
                this.height = maxChildSecondaryLength;
                break;
            }
            case 1: {
                this.width = maxChildSecondaryLength;
            }
        }
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        this.children.forEach(wrapper -> layoutElementVisitor.accept(wrapper.child));
    }

    @Override
    public void removeChildren() {
        this.children.clear();
    }

    public LayoutSettings newChildLayoutSettings() {
        return this.defaultChildLayoutSettings.copy();
    }

    public LayoutSettings defaultChildLayoutSetting() {
        return this.defaultChildLayoutSettings;
    }

    public <T extends LayoutElement> T addChild(T child) {
        return this.addChild(child, this.newChildLayoutSettings());
    }

    public <T extends LayoutElement> T addChild(T child, LayoutSettings layoutSettings) {
        this.children.add(new ChildContainer(child, layoutSettings));
        return child;
    }

    public <T extends LayoutElement> T addChild(T child, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.addChild(child, Util.make(this.newChildLayoutSettings(), layoutSettingsAdjustments));
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Orientation {
        HORIZONTAL,
        VERTICAL;


        private int getPrimaryLength(LayoutElement widget) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> widget.getWidth();
                case 1 -> widget.getHeight();
            };
        }

        private int getPrimaryLength(ChildContainer childContainer) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> childContainer.getWidth();
                case 1 -> childContainer.getHeight();
            };
        }

        private int getSecondaryLength(LayoutElement widget) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> widget.getHeight();
                case 1 -> widget.getWidth();
            };
        }

        private int getSecondaryLength(ChildContainer childContainer) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> childContainer.getHeight();
                case 1 -> childContainer.getWidth();
            };
        }

        private void setPrimaryPosition(ChildContainer childContainer, int position) {
            switch (this.ordinal()) {
                case 0: {
                    childContainer.setX(position, childContainer.getWidth());
                    break;
                }
                case 1: {
                    childContainer.setY(position, childContainer.getHeight());
                }
            }
        }

        private void setSecondaryPosition(ChildContainer childContainer, int position, int availableSpace) {
            switch (this.ordinal()) {
                case 0: {
                    childContainer.setY(position, availableSpace);
                    break;
                }
                case 1: {
                    childContainer.setX(position, availableSpace);
                }
            }
        }

        private int getPrimaryPosition(LayoutElement widget) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> widget.getX();
                case 1 -> widget.getY();
            };
        }

        private int getSecondaryPosition(LayoutElement widget) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> widget.getY();
                case 1 -> widget.getX();
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ChildContainer
    extends AbstractLayout.AbstractChildWrapper {
        protected ChildContainer(LayoutElement child, LayoutSettings layoutSettings) {
            super(child, layoutSettings);
        }
    }
}

