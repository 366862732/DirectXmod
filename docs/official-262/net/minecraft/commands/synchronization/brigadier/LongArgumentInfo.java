/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.LongArgumentType
 */
package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class LongArgumentInfo
implements ArgumentTypeInfo<LongArgumentType, Template> {
    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf out) {
        boolean hasMin = template.min != Long.MIN_VALUE;
        boolean hasMax = template.max != Long.MAX_VALUE;
        out.writeByte(ArgumentUtils.createNumberFlags(hasMin, hasMax));
        if (hasMin) {
            out.writeLong(template.min);
        }
        if (hasMax) {
            out.writeLong(template.max);
        }
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf in) {
        byte flags = in.readByte();
        long min = ArgumentUtils.numberHasMin(flags) ? in.readLong() : Long.MIN_VALUE;
        long max = ArgumentUtils.numberHasMax(flags) ? in.readLong() : Long.MAX_VALUE;
        return new Template(this, min, max);
    }

    @Override
    public void serializeToJson(Template template, JsonObject out) {
        if (template.min != Long.MIN_VALUE) {
            out.addProperty("min", (Number)template.min);
        }
        if (template.max != Long.MAX_VALUE) {
            out.addProperty("max", (Number)template.max);
        }
    }

    @Override
    public Template unpack(LongArgumentType argument) {
        return new Template(this, argument.getMinimum(), argument.getMaximum());
    }

    public final class Template
    implements ArgumentTypeInfo.Template<LongArgumentType> {
        private final long min;
        private final long max;
        final /* synthetic */ LongArgumentInfo this$0;

        private Template(LongArgumentInfo this$0, long min, long max) {
            LongArgumentInfo longArgumentInfo = this$0;
            Objects.requireNonNull(longArgumentInfo);
            this.this$0 = longArgumentInfo;
            this.min = min;
            this.max = max;
        }

        @Override
        public LongArgumentType instantiate(CommandBuildContext context) {
            return LongArgumentType.longArg((long)this.min, (long)this.max);
        }

        @Override
        public ArgumentTypeInfo<LongArgumentType, ?> type() {
            return this.this$0;
        }
    }
}

