/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.ArgumentType
 */
package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class SingletonArgumentInfo<A extends ArgumentType<?>>
implements ArgumentTypeInfo<A, Template> {
    private final Template template;

    private SingletonArgumentInfo(Function<CommandBuildContext, A> constructor) {
        this.template = new Template(this, constructor);
    }

    public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextFree(Supplier<T> constructor) {
        return new SingletonArgumentInfo<ArgumentType>(context -> (ArgumentType)constructor.get());
    }

    public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextAware(Function<CommandBuildContext, T> constructor) {
        return new SingletonArgumentInfo<T>(constructor);
    }

    @Override
    public void serializeToNetwork(Template template, FriendlyByteBuf out) {
    }

    @Override
    public void serializeToJson(Template template, JsonObject out) {
    }

    @Override
    public Template deserializeFromNetwork(FriendlyByteBuf in) {
        return this.template;
    }

    @Override
    public Template unpack(A argument) {
        return this.template;
    }

    public final class Template
    implements ArgumentTypeInfo.Template<A> {
        private final Function<CommandBuildContext, A> constructor;
        final /* synthetic */ SingletonArgumentInfo this$0;

        public Template(SingletonArgumentInfo this$0, Function<CommandBuildContext, A> constructor) {
            SingletonArgumentInfo singletonArgumentInfo = this$0;
            Objects.requireNonNull(singletonArgumentInfo);
            this.this$0 = singletonArgumentInfo;
            this.constructor = constructor;
        }

        @Override
        public A instantiate(CommandBuildContext context) {
            return (ArgumentType)this.constructor.apply(context);
        }

        @Override
        public ArgumentTypeInfo<A, ?> type() {
            return this.this$0;
        }
    }
}

