/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.chest;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class ChestModel
extends Model<Float> {
    private static final String BOTTOM = "bottom";
    private static final String LID = "lid";
    private static final String LOCK = "lock";
    private final ModelPart lid;
    private final ModelPart lock;

    public ChestModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutCull);
        this.lid = root.getChild(LID);
        this.lock = root.getChild(LOCK);
    }

    public static LayerDefinition createSingleBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0f, 0.0f, 1.0f, 14.0f, 10.0f, 14.0f), PartPose.ZERO);
        root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0f, 0.0f, 0.0f, 14.0f, 5.0f, 14.0f), PartPose.offset(0.0f, 9.0f, 1.0f));
        root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(7.0f, -2.0f, 14.0f, 2.0f, 4.0f, 1.0f), PartPose.offset(0.0f, 9.0f, 1.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createDoubleBodyRightLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Set<Direction> visibleFaces = Util.allOfEnumExcept(Direction.EAST);
        root.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(1.0f, 0.0f, 1.0f, 15.0f, 10.0f, 14.0f, visibleFaces), PartPose.ZERO);
        root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(1.0f, 0.0f, 0.0f, 15.0f, 5.0f, 14.0f, visibleFaces), PartPose.offset(0.0f, 9.0f, 1.0f));
        root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(15.0f, -2.0f, 14.0f, 1.0f, 4.0f, 1.0f, visibleFaces), PartPose.offset(0.0f, 9.0f, 1.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createDoubleBodyLeftLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Set<Direction> visibleFaces = Util.allOfEnumExcept(Direction.WEST);
        root.addOrReplaceChild(BOTTOM, CubeListBuilder.create().texOffs(0, 19).addBox(0.0f, 0.0f, 1.0f, 15.0f, 10.0f, 14.0f, visibleFaces), PartPose.ZERO);
        root.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, 0.0f, 15.0f, 5.0f, 14.0f, visibleFaces), PartPose.offset(0.0f, 9.0f, 1.0f));
        root.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -2.0f, 14.0f, 1.0f, 4.0f, 1.0f, visibleFaces), PartPose.offset(0.0f, 9.0f, 1.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Float open) {
        super.setupAnim(open);
        this.lock.xRot = this.lid.xRot = -(open.floatValue() * 1.5707964f);
    }
}

