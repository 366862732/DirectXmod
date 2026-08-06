/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.TagKey;

public class DialogTags {
    public static final TagKey<Dialog> PAUSE_SCREEN_ADDITIONS = DialogTags.create("pause_screen_additions");
    public static final TagKey<Dialog> QUICK_ACTIONS = DialogTags.create("quick_actions");

    private DialogTags() {
    }

    private static TagKey<Dialog> create(String name) {
        return TagKey.create(Registries.DIALOG, Identifier.withDefaultNamespace(name));
    }
}

