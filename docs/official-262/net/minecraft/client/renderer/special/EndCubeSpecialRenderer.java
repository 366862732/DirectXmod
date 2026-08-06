/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.StringRepresentable;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class EndCubeSpecialRenderer
implements NoDataSpecialModelRenderer {
    private final RenderType renderType;

    public EndCubeSpecialRenderer(RenderType renderType) {
        this.renderType = renderType;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        AbstractEndPortalRenderer.submitSpecial(this.renderType, poseStack, submitNodeCollector);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        AbstractEndPortalRenderer.getExtents(output);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Type implements StringRepresentable
    {
        PORTAL("portal"),
        GATEWAY("gateway");

        public static final Codec<Type> CODEC;
        private final String name;

        private Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Type effect) implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Type.CODEC.fieldOf("effect").forGetter(Unbaked::effect)).apply((Applicative)i, Unbaked::new));

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new EndCubeSpecialRenderer(switch (this.effect.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> RenderTypes.endPortal();
                case 1 -> RenderTypes.endGateway();
            });
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

