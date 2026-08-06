/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.object.book;

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
import net.minecraft.util.Mth;

@Environment(value=EnvType.CLIENT)
public class BookModel
extends Model<State> {
    private static final String LEFT_PAGES = "left_pages";
    private static final String RIGHT_PAGES = "right_pages";
    private static final String FLIP_PAGE_1 = "flip_page1";
    private static final String FLIP_PAGE_2 = "flip_page2";
    private final ModelPart leftLid;
    private final ModelPart rightLid;
    private final ModelPart leftPages;
    private final ModelPart rightPages;
    private final ModelPart flipPage1;
    private final ModelPart flipPage2;

    public BookModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
        this.leftLid = root.getChild("left_lid");
        this.rightLid = root.getChild("right_lid");
        this.leftPages = root.getChild(LEFT_PAGES);
        this.rightPages = root.getChild(RIGHT_PAGES);
        this.flipPage1 = root.getChild(FLIP_PAGE_1);
        this.flipPage2 = root.getChild(FLIP_PAGE_2);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_lid", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, -5.0f, -0.005f, 6.0f, 10.0f, 0.005f), PartPose.offset(0.0f, 0.0f, -1.0f));
        root.addOrReplaceChild("right_lid", CubeListBuilder.create().texOffs(16, 0).addBox(0.0f, -5.0f, -0.005f, 6.0f, 10.0f, 0.005f), PartPose.offset(0.0f, 0.0f, 1.0f));
        root.addOrReplaceChild("seam", CubeListBuilder.create().texOffs(12, 0).addBox(-1.0f, -5.0f, 0.0f, 2.0f, 10.0f, 0.005f), PartPose.rotation(0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild(LEFT_PAGES, CubeListBuilder.create().texOffs(0, 10).addBox(0.0f, -4.0f, -0.99f, 5.0f, 8.0f, 1.0f), PartPose.ZERO);
        root.addOrReplaceChild(RIGHT_PAGES, CubeListBuilder.create().texOffs(12, 10).addBox(0.0f, -4.0f, -0.01f, 5.0f, 8.0f, 1.0f), PartPose.ZERO);
        CubeListBuilder page = CubeListBuilder.create().texOffs(24, 10).addBox(0.0f, -4.0f, 0.0f, 5.0f, 8.0f, 0.005f);
        root.addOrReplaceChild(FLIP_PAGE_1, page, PartPose.ZERO);
        root.addOrReplaceChild(FLIP_PAGE_2, page, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        float openness = state.openness;
        this.leftLid.yRot = (float)Math.PI + openness;
        this.rightLid.yRot = -openness;
        this.leftPages.yRot = openness;
        this.rightPages.yRot = -openness;
        this.flipPage1.yRot = openness - openness * 2.0f * state.pageFlip1;
        this.flipPage2.yRot = openness - openness * 2.0f * state.pageFlip2;
        this.leftPages.x = Mth.sin(openness);
        this.rightPages.x = Mth.sin(openness);
        this.flipPage1.x = Mth.sin(openness);
        this.flipPage2.x = Mth.sin(openness);
    }

    @Environment(value=EnvType.CLIENT)
    public record State(float openness, float pageFlip1, float pageFlip2) {
        public static State forAnimation(float progress, float pageFlip1, float pageFlip2, float openness) {
            return new State((Mth.sin(progress * 0.02f) * 0.1f + 1.25f) * openness, pageFlip1, pageFlip2);
        }
    }
}

