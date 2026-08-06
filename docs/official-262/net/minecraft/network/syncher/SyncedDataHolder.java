/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.syncher;

import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

public interface SyncedDataHolder {
    public void onSyncedDataUpdated(EntityDataAccessor<?> var1);

    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> var1);
}

