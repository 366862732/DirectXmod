/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components.debug;

import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public interface DebugScreenDisplayer {
    public void addPriorityLine(String var1);

    public void addLine(String var1);

    public void addToGroup(Identifier var1, Collection<String> var2);

    public void addToGroup(Identifier var1, String var2);
}

