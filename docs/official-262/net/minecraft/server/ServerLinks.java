/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.server;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public record ServerLinks(List<Entry> entries) {
    public static final ServerLinks EMPTY = new ServerLinks(List.of());
    public static final StreamCodec<ByteBuf, Either<KnownLinkType, Component>> TYPE_STREAM_CODEC = ByteBufCodecs.either(KnownLinkType.STREAM_CODEC, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC);
    public static final StreamCodec<ByteBuf, List<UntrustedEntry>> UNTRUSTED_LINKS_STREAM_CODEC = UntrustedEntry.STREAM_CODEC.apply(ByteBufCodecs.list());

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public Optional<Entry> findKnownType(KnownLinkType type) {
        return this.entries.stream().filter(e -> (Boolean)e.type.map(l -> l == type, r -> false)).findFirst();
    }

    public List<UntrustedEntry> untrust() {
        return this.entries.stream().map(e -> new UntrustedEntry(e.type, e.link.toString())).toList();
    }

    public static enum KnownLinkType {
        BUG_REPORT(0, "report_bug"),
        COMMUNITY_GUIDELINES(1, "community_guidelines"),
        SUPPORT(2, "support"),
        STATUS(3, "status"),
        FEEDBACK(4, "feedback"),
        COMMUNITY(5, "community"),
        WEBSITE(6, "website"),
        FORUMS(7, "forums"),
        NEWS(8, "news"),
        ANNOUNCEMENTS(9, "announcements");

        private static final IntFunction<KnownLinkType> BY_ID;
        public static final StreamCodec<ByteBuf, KnownLinkType> STREAM_CODEC;
        private final int id;
        private final String name;

        private KnownLinkType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        private Component displayName() {
            return Component.translatable("known_server_link." + this.name);
        }

        public Entry create(URI link) {
            return Entry.knownType(this, link);
        }

        static {
            BY_ID = ByIdMap.continuous(e -> e.id, KnownLinkType.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, e -> e.id);
        }
    }

    public record UntrustedEntry(Either<KnownLinkType, Component> type, String link) {
        public static final StreamCodec<ByteBuf, UntrustedEntry> STREAM_CODEC = StreamCodec.composite(TYPE_STREAM_CODEC, UntrustedEntry::type, ByteBufCodecs.STRING_UTF8, UntrustedEntry::link, UntrustedEntry::new);
    }

    public record Entry(Either<KnownLinkType, Component> type, URI link) {
        public static Entry knownType(KnownLinkType type, URI link) {
            return new Entry((Either<KnownLinkType, Component>)Either.left((Object)((Object)type)), link);
        }

        public static Entry custom(Component displayName, URI link) {
            return new Entry((Either<KnownLinkType, Component>)Either.right((Object)displayName), link);
        }

        public Component displayName() {
            return (Component)this.type.map(KnownLinkType::displayName, r -> r);
        }
    }
}

