/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.joml.Vector4f
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.PanoramaRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GuiRenderState {
    private static final int DEBUG_RECTANGLE_COLOR = 0x774444FF;
    private final List<Node> strata = new ArrayList<Node>();
    private int firstStratumAfterBlur = Integer.MAX_VALUE;
    private Node current;
    private final Set<Object> itemModelIdentities = new HashSet<Object>();
    private @Nullable ScreenRectangle lastElementBounds;
    public @Nullable PanoramaRenderState panoramaRenderState;
    public Vector4f clearColorOverride = new Vector4f(0.0f);
    public boolean isHudHidden;

    public GuiRenderState() {
        this.nextStratum();
    }

    public void nextStratum() {
        this.current = new Node(null);
        this.strata.add(this.current);
    }

    public void blurBeforeThisStratum() {
        if (this.firstStratumAfterBlur != Integer.MAX_VALUE) {
            throw new IllegalStateException("Can only blur once per frame");
        }
        this.firstStratumAfterBlur = this.strata.size() - 1;
    }

    public void up() {
        if (this.current.up == null) {
            this.current.up = new Node(this.current);
        }
        this.current = this.current.up;
    }

    public void addItem(GuiItemRenderState itemState) {
        if (!this.findAppropriateNode(itemState)) {
            return;
        }
        this.itemModelIdentities.add(itemState.itemStackRenderState().getModelIdentity());
        this.current.addItem(itemState);
        this.addDebugRectangleIfEnabled(itemState.bounds());
    }

    public void addText(GuiTextRenderState textState) {
        if (!this.findAppropriateNode(textState)) {
            return;
        }
        this.current.addText(textState);
        this.addDebugRectangleIfEnabled(textState.bounds());
    }

    public void addPicturesInPictureState(PictureInPictureRenderState picturesInPictureState) {
        if (!this.findAppropriateNode(picturesInPictureState)) {
            return;
        }
        this.current.addPicturesInPictureState(picturesInPictureState);
        this.addDebugRectangleIfEnabled(picturesInPictureState.bounds());
    }

    public void addGuiElement(GuiElementRenderState blitState) {
        if (!this.findAppropriateNode(blitState)) {
            return;
        }
        this.current.addGuiElement(blitState);
        this.addDebugRectangleIfEnabled(blitState.bounds());
    }

    private void addDebugRectangleIfEnabled(@Nullable ScreenRectangle bounds) {
        if (!SharedConstants.DEBUG_RENDER_UI_LAYERING_RECTANGLES || bounds == null) {
            return;
        }
        this.up();
        this.current.addGuiElement(new ColoredRectangleRenderState(RenderPipelines.GUI, TextureSetup.noTexture(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 10000, 10000, 0x774444FF, 0x774444FF, bounds));
    }

    private boolean findAppropriateNode(ScreenArea screenArea) {
        ScreenRectangle bounds = screenArea.bounds();
        if (bounds == null) {
            return false;
        }
        if (this.lastElementBounds != null && this.lastElementBounds.encompasses(bounds)) {
            this.up();
        } else {
            this.navigateToAboveHighestElementWithIntersectingBounds(bounds);
        }
        this.lastElementBounds = bounds;
        return true;
    }

    private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle bounds) {
        Node node = this.strata.getLast();
        while (node.up != null) {
            node = node.up;
        }
        boolean found = false;
        while (!found) {
            boolean bl = found = this.hasIntersection(bounds, node.elementStates) || this.hasIntersection(bounds, node.itemStates) || this.hasIntersection(bounds, node.textStates) || this.hasIntersection(bounds, node.picturesInPictureStates);
            if (node.parent == null) break;
            if (found) continue;
            node = node.parent;
        }
        this.current = node;
        if (found) {
            this.up();
        }
    }

    private boolean hasIntersection(ScreenRectangle bounds, @Nullable List<? extends ScreenArea> states) {
        if (states != null) {
            for (ScreenArea screenArea : states) {
                ScreenRectangle existingBounds = screenArea.bounds();
                if (existingBounds == null || !existingBounds.intersects(bounds)) continue;
                return true;
            }
        }
        return false;
    }

    public void addBlitToCurrentLayer(BlitRenderState blitState) {
        this.current.addGuiElement(blitState);
    }

    public void addGlyphToCurrentLayer(GuiElementRenderState glyphState) {
        this.current.addGlyph(glyphState);
    }

    public Set<Object> getItemModelIdentities() {
        return this.itemModelIdentities;
    }

    public void forEachElement(Consumer<GuiElementRenderState> consumer, TraverseRange range) {
        this.traverse((Node node) -> {
            if (node.elementStates == null && node.glyphStates == null) {
                return;
            }
            if (node.elementStates != null) {
                for (GuiElementRenderState elementState : node.elementStates) {
                    consumer.accept(elementState);
                }
            }
            if (node.glyphStates != null) {
                for (GuiElementRenderState glyphState : node.glyphStates) {
                    consumer.accept(glyphState);
                }
            }
        }, range);
    }

    public void forEachItem(Consumer<GuiItemRenderState> consumer) {
        Node currentBackup = this.current;
        this.traverse((Node node) -> {
            if (node.itemStates != null) {
                this.current = node;
                for (GuiItemRenderState itemState : node.itemStates) {
                    consumer.accept(itemState);
                }
            }
        }, TraverseRange.ALL);
        this.current = currentBackup;
    }

    public void forEachText(Consumer<GuiTextRenderState> consumer) {
        Node currentBackup = this.current;
        this.traverse((Node node) -> {
            if (node.textStates != null) {
                for (GuiTextRenderState textState : node.textStates) {
                    this.current = node;
                    consumer.accept(textState);
                }
            }
        }, TraverseRange.ALL);
        this.current = currentBackup;
    }

    public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> consumer) {
        Node currentBackup = this.current;
        this.traverse((Node node) -> {
            if (node.picturesInPictureStates != null) {
                this.current = node;
                for (PictureInPictureRenderState pictureInPictureState : node.picturesInPictureStates) {
                    consumer.accept(pictureInPictureState);
                }
            }
        }, TraverseRange.ALL);
        this.current = currentBackup;
    }

    public void sortElements(Comparator<GuiElementRenderState> comparator) {
        this.traverse((Node node) -> {
            if (node.elementStates != null) {
                if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
                    Collections.shuffle(node.elementStates);
                }
                node.elementStates.sort(comparator);
            }
        }, TraverseRange.ALL);
    }

    private void traverse(Consumer<Node> consumer, TraverseRange range) {
        int startIndex = 0;
        int endIndex = this.strata.size();
        if (range == TraverseRange.BEFORE_BLUR) {
            endIndex = Math.min(this.firstStratumAfterBlur, this.strata.size());
        } else if (range == TraverseRange.AFTER_BLUR) {
            startIndex = this.firstStratumAfterBlur;
        }
        for (int i = startIndex; i < endIndex; ++i) {
            Node stratum = this.strata.get(i);
            this.traverse(stratum, consumer);
        }
    }

    private void traverse(Node node, Consumer<Node> consumer) {
        consumer.accept(node);
        if (node.up != null) {
            this.traverse(node.up, consumer);
        }
    }

    public void reset() {
        this.itemModelIdentities.clear();
        this.strata.clear();
        this.firstStratumAfterBlur = Integer.MAX_VALUE;
        this.nextStratum();
        this.panoramaRenderState = null;
        this.clearColorOverride.set(0.0f);
    }

    @Environment(value=EnvType.CLIENT)
    private static class Node {
        public final @Nullable Node parent;
        public @Nullable Node up;
        public @Nullable List<GuiElementRenderState> elementStates;
        public @Nullable List<GuiElementRenderState> glyphStates;
        public @Nullable List<GuiItemRenderState> itemStates;
        public @Nullable List<GuiTextRenderState> textStates;
        public @Nullable List<PictureInPictureRenderState> picturesInPictureStates;

        private Node(@Nullable Node parent) {
            this.parent = parent;
        }

        public void addItem(GuiItemRenderState itemState) {
            if (this.itemStates == null) {
                this.itemStates = new ArrayList<GuiItemRenderState>();
            }
            this.itemStates.add(itemState);
        }

        public void addText(GuiTextRenderState textState) {
            if (this.textStates == null) {
                this.textStates = new ArrayList<GuiTextRenderState>();
            }
            this.textStates.add(textState);
        }

        public void addPicturesInPictureState(PictureInPictureRenderState picturesInPictureState) {
            if (this.picturesInPictureStates == null) {
                this.picturesInPictureStates = new ArrayList<PictureInPictureRenderState>();
            }
            this.picturesInPictureStates.add(picturesInPictureState);
        }

        public void addGuiElement(GuiElementRenderState blitState) {
            if (this.elementStates == null) {
                this.elementStates = new ArrayList<GuiElementRenderState>();
            }
            this.elementStates.add(blitState);
        }

        public void addGlyph(GuiElementRenderState glyphState) {
            if (this.glyphStates == null) {
                this.glyphStates = new ArrayList<GuiElementRenderState>();
            }
            this.glyphStates.add(glyphState);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum TraverseRange {
        ALL,
        BEFORE_BLUR,
        AFTER_BLUR;

    }
}

