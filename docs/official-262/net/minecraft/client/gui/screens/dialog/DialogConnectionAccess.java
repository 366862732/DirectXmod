/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface DialogConnectionAccess {
    public void disconnect(Component var1);

    public void runCommand(String var1, @Nullable Screen var2);

    public void openDialog(Holder<Dialog> var1, @Nullable Screen var2);

    public void sendCustomAction(Identifier var1, Optional<Tag> var2);

    public ServerLinks serverLinks();
}

