/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class VertexFormat {
    private static final int VERTEX_ALIGNMENT = 4;
    public static final int MAX_VERTEX_ELEMENTS = 16;
    private final Map<String, VertexFormatElement> elements = new Object2ObjectArrayMap(16);
    private final int vertexSize;
    private final int stepRate;
    private final List<VertexFormatElement> elementValues;

    private VertexFormat(List<VertexFormatElement> elements, int vertexSize, int stepRate) {
        this.vertexSize = vertexSize;
        this.stepRate = stepRate;
        for (VertexFormatElement element : elements) {
            this.elements.putIfAbsent(element.name(), element);
        }
        this.elementValues = elements;
    }

    public static Builder builder(int stepRate) {
        return new Builder(stepRate);
    }

    public String toString() {
        return "VertexFormat" + this.elementValues.stream().map(VertexFormatElement::name).collect(Collectors.joining(", ", "[", "]"));
    }

    public int getVertexSize() {
        return this.vertexSize;
    }

    public int getStepRate() {
        return this.stepRate;
    }

    public List<VertexFormatElement> getElements() {
        return this.elementValues;
    }

    public @Nullable VertexFormatElement getElement(String attributeName) {
        return this.elements.get(attributeName);
    }

    public boolean contains(String attributeName) {
        return this.elements.containsKey(attributeName);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VertexFormat)) return false;
        VertexFormat format = (VertexFormat)o;
        if (!this.elements.equals(format.elements)) return false;
        if (this.vertexSize != format.vertexSize) return false;
        return true;
    }

    public int hashCode() {
        return this.elementValues.hashCode();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final List<VertexFormatElement> elements = new ArrayList<VertexFormatElement>(16);
        private int offset = 0;
        private final int stepRate;

        private Builder(int stepRate) {
            this.stepRate = stepRate;
        }

        private void createAttribute(String name, int offset, GpuFormat elementFormat) {
            if (this.elements.size() >= 16) {
                throw new IllegalArgumentException("Having more than 16 attributes are not supported");
            }
            if (!Mth.isMultipleOf(offset, elementFormat.byteAlignment())) {
                throw new IllegalArgumentException(name + " is not aligned to " + elementFormat.byteAlignment() + " as required by " + String.valueOf((Object)elementFormat));
            }
            VertexFormatElement element = new VertexFormatElement(name, offset, elementFormat);
            this.elements.add(element);
        }

        private void validateUniqueName(String name) {
            for (VertexFormatElement element : this.elements) {
                if (!element.name().equals(name)) continue;
                throw new IllegalArgumentException("Another vertex attribute exists with the name " + name);
            }
        }

        public Builder addAttribute(String name, GpuFormat elementFormat) {
            this.validateUniqueName(name);
            this.createAttribute(name, this.offset, elementFormat);
            this.offset += elementFormat.blockSize();
            return this;
        }

        public Builder addAttribute(String name, int stride, GpuFormat elementFormat) {
            this.validateUniqueName(name);
            this.createAttribute(name, this.offset, elementFormat);
            this.offset += stride;
            return this;
        }

        public Builder addAttribute(String name, GpuFormat elementFormat, int columnCount) {
            this.validateUniqueName(name);
            for (int i = 0; i < columnCount; ++i) {
                this.createAttribute(name, this.offset, elementFormat);
                this.offset += elementFormat.blockSize();
            }
            return this;
        }

        public Builder addAttribute(String name, int offset, int stride, GpuFormat elementFormat, int columnCount) {
            this.validateUniqueName(name);
            int offsetTracker = offset;
            for (int i = 0; i < columnCount; ++i) {
                this.createAttribute(name, offsetTracker, elementFormat);
                offsetTracker += stride;
            }
            this.offset = Math.max(this.offset, offsetTracker);
            return this;
        }

        public VertexFormat build() {
            int vertexSize = this.offset;
            if (!Mth.isMultipleOf(vertexSize, 4)) {
                throw new IllegalStateException("Vertex size must be a multiple of 4, was " + vertexSize);
            }
            return new VertexFormat(this.elements, vertexSize, this.stepRate);
        }
    }
}

