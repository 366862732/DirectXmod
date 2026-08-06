/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SpriteContents
implements AutoCloseable,
Stitcher.Entry {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int UBO_SIZE = new Std140SizeCalculator().putMat4f().putMat4f().putFloat().putFloat().putInt().get();
    private final Identifier name;
    private final int width;
    private final int height;
    private final NativeImage originalImage;
    private NativeImage[] byMipLevel;
    private final @Nullable AnimatedTexture animatedTexture;
    private final List<MetadataSectionType.WithValue<?>> additionalMetadata;
    private final MipmapStrategy mipmapStrategy;
    private final float alphaCutoffBias;
    private final Transparency transparency;

    public SpriteContents(Identifier name, FrameSize frameSize, NativeImage image) {
        this(name, frameSize, image, Optional.empty(), List.of(), Optional.empty());
    }

    public SpriteContents(Identifier name, FrameSize frameSize, NativeImage image, Optional<AnimationMetadataSection> animationInfo, List<MetadataSectionType.WithValue<?>> additionalMetadata, Optional<TextureMetadataSection> textureInfo) {
        this.name = name;
        this.width = frameSize.width();
        this.height = frameSize.height();
        this.additionalMetadata = additionalMetadata;
        this.animatedTexture = animationInfo.map(animation -> this.createAnimatedTexture(frameSize, image.getWidth(), image.getHeight(), (AnimationMetadataSection)animation)).orElse(null);
        this.originalImage = image;
        this.byMipLevel = new NativeImage[]{this.originalImage};
        this.mipmapStrategy = textureInfo.map(TextureMetadataSection::mipmapStrategy).orElse(MipmapStrategy.AUTO);
        this.alphaCutoffBias = textureInfo.map(TextureMetadataSection::alphaCutoffBias).orElse(Float.valueOf(0.0f)).floatValue();
        this.transparency = image.computeTransparency();
    }

    public void increaseMipLevel(int mipLevel) {
        try {
            this.byMipLevel = MipmapGenerator.generateMipLevels(this.name, this.byMipLevel, mipLevel, this.mipmapStrategy, this.alphaCutoffBias, this.transparency);
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Generating mipmaps for frame");
            CrashReportCategory frameCategory = report.addCategory("Frame being iterated");
            frameCategory.setDetail("Sprite name", this.name);
            frameCategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
            frameCategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
            frameCategory.setDetail("Mipmap levels", mipLevel);
            frameCategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
            throw new ReportedException(report);
        }
    }

    private int getFrameCount() {
        return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
    }

    public boolean isAnimated() {
        return this.getFrameCount() > 1;
    }

    public Transparency transparency() {
        return this.transparency;
    }

    private @Nullable AnimatedTexture createAnimatedTexture(FrameSize frameSize, int fullWidth, int fullHeight, AnimationMetadataSection metadata) {
        ArrayList<FrameInfo> frames;
        int frameRowSize = fullWidth / frameSize.width();
        int frameColumnSize = fullHeight / frameSize.height();
        int totalFrameCount = frameRowSize * frameColumnSize;
        int defaultFrameTime = metadata.defaultFrameTime();
        if (metadata.frames().isEmpty()) {
            frames = new ArrayList<FrameInfo>(totalFrameCount);
            for (int i = 0; i < totalFrameCount; ++i) {
                frames.add(new FrameInfo(i, defaultFrameTime));
            }
        } else {
            List<AnimationFrame> metadataFrames = metadata.frames().get();
            frames = new ArrayList(metadataFrames.size());
            for (AnimationFrame frame : metadataFrames) {
                frames.add(new FrameInfo(frame.index(), frame.timeOr(defaultFrameTime)));
            }
            int index = 0;
            IntOpenHashSet usedFrameIndices = new IntOpenHashSet();
            Iterator iterator = frames.iterator();
            while (iterator.hasNext()) {
                FrameInfo frame = (FrameInfo)iterator.next();
                boolean isValid = true;
                if (frame.time <= 0) {
                    LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, index, frame.time});
                    isValid = false;
                }
                if (frame.index < 0 || frame.index >= totalFrameCount) {
                    LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, index, frame.index});
                    isValid = false;
                }
                if (isValid) {
                    usedFrameIndices.add(frame.index);
                } else {
                    iterator.remove();
                }
                ++index;
            }
            int[] unusedFrameIndices = IntStream.range(0, totalFrameCount).filter(arg_0 -> SpriteContents.lambda$createAnimatedTexture$0((IntSet)usedFrameIndices, arg_0)).toArray();
            if (unusedFrameIndices.length > 0) {
                LOGGER.warn("Unused frames in sprite {}: {}", (Object)this.name, (Object)Arrays.toString(unusedFrameIndices));
            }
        }
        if (frames.size() <= 1) {
            return null;
        }
        return new AnimatedTexture(this, List.copyOf(frames), frameRowSize, metadata.interpolatedFrames());
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public Identifier name() {
        return this.name;
    }

    public IntList getUniqueFrames() {
        return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntList.of((int)1);
    }

    public @Nullable AnimationState createAnimationState(GpuBufferSlice uboSlice, int spriteUboSize) {
        return this.animatedTexture != null ? this.animatedTexture.createAnimationState(uboSlice, spriteUboSize) : null;
    }

    public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> type) {
        for (MetadataSectionType.WithValue<?> metadata : this.additionalMetadata) {
            Optional<T> result = metadata.unwrapToType(type);
            if (!result.isPresent()) continue;
            return result;
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        for (NativeImage image : this.byMipLevel) {
            image.close();
        }
    }

    public String toString() {
        return "SpriteContents{name=" + String.valueOf(this.name) + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
    }

    public boolean isTransparent(int frame, int x, int y) {
        int actualX = x;
        int actualY = y;
        if (this.animatedTexture != null) {
            actualX += this.animatedTexture.getFrameX(frame) * this.width;
            actualY += this.animatedTexture.getFrameY(frame) * this.height;
        }
        return ARGB.alpha(this.originalImage.getPixel(actualX, actualY)) == 0;
    }

    public Transparency computeTransparency(float u0, float v0, float u1, float v1) {
        if (this.transparency.isOpaque()) {
            return this.transparency;
        }
        if (u0 == 0.0f && v0 == 0.0f && u1 == 1.0f && v1 == 1.0f) {
            return this.transparency;
        }
        int x0 = Mth.floor(u0 * (float)this.width);
        int y0 = Mth.floor(v0 * (float)this.height);
        int x1 = Mth.ceil(u1 * (float)this.width);
        int y1 = Mth.ceil(v1 * (float)this.height);
        if (this.animatedTexture != null) {
            IntList uniqueFrames = this.animatedTexture.uniqueFrames;
            Transparency transparency = Transparency.NONE;
            for (int i = 0; i < uniqueFrames.size(); ++i) {
                int frame = uniqueFrames.getInt(i);
                int frameX = this.animatedTexture.getFrameX(frame) * this.width;
                int frameY = this.animatedTexture.getFrameY(frame) * this.height;
                transparency = transparency.or(this.originalImage.computeTransparency(frameX + x0, frameY + y0, frameX + x1, frameY + y1));
            }
            return transparency;
        }
        return this.originalImage.computeTransparency(x0, y0, x1, y1);
    }

    public void uploadFirstFrame(GpuTexture destination, int level) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(destination, this.byMipLevel[level], level, 0, 0, 0);
    }

    private static /* synthetic */ boolean lambda$createAnimatedTexture$0(IntSet usedFrameIndices, int i) {
        return !usedFrameIndices.contains(i);
    }

    @Environment(value=EnvType.CLIENT)
    private class AnimatedTexture {
        private final List<FrameInfo> frames;
        private final IntList uniqueFrames;
        private final int frameRowSize;
        private final boolean interpolateFrames;
        final /* synthetic */ SpriteContents this$0;

        private AnimatedTexture(SpriteContents spriteContents, List<FrameInfo> frames, int frameRowSize, boolean interpolateFrames) {
            SpriteContents spriteContents2 = spriteContents;
            Objects.requireNonNull(spriteContents2);
            this.this$0 = spriteContents2;
            this.frames = frames;
            this.frameRowSize = frameRowSize;
            this.interpolateFrames = interpolateFrames;
            this.uniqueFrames = IntArrayList.toList((IntStream)frames.stream().mapToInt(FrameInfo::index).distinct());
        }

        private int getFrameX(int index) {
            return index % this.frameRowSize;
        }

        private int getFrameY(int index) {
            return index / this.frameRowSize;
        }

        public AnimationState createAnimationState(GpuBufferSlice uboSlice, int spriteUboSize) {
            GpuDevice device = RenderSystem.getDevice();
            Int2ObjectOpenHashMap frameTexturesByIndex = new Int2ObjectOpenHashMap();
            GpuBufferSlice[] spriteUbosByMip = new GpuBufferSlice[this.this$0.byMipLevel.length];
            CommandEncoder encoder = device.createCommandEncoder();
            List<GpuBufferSlice> stagingBuffers = encoder.transientMemory().multiUploadStaging(Arrays.stream(this.this$0.byMipLevel).map(NativeImage::getPixelBytes).toList(), 1L, 16);
            for (int i = 0; i < this.uniqueFrames.size(); ++i) {
                int frame = this.uniqueFrames.getInt(i);
                GpuTexture texture = device.createTexture(() -> String.valueOf(this.this$0.name) + " animation frame " + frame, 5, GpuFormat.RGBA8_UNORM, this.this$0.width, this.this$0.height, 1, this.this$0.byMipLevel.length);
                int offsetX = this.getFrameX(frame) * this.this$0.width;
                int offsetY = this.getFrameY(frame) * this.this$0.height;
                for (int level = 0; level < this.this$0.byMipLevel.length; ++level) {
                    encoder.copyBufferToTexture(stagingBuffers.get(level), offsetX >> level, offsetY >> level, this.this$0.byMipLevel[level].getWidth(), this.this$0.byMipLevel[level].getHeight(), texture, 0, 0, this.this$0.width >> level, this.this$0.height >> level, level, 0);
                }
                frameTexturesByIndex.put(frame, (Object)RenderSystem.getDevice().createTextureView(texture));
            }
            for (int level = 0; level < this.this$0.byMipLevel.length; ++level) {
                spriteUbosByMip[level] = uboSlice.slice(level * spriteUboSize, spriteUboSize);
            }
            return new AnimationState(this.this$0, this, (Int2ObjectMap<GpuTextureView>)frameTexturesByIndex, spriteUbosByMip);
        }

        public IntList getUniqueFrames() {
            return this.uniqueFrames;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record FrameInfo(int index, int time) {
    }

    @Environment(value=EnvType.CLIENT)
    public class AnimationState
    implements AutoCloseable {
        private int frame;
        private int subFrame;
        private final AnimatedTexture animationInfo;
        private final Int2ObjectMap<GpuTextureView> frameTexturesByIndex;
        private final GpuBufferSlice[] spriteUbosByMip;
        private boolean isDirty;

        private AnimationState(SpriteContents this$0, AnimatedTexture animationInfo, Int2ObjectMap<GpuTextureView> frameTexturesByIndex, GpuBufferSlice[] spriteUbosByMip) {
            Objects.requireNonNull(this$0);
            this.isDirty = true;
            this.animationInfo = animationInfo;
            this.frameTexturesByIndex = frameTexturesByIndex;
            this.spriteUbosByMip = spriteUbosByMip;
        }

        public void tick() {
            ++this.subFrame;
            this.isDirty = false;
            FrameInfo currentFrame = this.animationInfo.frames.get(this.frame);
            if (this.subFrame >= currentFrame.time) {
                int oldFrame = currentFrame.index;
                this.frame = (this.frame + 1) % this.animationInfo.frames.size();
                this.subFrame = 0;
                int newFrame = this.animationInfo.frames.get((int)this.frame).index;
                if (oldFrame != newFrame) {
                    this.isDirty = true;
                }
            }
        }

        public GpuBufferSlice getDrawUbo(int level) {
            return this.spriteUbosByMip[level];
        }

        public boolean needsToDraw() {
            return this.animationInfo.interpolateFrames || this.isDirty;
        }

        public void drawToAtlas(RenderPass renderPass, GpuBufferSlice ubo) {
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
            List<FrameInfo> frames = this.animationInfo.frames;
            int oldFrame = frames.get((int)this.frame).index;
            float frameProgress = (float)this.subFrame / (float)this.animationInfo.frames.get((int)this.frame).time;
            int frameProgressAsInt = (int)(frameProgress * 1000.0f);
            if (this.animationInfo.interpolateFrames) {
                int newFrame = frames.get((int)((this.frame + 1) % frames.size())).index;
                renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_INTERPOLATE);
                renderPass.bindTexture("CurrentSprite", (GpuTextureView)this.frameTexturesByIndex.get(oldFrame), sampler);
                renderPass.bindTexture("NextSprite", (GpuTextureView)this.frameTexturesByIndex.get(newFrame), sampler);
            } else if (this.isDirty) {
                renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);
                renderPass.bindTexture("Sprite", (GpuTextureView)this.frameTexturesByIndex.get(oldFrame), sampler);
            }
            renderPass.setUniform("SpriteAnimationInfo", ubo);
            renderPass.draw(6, 1, frameProgressAsInt << 3, 0);
        }

        @Override
        public void close() {
            for (GpuTextureView view : this.frameTexturesByIndex.values()) {
                view.texture().close();
                view.close();
            }
        }
    }
}

