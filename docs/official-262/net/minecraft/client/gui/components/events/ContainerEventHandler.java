/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector2i
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface ContainerEventHandler
extends GuiEventListener {
    public List<? extends GuiEventListener> children();

    default public Optional<GuiEventListener> getChildAt(double x, double y) {
        for (GuiEventListener guiEventListener : this.children()) {
            if (!guiEventListener.isMouseOver(x, y)) continue;
            return Optional.of(guiEventListener);
        }
        return Optional.empty();
    }

    @Override
    default public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Optional<GuiEventListener> child = this.getChildAt(event.x(), event.y());
        if (child.isEmpty()) {
            return false;
        }
        GuiEventListener widget = child.get();
        if (widget.mouseClicked(event, doubleClick) && widget.shouldTakeFocusAfterInteraction()) {
            this.setFocused(widget);
            if (event.button() == 0) {
                this.setDragging(true);
            }
        }
        return true;
    }

    @Override
    default public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            if (this.getFocused() != null) {
                return this.getFocused().mouseReleased(event);
            }
        }
        return false;
    }

    @Override
    default public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.getFocused() != null && this.isDragging() && event.button() == 0) {
            return this.getFocused().mouseDragged(event, dx, dy);
        }
        return false;
    }

    public boolean isDragging();

    public void setDragging(boolean var1);

    @Override
    default public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return this.getChildAt(x, y).filter(child -> child.mouseScrolled(x, y, scrollX, scrollY)).isPresent();
    }

    @Override
    default public boolean keyPressed(KeyEvent event) {
        return this.getFocused() != null && this.getFocused().keyPressed(event);
    }

    @Override
    default public boolean keyReleased(KeyEvent event) {
        return this.getFocused() != null && this.getFocused().keyReleased(event);
    }

    @Override
    default public boolean charTyped(CharacterEvent event) {
        return this.getFocused() != null && this.getFocused().charTyped(event);
    }

    @Override
    default public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return this.getFocused() != null && this.getFocused().preeditUpdated(event);
    }

    @Override
    default public ScreenRectangle getBorderForArrowNavigation(ScreenDirection opposite) {
        GuiEventListener focused = this.getFocused();
        return focused != null ? focused.getBorderForArrowNavigation(opposite) : GuiEventListener.super.getBorderForArrowNavigation(opposite);
    }

    public @Nullable GuiEventListener getFocused();

    public void setFocused(@Nullable GuiEventListener var1);

    @Override
    default public void setFocused(boolean focused) {
        if (!focused) {
            this.setFocused(null);
        }
    }

    @Override
    default public boolean isFocused() {
        return this.getFocused() != null;
    }

    @Override
    default public @Nullable ComponentPath getCurrentFocusPath() {
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return ComponentPath.path(this, focused.getCurrentFocusPath());
        }
        return null;
    }

    @Override
    default public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        ComponentPath focusPath;
        GuiEventListener focus = this.getFocused();
        if (focus != null && (focusPath = focus.nextFocusPath(navigationEvent)) != null) {
            return ComponentPath.path(this, focusPath);
        }
        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
            FocusNavigationEvent.TabNavigation tabNavigation = (FocusNavigationEvent.TabNavigation)navigationEvent;
            return this.handleTabNavigation(tabNavigation);
        }
        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
            FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)navigationEvent;
            return this.handleArrowNavigation(arrowNavigation);
        }
        return null;
    }

    private @Nullable ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation tabNavigation) {
        Supplier<GuiEventListener> getter;
        BooleanSupplier test;
        boolean forward = tabNavigation.forward();
        GuiEventListener focus = this.getFocused();
        ArrayList<? extends GuiEventListener> sortedChildren = new ArrayList<GuiEventListener>(this.children());
        Collections.sort(sortedChildren, Comparator.comparingInt(child -> child.getTabOrderGroup()));
        int index = sortedChildren.indexOf(focus);
        int newIndex = focus != null && index >= 0 ? index + (forward ? 1 : 0) : (forward ? 0 : sortedChildren.size());
        ListIterator iterator = sortedChildren.listIterator(newIndex);
        BooleanSupplier booleanSupplier = forward ? iterator::hasNext : (test = iterator::hasPrevious);
        Supplier<GuiEventListener> supplier = forward ? iterator::next : (getter = iterator::previous);
        while (test.getAsBoolean()) {
            GuiEventListener child2 = getter.get();
            ComponentPath focusPath = child2.nextFocusPath(tabNavigation);
            if (focusPath == null) continue;
            return ComponentPath.path(this, focusPath);
        }
        return null;
    }

    private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        GuiEventListener focus = this.getFocused();
        ScreenDirection direction = arrowNavigation.direction();
        if (focus == null) {
            if (arrowNavigation.previousFocus() != null) {
                return ComponentPath.path(this, this.nextFocusPathInDirection(arrowNavigation.previousFocus(), arrowNavigation.direction(), null, arrowNavigation));
            }
            ScreenRectangle borderRectangle = this.getBorderForArrowNavigation(direction.getOpposite());
            return ComponentPath.path(this, this.nextFocusPathInDirection(borderRectangle, direction, null, arrowNavigation));
        }
        ScreenRectangle focusedRectangle = focus.getBorderForArrowNavigation(direction);
        return ComponentPath.path(this, this.nextFocusPathInDirection(focusedRectangle, arrowNavigation.direction(), focus, arrowNavigation.with(focusedRectangle)));
    }

    private @Nullable ComponentPath nextFocusPathInDirection(ScreenRectangle focusedRectangle, ScreenDirection direction, @Nullable GuiEventListener excluded, FocusNavigationEvent.ArrowNavigation navigationEvent) {
        ScreenAxis axis = direction.getAxis();
        ScreenAxis otherAxis = axis.orthogonal();
        ScreenDirection positiveDirectionOtherAxis = otherAxis.getPositive();
        int focusedFirstBound = focusedRectangle.getBoundInDirection(direction.getOpposite());
        ArrayList<GuiEventListener> potentialChildren = new ArrayList<GuiEventListener>();
        for (GuiEventListener guiEventListener : this.children()) {
            ScreenRectangle childRectangle;
            if (guiEventListener == excluded || !(childRectangle = guiEventListener.getRectangle()).overlapsInAxis(focusedRectangle, otherAxis)) continue;
            int childFirstBound = childRectangle.getBoundInDirection(direction.getOpposite());
            if (direction.isAfter(childFirstBound, focusedFirstBound)) {
                potentialChildren.add(guiEventListener);
                continue;
            }
            if (childFirstBound != focusedFirstBound || !direction.isAfter(childRectangle.getBoundInDirection(direction), focusedRectangle.getBoundInDirection(direction))) continue;
            potentialChildren.add(guiEventListener);
        }
        Comparator<GuiEventListener> primaryComparator = Comparator.comparing(child -> child.getRectangle().getBoundInDirection(direction.getOpposite()), direction.coordinateValueComparator());
        Comparator<GuiEventListener> comparator = Comparator.comparing(child -> child.getRectangle().getBoundInDirection(positiveDirectionOtherAxis.getOpposite()), positiveDirectionOtherAxis.coordinateValueComparator());
        potentialChildren.sort(primaryComparator.thenComparing(comparator));
        for (GuiEventListener child3 : potentialChildren) {
            ComponentPath componentPath = child3.nextFocusPath(navigationEvent);
            if (componentPath == null) continue;
            return componentPath;
        }
        return this.nextFocusPathVaguelyInDirection(focusedRectangle, direction, excluded, navigationEvent);
    }

    private @Nullable ComponentPath nextFocusPathVaguelyInDirection(ScreenRectangle focusedRectangle, ScreenDirection direction, @Nullable GuiEventListener excluded, FocusNavigationEvent navigationEvent) {
        ScreenAxis axis = direction.getAxis();
        ScreenAxis otherAxis = axis.orthogonal();
        ArrayList<Pair> potentialChildren = new ArrayList<Pair>();
        ScreenPosition focusedSideCenter = ScreenPosition.of(axis, focusedRectangle.getBoundInDirection(direction), focusedRectangle.getCenterInAxis(otherAxis));
        for (GuiEventListener guiEventListener : this.children()) {
            ScreenRectangle childRectangle;
            ScreenPosition childOpposingSideCenter;
            if (guiEventListener == excluded || !direction.isAfter((childOpposingSideCenter = ScreenPosition.of(axis, (childRectangle = guiEventListener.getRectangle()).getBoundInDirection(direction.getOpposite()), childRectangle.getCenterInAxis(otherAxis))).getCoordinate(axis), focusedSideCenter.getCoordinate(axis))) continue;
            long distanceSquared = Vector2i.distanceSquared((int)focusedSideCenter.x(), (int)focusedSideCenter.y(), (int)childOpposingSideCenter.x(), (int)childOpposingSideCenter.y());
            potentialChildren.add(Pair.of((Object)guiEventListener, (Object)distanceSquared));
        }
        potentialChildren.sort(Comparator.comparingDouble(Pair::getSecond));
        for (Pair pair : potentialChildren) {
            ComponentPath componentPath = ((GuiEventListener)pair.getFirst()).nextFocusPath(navigationEvent);
            if (componentPath == null) continue;
            return componentPath;
        }
        return null;
    }
}

