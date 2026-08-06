/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.monster.guardian;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class GuardianModel
extends EntityModel<GuardianRenderState> {
    public static final MeshTransformer ELDER_GUARDIAN_SCALE = MeshTransformer.scaling(2.35f);
    private static final float[] SPIKE_X_ROT = new float[]{1.75f, 0.25f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 1.25f, 0.75f, 0.0f, 0.0f};
    private static final float[] SPIKE_Y_ROT = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.25f, 1.75f, 1.25f, 0.75f, 0.0f, 0.0f, 0.0f, 0.0f};
    private static final float[] SPIKE_Z_ROT = new float[]{0.0f, 0.0f, 0.25f, 1.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.75f, 1.25f};
    private static final float[] SPIKE_X = new float[]{0.0f, 0.0f, 8.0f, -8.0f, -8.0f, 8.0f, 8.0f, -8.0f, 0.0f, 0.0f, 8.0f, -8.0f};
    private static final float[] SPIKE_Y = new float[]{-8.0f, -8.0f, -8.0f, -8.0f, 0.0f, 0.0f, 0.0f, 0.0f, 8.0f, 8.0f, 8.0f, 8.0f};
    private static final float[] SPIKE_Z = new float[]{8.0f, -8.0f, 0.0f, 0.0f, -8.0f, -8.0f, 8.0f, 8.0f, 8.0f, -8.0f, 0.0f, 0.0f};
    private static final String EYE = "eye";
    private static final String TAIL_0 = "tail0";
    private static final String TAIL_1 = "tail1";
    private static final String TAIL_2 = "tail2";
    private final ModelPart head;
    private final ModelPart eye;
    private final ModelPart[] spikeParts = new ModelPart[12];
    private final ModelPart[] tailParts;

    public GuardianModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        for (int i = 0; i < this.spikeParts.length; ++i) {
            this.spikeParts[i] = this.head.getChild(GuardianModel.createSpikeName(i));
        }
        this.eye = this.head.getChild(EYE);
        this.tailParts = new ModelPart[3];
        this.tailParts[0] = this.head.getChild(TAIL_0);
        this.tailParts[1] = this.tailParts[0].getChild(TAIL_1);
        this.tailParts[2] = this.tailParts[1].getChild(TAIL_2);
    }

    private static String createSpikeName(int i) {
        return "spike" + i;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, 10.0f, -8.0f, 12.0f, 12.0f, 16.0f).texOffs(0, 28).addBox(-8.0f, 10.0f, -6.0f, 2.0f, 12.0f, 12.0f).texOffs(0, 28).addBox(6.0f, 10.0f, -6.0f, 2.0f, 12.0f, 12.0f, true).texOffs(16, 40).addBox(-6.0f, 8.0f, -6.0f, 12.0f, 2.0f, 12.0f).texOffs(16, 40).addBox(-6.0f, 22.0f, -6.0f, 12.0f, 2.0f, 12.0f), PartPose.ZERO);
        CubeListBuilder spike = CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -4.5f, -1.0f, 2.0f, 9.0f, 2.0f);
        for (int i = 0; i < 12; ++i) {
            float x = GuardianModel.getSpikeX(i, 0.0f, 0.0f);
            float y = GuardianModel.getSpikeY(i, 0.0f, 0.0f);
            float z = GuardianModel.getSpikeZ(i, 0.0f, 0.0f);
            float xRot = (float)Math.PI * SPIKE_X_ROT[i];
            float yRot = (float)Math.PI * SPIKE_Y_ROT[i];
            float zRot = (float)Math.PI * SPIKE_Z_ROT[i];
            head.addOrReplaceChild(GuardianModel.createSpikeName(i), spike, PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
        }
        head.addOrReplaceChild(EYE, CubeListBuilder.create().texOffs(8, 0).addBox(-1.0f, 15.0f, 0.0f, 2.0f, 2.0f, 1.0f), PartPose.offset(0.0f, 0.0f, -8.25f));
        PartDefinition tailPart0 = head.addOrReplaceChild(TAIL_0, CubeListBuilder.create().texOffs(40, 0).addBox(-2.0f, 14.0f, 7.0f, 4.0f, 4.0f, 8.0f), PartPose.ZERO);
        PartDefinition tailPart1 = tailPart0.addOrReplaceChild(TAIL_1, CubeListBuilder.create().texOffs(0, 54).addBox(0.0f, 14.0f, 0.0f, 3.0f, 3.0f, 7.0f), PartPose.offset(-1.5f, 0.5f, 14.0f));
        tailPart1.addOrReplaceChild(TAIL_2, CubeListBuilder.create().texOffs(41, 32).addBox(0.0f, 14.0f, 0.0f, 2.0f, 2.0f, 6.0f).texOffs(25, 19).addBox(1.0f, 10.5f, 3.0f, 1.0f, 9.0f, 9.0f), PartPose.offset(0.5f, 0.5f, 6.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createElderGuardianLayer() {
        return GuardianModel.createBodyLayer().apply(ELDER_GUARDIAN_SCALE);
    }

    @Override
    public void setupAnim(GuardianRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float)Math.PI / 180);
        this.head.xRot = state.xRot * ((float)Math.PI / 180);
        float withdrawal = (1.0f - state.spikesAnimation) * 0.55f;
        this.setupSpikes(state.ageInTicks, withdrawal);
        if (state.lookAtPosition != null && state.lookDirection != null) {
            double dy = state.lookAtPosition.y - state.eyePosition.y;
            this.eye.y = dy > 0.0 ? 0.0f : 1.0f;
            Vec3 viewVector = state.lookDirection;
            viewVector = new Vec3(viewVector.x, 0.0, viewVector.z);
            Vec3 delta = new Vec3(state.eyePosition.x - state.lookAtPosition.x, 0.0, state.eyePosition.z - state.lookAtPosition.z).normalize().yRot(1.5707964f);
            double dot = viewVector.dot(delta);
            this.eye.x = Mth.sqrt((float)Math.abs(dot)) * 2.0f * (float)Math.signum(dot);
        }
        this.eye.visible = true;
        float swim = state.tailAnimation;
        this.tailParts[0].yRot = Mth.sin(swim) * (float)Math.PI * 0.05f;
        this.tailParts[1].yRot = Mth.sin(swim) * (float)Math.PI * 0.1f;
        this.tailParts[2].yRot = Mth.sin(swim) * (float)Math.PI * 0.15f;
    }

    private void setupSpikes(float ageInTicks, float withdrawal) {
        for (int i = 0; i < 12; ++i) {
            this.spikeParts[i].x = GuardianModel.getSpikeX(i, ageInTicks, withdrawal);
            this.spikeParts[i].y = GuardianModel.getSpikeY(i, ageInTicks, withdrawal);
            this.spikeParts[i].z = GuardianModel.getSpikeZ(i, ageInTicks, withdrawal);
        }
    }

    private static float getSpikeOffset(int spike, float ageInTicks, float withdrawal) {
        return 1.0f + Mth.cos(ageInTicks * 1.5f + (float)spike) * 0.01f - withdrawal;
    }

    private static float getSpikeX(int spike, float ageInTicks, float withdrawal) {
        return SPIKE_X[spike] * GuardianModel.getSpikeOffset(spike, ageInTicks, withdrawal);
    }

    private static float getSpikeY(int spike, float ageInTicks, float withdrawal) {
        return 16.0f + SPIKE_Y[spike] * GuardianModel.getSpikeOffset(spike, ageInTicks, withdrawal);
    }

    private static float getSpikeZ(int spike, float ageInTicks, float withdrawal) {
        return SPIKE_Z[spike] * GuardianModel.getSpikeOffset(spike, ageInTicks, withdrawal);
    }
}

