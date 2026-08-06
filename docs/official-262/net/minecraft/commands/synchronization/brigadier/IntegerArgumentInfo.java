/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 */
package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentInfo
implements ArgumentTypeInfo<IntegerArgumentType, Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf out) {
        boolean hasMin = template.min != Integer.MIN_VALUE;
        boolean hasMax = template.max != Integer.MAX_VALUE;
        out.writeByte(ArgumentUtils.createNumberFlags(hasMin, hasMax));
        if (hasMin) {
            out.writeInt(template.min);
        }
        if (hasMax) {
            out.writeInt(template.max);
        }
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf in) {
        byte flags = in.readByte();
        int min = ArgumentUtils.numberHasMin(flags) ? in.readInt() : Integer.MIN_VALUE;
        int max = ArgumentUtils.numberHasMax(flags) ? in.readInt() : Integer.MAX_VALUE;
        return new Template(this, min, max);
    }

    @Override
    public void serializeToJson(Template template, JsonObject out) {
        if (template.min != Integer.MIN_VALUE) {
            out.addProperty("min", (Number)template.min);
        }
        if (template.max != Integer.MAX_VALUE) {
            out.addProperty("max", (Number)template.max);
        }
    }

    @Override
    public Template unpack(IntegerArgumentType argument) {
        return new Template(this, argument.getMinimum(), argument.getMaximum());
    }

    public final class Template
    implements ArgumentTypeInfo.Template<IntegerArgumentType> {
        private final int min;
        private final int max;
        final /* synthetic */ IntegerArgumentInfo this$0;

        private Template(IntegerArgumentInfo this$0, int min, int max) {
            IntegerArgumentInfo integerArgumentInfo = this$0;
            Objects.requireNonNull(integerArgumentInfo);
            this.this$0 = integerArgumentInfo;
            this.min = min;
            this.max = max;
        }

        @Override
        public IntegerArgumentType instantiate(CommandBuildContext context) {
            return IntegerArgumentType.integer((int)this.min, (int)this.max);
        }

        @Override
        public ArgumentTypeInfo<IntegerArgumentType, ?> type() {
            return this.this$0;
        }
    }
}

