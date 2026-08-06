/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.gameevent;

import java.util.function.Consumer;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import org.jspecify.annotations.Nullable;

public class DynamicGameEventListener<T extends GameEventListener> {
    private final T listener;
    private @Nullable SectionPos lastSection;

    public DynamicGameEventListener(T listener) {
        this.listener = listener;
    }

    public void add(ServerLevel level) {
        this.move(level);
    }

    public T getListener() {
        return this.listener;
    }

    public void remove(ServerLevel level) {
        DynamicGameEventListener.ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.unregister((GameEventListener)this.listener));
    }

    public void move(ServerLevel level) {
        this.listener.getListenerSource().getPosition(level).map(SectionPos::of).ifPresent(currentSection -> {
            if (this.lastSection == null || !this.lastSection.equals(currentSection)) {
                DynamicGameEventListener.ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.unregister((GameEventListener)this.listener));
                this.lastSection = currentSection;
                DynamicGameEventListener.ifChunkExists(level, this.lastSection, dispatcher -> dispatcher.register((GameEventListener)this.listener));
            }
        });
    }

    private static void ifChunkExists(LevelReader level, @Nullable SectionPos sectionPos, Consumer<GameEventListenerRegistry> action) {
        if (sectionPos == null) {
            return;
        }
        ChunkAccess chunk = level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.FULL, false);
        if (chunk != null) {
            action.accept(chunk.getListenerRegistry(sectionPos.y()));
        }
    }
}

