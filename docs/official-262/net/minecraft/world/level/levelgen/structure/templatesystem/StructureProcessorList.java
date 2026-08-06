/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

public class StructureProcessorList {
    private final List<StructureProcessor> list;

    public StructureProcessorList(List<StructureProcessor> list) {
        this.list = list;
    }

    public List<StructureProcessor> list() {
        return this.list;
    }

    public String toString() {
        return "ProcessorList[" + String.valueOf(this.list) + "]";
    }
}

