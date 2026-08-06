/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SetNameFunction
extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<SetNameFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetNameFunction.commonFields(i).and(i.group((App)ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(f -> f.name), (App)LootContext.EntityTarget.CODEC.optionalFieldOf("entity").forGetter(f -> f.resolutionContext), (App)Target.CODEC.optionalFieldOf("target", (Object)Target.CUSTOM_NAME).forGetter(f -> f.target))).apply((Applicative)i, SetNameFunction::new));
    private final Optional<Component> name;
    private final Optional<LootContext.EntityTarget> resolutionContext;
    private final Target target;

    private SetNameFunction(List<LootItemCondition> predicates, Optional<Component> name, Optional<LootContext.EntityTarget> resolutionContext, Target target) {
        super(predicates);
        this.name = name;
        this.resolutionContext = resolutionContext;
        this.target = target;
    }

    public MapCodec<SetNameFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return (Set)DataFixUtils.orElse(this.resolutionContext.map(target -> Set.of(target.contextParam())), Set.of());
    }

    public static UnaryOperator<Component> createResolver(LootContext context, @Nullable LootContext.EntityTarget entityTarget) {
        Entity entity;
        if (entityTarget != null && (entity = context.getOptionalParameter(entityTarget.contextParam())) != null) {
            CommandSourceStack commandSourceStack = entity.createCommandSourceStackForNameResolution(context.getLevel()).withPermission(LevelBasedPermissionSet.GAMEMASTER);
            ResolutionContext resolutionContext = ResolutionContext.create(commandSourceStack);
            return line -> {
                try {
                    return ComponentUtils.resolve(resolutionContext, line);
                }
                catch (CommandSyntaxException e) {
                    LOGGER.warn("Failed to resolve text component", (Throwable)e);
                    return line;
                }
            };
        }
        return line -> line;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        this.name.ifPresent(name -> itemStack.set(this.target.component(), (Component)SetNameFunction.createResolver(context, this.resolutionContext.orElse(null)).apply((Component)name)));
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component value, Target target) {
        return SetNameFunction.simpleBuilder(conditions -> new SetNameFunction((List<LootItemCondition>)conditions, Optional.of(value), Optional.empty(), target));
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component value, Target target, LootContext.EntityTarget resolutionContext) {
        return SetNameFunction.simpleBuilder(conditions -> new SetNameFunction((List<LootItemCondition>)conditions, Optional.of(value), Optional.of(resolutionContext), target));
    }

    public static enum Target implements StringRepresentable
    {
        CUSTOM_NAME("custom_name"),
        ITEM_NAME("item_name");

        public static final Codec<Target> CODEC;
        private final String name;

        private Target(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public DataComponentType<Component> component() {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 1 -> DataComponents.ITEM_NAME;
                case 0 -> DataComponents.CUSTOM_NAME;
            };
        }

        static {
            CODEC = StringRepresentable.fromEnum(Target::values);
        }
    }
}

