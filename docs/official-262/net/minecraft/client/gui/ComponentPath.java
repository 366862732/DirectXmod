/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui;

import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface ComponentPath {
    public static ComponentPath leaf(GuiEventListener component) {
        return new Leaf(component);
    }

    public static @Nullable ComponentPath path(ContainerEventHandler container, @Nullable ComponentPath childPath) {
        if (childPath == null) {
            return null;
        }
        return new Path(container, childPath);
    }

    public static ComponentPath path(GuiEventListener target, ContainerEventHandler ... containerPath) {
        ComponentPath path = ComponentPath.leaf(target);
        for (ContainerEventHandler container : containerPath) {
            path = ComponentPath.path(container, path);
        }
        return path;
    }

    public GuiEventListener component();

    public void applyFocus(boolean var1);

    public GuiEventListener leafComponent();

    @Environment(value=EnvType.CLIENT)
    public record Leaf(GuiEventListener component) implements ComponentPath
    {
        @Override
        public void applyFocus(boolean focused) {
            this.component.setFocused(focused);
        }

        @Override
        public GuiEventListener leafComponent() {
            return this.component;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static final class Path
    extends Record
    implements ComponentPath {
        private final ContainerEventHandler component;
        private final ComponentPath childPath;

        public Path(ContainerEventHandler component, ComponentPath childPath) {
            this.component = component;
            this.childPath = childPath;
        }

        @Override
        public void applyFocus(boolean focused) {
            if (!focused) {
                this.component.setFocused(null);
            } else {
                this.component.setFocused(this.childPath.component());
            }
            this.childPath.applyFocus(focused);
        }

        @Override
        public GuiEventListener leafComponent() {
            return this.childPath.leafComponent();
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{Path.class, "component;childPath", "component", "childPath"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{Path.class, "component;childPath", "component", "childPath"}, this);
        }

        @Override
        public final boolean equals(Object o) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{Path.class, "component;childPath", "component", "childPath"}, this, o);
        }

        @Override
        public ContainerEventHandler component() {
            return this.component;
        }

        public ComponentPath childPath() {
            return this.childPath;
        }
    }
}

