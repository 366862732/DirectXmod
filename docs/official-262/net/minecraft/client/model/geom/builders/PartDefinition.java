/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom.builders;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

@Environment(value=EnvType.CLIENT)
public class PartDefinition {
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();

    PartDefinition(List<CubeDefinition> cubes, PartPose partPose) {
        this.cubes = cubes;
        this.partPose = partPose;
    }

    public PartDefinition addOrReplaceChild(String name, CubeListBuilder cubes, PartPose partPose) {
        PartDefinition child = new PartDefinition(cubes.getCubes(), partPose);
        return this.addOrReplaceChild(name, child);
    }

    public PartDefinition addOrReplaceChild(String name, PartDefinition child) {
        PartDefinition previous = this.children.put(name, child);
        if (previous != null) {
            child.children.putAll(previous.children);
        }
        return child;
    }

    public PartDefinition clearRecursively() {
        for (String name : this.children.keySet()) {
            this.clearChild(name).clearRecursively();
        }
        return this;
    }

    public PartDefinition clearChild(String name) {
        PartDefinition child = this.children.get(name);
        if (child == null) {
            throw new IllegalArgumentException("No child with name: " + name);
        }
        return this.addOrReplaceChild(name, CubeListBuilder.create(), child.partPose);
    }

    public void retainPartsAndChildren(Set<String> parts) {
        for (Map.Entry<String, PartDefinition> entry : this.children.entrySet()) {
            PartDefinition child = entry.getValue();
            if (parts.contains(entry.getKey())) continue;
            this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), child.partPose).retainPartsAndChildren(parts);
        }
    }

    public void retainExactParts(Set<String> parts) {
        for (Map.Entry<String, PartDefinition> entry : this.children.entrySet()) {
            PartDefinition child = entry.getValue();
            if (parts.contains(entry.getKey())) {
                child.clearRecursively();
                continue;
            }
            this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), child.partPose).retainExactParts(parts);
        }
    }

    public ModelPart bake(int texScaleX, int texScaleY) {
        Object2ObjectArrayMap bakedChildren = this.children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> ((PartDefinition)e.getValue()).bake(texScaleX, texScaleY), (a, b) -> a, Object2ObjectArrayMap::new));
        List<ModelPart.Cube> bakedCubes = this.cubes.stream().map(definition -> definition.bake(texScaleX, texScaleY)).toList();
        ModelPart result = new ModelPart(bakedCubes, (Map<String, ModelPart>)bakedChildren);
        result.setInitialPose(this.partPose);
        result.loadPose(this.partPose);
        return result;
    }

    public PartDefinition getChild(String name) {
        return this.children.get(name);
    }

    public Set<Map.Entry<String, PartDefinition>> getChildren() {
        return this.children.entrySet();
    }

    public PartDefinition transformed(UnaryOperator<PartPose> function) {
        PartDefinition newPart = new PartDefinition(this.cubes, (PartPose)function.apply(this.partPose));
        newPart.children.putAll(this.children);
        return newPart;
    }
}

