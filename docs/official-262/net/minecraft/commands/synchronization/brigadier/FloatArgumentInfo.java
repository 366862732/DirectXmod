/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.FloatArgumentType
 */
package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class FloatArgumentInfo
implements ArgumentTypeInfo<FloatArgumentType, Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf out) {
        boolean hasMin = template.min != -3.4028235E38f;
        boolean hasMax = template.max != Float.MAX_VALUE;
        out.writeByte(ArgumentUtils.createNumberFlags(hasMin, hasMax));
        if (hasMin) {
            out.writeFloat(template.min);
        }
        if (hasMax) {
            out.writeFloat(template.max);
        }
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf in) {
        byte flags = in.readByte();
        float min = ArgumentUtils.numberHasMin(flags) ? in.readFloat() : -3.4028235E38f;
        float max = ArgumentUtils.numberHasMax(flags) ? in.readFloat() : Float.MAX_VALUE;
        return new Template(this, min, max);
    }

    @Override
    public void serializeToJson(Template template, JsonObject out) {
        if (template.min != -3.4028235E38f) {
            out.addProperty("min", (Number)Float.valueOf(template.min));
        }
        if (template.max != Float.MAX_VALUE) {
            out.addProperty("max", (Number)Float.valueOf(template.max));
        }
    }

    @Override
    public Template unpack(FloatArgumentType argument) {
        return new Template(this, argument.getMinimum(), argument.getMaximum());
    }

    public final class Template
    implements ArgumentTypeInfo.Template<FloatArgumentType> {
        private final float min;
        private final float max;
        final /* synthetic */ FloatArgumentInfo this$0;

        private Template(FloatArgumentInfo this$0, float min, float max) {
            FloatArgumentInfo floatArgumentInfo = this$0;
            Objects.requireNonNull(floatArgumentInfo);
            this.this$0 = floatArgumentInfo;
            this.min = min;
            this.max = max;
        }

        @Override
        public FloatArgumentType instantiate(CommandBuildContext context) {
            return FloatArgumentType.floatArg((float)this.min, (float)this.max);
        }

        @Override
        public ArgumentTypeInfo<FloatArgumentType, ?> type() {
            return this.this$0;
        }
    }
}

