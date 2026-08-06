/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientAdvancements {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final WorldSessionTelemetryManager telemetryManager;
    private final AdvancementTree tree = new AdvancementTree();
    private final Map<AdvancementHolder, AdvancementProgress> progress = new Object2ObjectOpenHashMap();
    private @Nullable Listener listener;
    private @Nullable AdvancementHolder selectedTab;

    public ClientAdvancements(Minecraft minecraft, WorldSessionTelemetryManager telemetryManager) {
        this.minecraft = minecraft;
        this.telemetryManager = telemetryManager;
    }

    public void update(ClientboundUpdateAdvancementsPacket packet) {
        if (packet.shouldReset()) {
            this.tree.clear();
            this.progress.clear();
        }
        this.tree.remove(packet.getRemoved());
        this.tree.addAll(packet.getAdded());
        for (Map.Entry<Identifier, AdvancementProgress> entry : packet.getProgress().entrySet()) {
            AdvancementNode node = this.tree.get(entry.getKey());
            if (node != null) {
                AdvancementProgress progress = entry.getValue();
                progress.update(node.advancement().requirements());
                this.progress.put(node.holder(), progress);
                if (this.listener != null) {
                    this.listener.onUpdateAdvancementProgress(node, progress);
                }
                if (packet.shouldReset() || !progress.isDone()) continue;
                if (this.minecraft.level != null) {
                    this.telemetryManager.onAdvancementDone(this.minecraft.level, node.holder());
                }
                Optional<DisplayInfo> display = node.advancement().display();
                if (!packet.shouldShowAdvancements() || !display.isPresent() || !display.get().shouldShowToast()) continue;
                this.minecraft.gui.toastManager().addToast(new AdvancementToast(node.holder()));
                continue;
            }
            LOGGER.warn("Server informed client about progress for unknown advancement {}", (Object)entry.getKey());
        }
    }

    public AdvancementTree getTree() {
        return this.tree;
    }

    public void setSelectedTab(@Nullable AdvancementHolder selectedTab, boolean tellServer) {
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null && selectedTab != null && tellServer) {
            connection.send(ServerboundSeenAdvancementsPacket.openedTab(selectedTab));
        }
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab;
            if (this.listener != null) {
                this.listener.onSelectedTabChanged(selectedTab);
            }
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        this.tree.setListener(listener);
        if (listener != null) {
            this.progress.forEach((holder, progress) -> {
                AdvancementNode node = this.tree.get((AdvancementHolder)holder);
                if (node != null) {
                    listener.onUpdateAdvancementProgress(node, (AdvancementProgress)progress);
                }
            });
            listener.onSelectedTabChanged(this.selectedTab);
        }
    }

    public @Nullable AdvancementHolder get(Identifier id) {
        AdvancementNode node = this.tree.get(id);
        return node != null ? node.holder() : null;
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Listener
    extends AdvancementTree.Listener {
        public void onUpdateAdvancementProgress(AdvancementNode var1, AdvancementProgress var2);

        public void onSelectedTabChanged(@Nullable AdvancementHolder var1);
    }
}

