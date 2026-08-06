/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.model.object.bell;

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
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BellModel
extends Model<State> {
    private static final String BELL_BODY = "bell_body";
    private final ModelPart bellBody;

    public BellModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
        this.bellBody = root.getChild(BELL_BODY);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bellBody = root.addOrReplaceChild(BELL_BODY, CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -6.0f, -3.0f, 6.0f, 7.0f, 6.0f), PartPose.offset(8.0f, 12.0f, 8.0f));
        bellBody.addOrReplaceChild("bell_base", CubeListBuilder.create().texOffs(0, 13).addBox(4.0f, 4.0f, 4.0f, 8.0f, 2.0f, 8.0f), PartPose.offset(-8.0f, -12.0f, -8.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        float xRot = 0.0f;
        float zRot = 0.0f;
        if (state.shakeDirection != null) {
            float baseRot = Mth.sin(state.ticks / (float)Math.PI) / (4.0f + state.ticks / 3.0f);
            switch (state.shakeDirection) {
                case NORTH: {
                    xRot = -baseRot;
                    break;
                }
                case SOUTH: {
                    xRot = baseRot;
                    break;
                }
                case EAST: {
                    zRot = -baseRot;
                    break;
                }
                case WEST: {
                    zRot = baseRot;
                }
            }
        }
        this.bellBody.xRot = xRot;
        this.bellBody.zRot = zRot;
    }

    @Environment(value=EnvType.CLIENT)
    public record State(float ticks, @Nullable Direction shakeDirection) {
    }
}

