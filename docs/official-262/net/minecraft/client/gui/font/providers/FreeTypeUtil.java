/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.util.freetype.FT_Vector
 *  org.lwjgl.util.freetype.FreeType
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.font.providers;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class FreeTypeUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Object LIBRARY_LOCK = new Object();
    private static long library = 0L;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long getLibrary() {
        Object object = LIBRARY_LOCK;
        synchronized (object) {
            if (library == 0L) {
                try (MemoryStack stack = MemoryStack.stackPush();){
                    PointerBuffer libraryBuffer = stack.mallocPointer(1);
                    FreeTypeUtil.assertError(FreeType.FT_Init_FreeType((PointerBuffer)libraryBuffer), "Initializing FreeType library");
                    library = libraryBuffer.get();
                }
            }
            return library;
        }
    }

    public static void assertError(int errorCode, String type) {
        if (errorCode != 0) {
            throw new IllegalStateException("FreeType error: " + FreeTypeUtil.describeError(errorCode) + " (" + type + ")");
        }
    }

    public static boolean checkError(int errorCode, String type) {
        if (errorCode != 0) {
            LOGGER.error("FreeType error: {} ({})", (Object)FreeTypeUtil.describeError(errorCode), (Object)type);
            return true;
        }
        return false;
    }

    private static String describeError(int code) {
        String string = FreeType.FT_Error_String((int)code);
        if (string != null) {
            return string;
        }
        return "Unrecognized error: 0x" + Integer.toHexString(code);
    }

    public static FT_Vector setVector(FT_Vector vector, float x, float y) {
        long fixedPointX = Math.round(x * 64.0f);
        long fixedPointY = Math.round(y * 64.0f);
        return vector.set(fixedPointX, fixedPointY);
    }

    public static float x(FT_Vector vector) {
        return (float)vector.x() / 64.0f;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void destroy() {
        Object object = LIBRARY_LOCK;
        synchronized (object) {
            if (library != 0L) {
                FreeType.FT_Done_Library((long)library);
                library = 0L;
            }
        }
    }
}

