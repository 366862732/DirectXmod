/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChannelAccess {
    private final Set<ChannelHandle> channels = Sets.newIdentityHashSet();
    private final Library library;
    private final Executor executor;

    public ChannelAccess(Library library, Executor executor) {
        this.library = library;
        this.executor = executor;
    }

    public CompletableFuture<@Nullable ChannelHandle> createHandle(Library.Pool pool) {
        CompletableFuture<@Nullable ChannelHandle> result = new CompletableFuture<ChannelHandle>();
        this.executor.execute(() -> {
            Channel channel = this.library.acquireChannel(pool);
            if (channel != null) {
                ChannelHandle handle = new ChannelHandle(this, channel);
                this.channels.add(handle);
                result.complete(handle);
            } else {
                result.complete(null);
            }
        });
        return result;
    }

    public void executeOnChannels(Consumer<Stream<Channel>> action) {
        this.executor.execute(() -> action.accept(this.channels.stream().map(channelHandle -> channelHandle.channel).filter(Objects::nonNull)));
    }

    public void scheduleTick() {
        this.executor.execute(() -> {
            Iterator<ChannelHandle> it = this.channels.iterator();
            while (it.hasNext()) {
                ChannelHandle handle = it.next();
                handle.channel.updateStream();
                if (!handle.channel.stopped()) continue;
                handle.release();
                it.remove();
            }
        });
    }

    public void clear() {
        this.channels.forEach(ChannelHandle::release);
        this.channels.clear();
    }

    @Environment(value=EnvType.CLIENT)
    public class ChannelHandle {
        private @Nullable Channel channel;
        private boolean stopped;
        final /* synthetic */ ChannelAccess this$0;

        public boolean isStopped() {
            return this.stopped;
        }

        public ChannelHandle(ChannelAccess this$0, Channel channel) {
            ChannelAccess channelAccess = this$0;
            Objects.requireNonNull(channelAccess);
            this.this$0 = channelAccess;
            this.channel = channel;
        }

        public void execute(Consumer<Channel> action) {
            this.this$0.executor.execute(() -> {
                if (this.channel != null) {
                    action.accept(this.channel);
                }
            });
        }

        public void release() {
            this.stopped = true;
            this.this$0.library.releaseChannel(this.channel);
            this.channel = null;
        }
    }
}

