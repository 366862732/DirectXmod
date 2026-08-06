/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.lwjgl.util.freetype.FT_Face
 *  org.lwjgl.util.freetype.FreeType
 */
package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

@Environment(value=EnvType.CLIENT)
public record TrueTypeGlyphProviderDefinition(Identifier location, float size, float oversample, Shift shift, String skip) implements GlyphProviderDefinition
{
    private static final Codec<String> SKIP_LIST_CODEC = Codec.withAlternative((Codec)Codec.STRING, (Codec)Codec.STRING.listOf(), list -> String.join((CharSequence)"", list));
    public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location), (App)Codec.FLOAT.optionalFieldOf("size", (Object)Float.valueOf(11.0f)).forGetter(TrueTypeGlyphProviderDefinition::size), (App)Codec.FLOAT.optionalFieldOf("oversample", (Object)Float.valueOf(1.0f)).forGetter(TrueTypeGlyphProviderDefinition::oversample), (App)Shift.CODEC.optionalFieldOf("shift", (Object)Shift.NONE).forGetter(TrueTypeGlyphProviderDefinition::shift), (App)SKIP_LIST_CODEC.optionalFieldOf("skip", (Object)"").forGetter(TrueTypeGlyphProviderDefinition::skip)).apply((Applicative)i, TrueTypeGlyphProviderDefinition::new));

    @Override
    public GlyphProviderType type() {
        return GlyphProviderType.TTF;
    }

    @Override
    public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
        return Either.left(this::load);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private GlyphProvider load(ResourceManager resourceManager) throws IOException {
        FT_Face face = null;
        ByteBuffer fontData = null;
        try (InputStream resource = resourceManager.open(this.location.withPrefix("font/"));){
            fontData = TextureUtil.readResource(resource);
            Object object = FreeTypeUtil.LIBRARY_LOCK;
            synchronized (object) {
                try (MemoryStack stack = MemoryStack.stackPush();){
                    PointerBuffer faceBuffer = stack.mallocPointer(1);
                    FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face((long)FreeTypeUtil.getLibrary(), (ByteBuffer)fontData, (long)0L, (PointerBuffer)faceBuffer), "Initializing font face");
                    face = FT_Face.create((long)faceBuffer.get());
                }
                String format = FreeType.FT_Get_Font_Format((FT_Face)face);
                if (!"TrueType".equals(format)) {
                    throw new IOException("Font is not in TTF format, was " + format);
                }
                FreeTypeUtil.assertError(FreeType.FT_Select_Charmap((FT_Face)face, (int)FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
                TrueTypeGlyphProvider trueTypeGlyphProvider = new TrueTypeGlyphProvider(fontData, face, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
                return trueTypeGlyphProvider;
            }
        }
        catch (Exception ex) {
            Object object = FreeTypeUtil.LIBRARY_LOCK;
            synchronized (object) {
                if (face != null) {
                    FreeType.FT_Done_Face(face);
                }
            }
            MemoryUtil.memFree((ByteBuffer)fontData);
            throw ex;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Shift(float x, float y) {
        public static final Shift NONE = new Shift(0.0f, 0.0f);
        public static final Codec<Shift> CODEC = Codec.floatRange((float)-512.0f, (float)512.0f).listOf().comapFlatMap(input -> Util.fixedSize(input, 2).map(floats -> new Shift(((Float)floats.get(0)).floatValue(), ((Float)floats.get(1)).floatValue())), shift -> List.of(Float.valueOf(shift.x), Float.valueOf(shift.y)));
    }
}

