/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

@Environment(value=EnvType.CLIENT)
public class KnownPacksManager {
    private final PackRepository repository = ServerPacksSource.createVanillaTrustedRepository();
    private final Map<KnownPack, String> knownPackToId;

    public KnownPacksManager() {
        this.repository.reload();
        ImmutableMap.Builder knownPacks = ImmutableMap.builder();
        this.repository.getAvailablePacks().forEach(pack -> {
            PackLocationInfo location = pack.location();
            location.knownPackInfo().ifPresent(knownPack -> knownPacks.put(knownPack, (Object)location.id()));
        });
        this.knownPackToId = knownPacks.build();
    }

    public List<KnownPack> trySelectingPacks(List<KnownPack> packsToSelect) {
        ArrayList<KnownPack> response = new ArrayList<KnownPack>(packsToSelect.size());
        ArrayList<String> selectedPacks = new ArrayList<String>(packsToSelect.size());
        for (KnownPack knownPack : packsToSelect) {
            String knownPackId = this.knownPackToId.get(knownPack);
            if (knownPackId == null) continue;
            selectedPacks.add(knownPackId);
            response.add(knownPack);
        }
        this.repository.setSelected(selectedPacks);
        return response;
    }

    public CloseableResourceManager createResourceManager() {
        List<PackResources> openedPacks = this.repository.openAllSelected();
        return new MultiPackResourceManager(PackType.SERVER_DATA, openedPacks);
    }
}

