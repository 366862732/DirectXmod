/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public interface HoverEvent {
    public static final Codec<HoverEvent> CODEC = Action.CODEC.dispatch("action", HoverEvent::action, action -> action.codec);

    public Action action();

    public static enum Action implements StringRepresentable
    {
        SHOW_TEXT("show_text", true, ShowText.CODEC),
        SHOW_ITEM("show_item", true, ShowItem.CODEC),
        SHOW_ENTITY("show_entity", true, ShowEntity.CODEC);

        public static final Codec<Action> UNSAFE_CODEC;
        public static final Codec<Action> CODEC;
        private final String name;
        private final boolean allowFromServer;
        private final MapCodec<? extends HoverEvent> codec;

        private Action(String name, boolean allowFromServer, MapCodec<? extends HoverEvent> codec) {
            this.name = name;
            this.allowFromServer = allowFromServer;
            this.codec = codec;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public String toString() {
            return "<action " + this.name + ">";
        }

        private static DataResult<Action> filterForSerialization(Action action) {
            if (!action.isAllowedFromServer()) {
                return DataResult.error(() -> "Action not allowed: " + String.valueOf(action));
            }
            return DataResult.success((Object)action, (Lifecycle)Lifecycle.stable());
        }

        static {
            UNSAFE_CODEC = StringRepresentable.fromValues(Action::values);
            CODEC = UNSAFE_CODEC.validate(Action::filterForSerialization);
        }
    }

    public static class EntityTooltipInfo {
        public static final MapCodec<EntityTooltipInfo> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id").forGetter(o -> o.type), (App)UUIDUtil.LENIENT_CODEC.fieldOf("uuid").forGetter(o -> o.uuid), (App)ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(o -> o.name)).apply((Applicative)i, EntityTooltipInfo::new));
        public final EntityType<?> type;
        public final UUID uuid;
        public final Optional<Component> name;
        private @Nullable List<Component> linesCache;

        public EntityTooltipInfo(EntityType<?> type, UUID uuid, @Nullable Component name) {
            this(type, uuid, Optional.ofNullable(name));
        }

        public EntityTooltipInfo(EntityType<?> type, UUID uuid, Optional<Component> name) {
            this.type = type;
            this.uuid = uuid;
            this.name = name;
        }

        public List<Component> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = new ArrayList<Component>();
                this.name.ifPresent(this.linesCache::add);
                this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(Component.literal(this.uuid.toString()));
            }
            return this.linesCache;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            EntityTooltipInfo that = (EntityTooltipInfo)o;
            return this.type.equals(that.type) && this.uuid.equals(that.uuid) && this.name.equals(that.name);
        }

        public int hashCode() {
            int result = this.type.hashCode();
            result = 31 * result + this.uuid.hashCode();
            result = 31 * result + this.name.hashCode();
            return result;
        }
    }

    public record ShowEntity(EntityTooltipInfo entity) implements HoverEvent
    {
        public static final MapCodec<ShowEntity> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)EntityTooltipInfo.CODEC.forGetter(ShowEntity::entity)).apply((Applicative)i, ShowEntity::new));

        @Override
        public Action action() {
            return Action.SHOW_ENTITY;
        }
    }

    public record ShowItem(ItemStackTemplate item) implements HoverEvent
    {
        public static final MapCodec<ShowItem> CODEC = ItemStackTemplate.MAP_CODEC.xmap(ShowItem::new, ShowItem::item);

        @Override
        public Action action() {
            return Action.SHOW_ITEM;
        }
    }

    public record ShowText(Component value) implements HoverEvent
    {
        public static final MapCodec<ShowText> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ComponentSerialization.CODEC.fieldOf("value").forGetter(ShowText::value)).apply((Applicative)i, ShowText::new));

        @Override
        public Action action() {
            return Action.SHOW_TEXT;
        }
    }
}

