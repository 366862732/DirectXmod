/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 */
package net.minecraft.client.model.object.crystal;

import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class EndCrystalModel
extends EntityModel<EndCrystalRenderState> {
    private static final String OUTER_GLASS = "outer_glass";
    private static final String INNER_GLASS = "inner_glass";
    private static final String BASE = "base";
    private static final float SIN_45 = (float)Math.sin(0.7853981633974483);
    public final ModelPart base;
    public final ModelPart outerGlass;
    public final ModelPart innerGlass;
    public final ModelPart cube;

    public EndCrystalModel(ModelPart root) {
        super(root);
        this.base = root.getChild(BASE);
        this.outerGlass = root.getChild(OUTER_GLASS);
        this.innerGlass = this.outerGlass.getChild(INNER_GLASS);
        this.cube = this.innerGlass.getChild("cube");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float scale = 0.875f;
        CubeListBuilder glassCube = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f);
        PartDefinition outerGlass = root.addOrReplaceChild(OUTER_GLASS, glassCube, PartPose.offset(0.0f, 24.0f, 0.0f));
        PartDefinition innerGlass = outerGlass.addOrReplaceChild(INNER_GLASS, glassCube, PartPose.ZERO.withScale(0.875f));
        innerGlass.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.ZERO.withScale(0.765625f));
        root.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 16).addBox(-6.0f, 0.0f, -6.0f, 12.0f, 4.0f, 12.0f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EndCrystalRenderState state) {
        super.setupAnim(state);
        this.base.visible = state.showsBottom;
        float animationSpeed = state.ageInTicks * 3.0f;
        float crystalY = EndCrystalRenderer.getY(state.ageInTicks) * 16.0f;
        this.outerGlass.y += crystalY / 2.0f;
        this.outerGlass.rotateBy(Axis.YP.rotationDegrees(animationSpeed).rotateAxis(1.0471976f, SIN_45, 0.0f, SIN_45));
        this.innerGlass.rotateBy(new Quaternionf().setAngleAxis(1.0471976f, SIN_45, 0.0f, SIN_45).rotateY(animationSpeed * ((float)Math.PI / 180)));
        this.cube.rotateBy(new Quaternionf().setAngleAxis(1.0471976f, SIN_45, 0.0f, SIN_45).rotateY(animationSpeed * ((float)Math.PI / 180)));
    }
}

