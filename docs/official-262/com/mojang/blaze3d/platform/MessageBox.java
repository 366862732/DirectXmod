/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.util.tinyfd.TinyFileDialogs
 */
package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Environment(value=EnvType.CLIENT)
public class MessageBox {
    private static final String DEFAULT_TITLE = "Minecraft";
    public static final String TYPE_OK = "ok";
    public static final String TYPE_OK_CANCEL = "okcancel";
    public static final String TYPE_YES_NO = "yesno";
    public static final String TYPE_YES_NO_CANCEL = "yesnocancel";
    public static final String ICON_INFO = "info";
    public static final String ICON_WARNING = "warning";
    public static final String ICON_ERROR = "error";
    public static final String ICON_QUESTION = "question";
    public static final int BUTTON_CANCEL_OR_NO = 0;
    public static final int BUTTON_OK_OR_YES = 1;
    public static final int BUTTON_NO = 2;

    public static void error(String message) {
        TinyFileDialogs.tinyfd_messageBox((CharSequence)DEFAULT_TITLE, (CharSequence)message, (CharSequence)TYPE_OK, (CharSequence)ICON_ERROR, (int)1);
    }

    public static boolean errorWithContinue(String message) {
        return TinyFileDialogs.tinyfd_messageBox((CharSequence)DEFAULT_TITLE, (CharSequence)message, (CharSequence)TYPE_YES_NO, (CharSequence)ICON_ERROR, (int)1) == 1;
    }
}

