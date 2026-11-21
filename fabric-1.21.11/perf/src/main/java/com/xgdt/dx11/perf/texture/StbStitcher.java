package com.xgdt.dx11.perf.texture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rectangle packing for {@code Stitcher} atlases using stb_rect_pack.
 *
 * <p>Vanilla {@code Stitcher.stitch} sorts the holders and inserts them one at
 * a time: every insert walks the existing region tree and, when nothing fits,
 * {@code expand} doubles the atlas and doubles the node count.  Building a
 * block atlas with a few thousand sprites therefore does a lot of tree walking
 * and repeated re-packing.  stb_rect_pack answers the same question in one
 * skyline pass.</p>
 *
 * <p>Unlike the original lwjgl3ify/ModernFix packer this one preserves vanilla's
 * mip alignment: holder sizes are always multiples of {@code 1 << mipLevel} and
 * so is every sprite position here.  Packing happens in units of that quantum,
 * which keeps sprite edges mip-aligned without giving up the packing density.
 * Positions are converted back to texels before they are returned.</p>
 *
 * <p>The packer never throws: any failure (sprites that do not fit, a size
 * above the driver limit, non-aligned holders, or the stb natives being
 * unavailable) returns {@code null} so the caller can fall back to vanilla
 * instead of crashing texture loading.</p>
 */
public final class StbStitcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    /** Below this many sprites the vanilla pack is fast enough and safer. */
    public static final int MIN_SPRITES = 100;

    /** Grow attempts after the area-based guess, matching ModernFix. */
    private static final int MAX_GROWTH_ATTEMPTS = 4;

    private StbStitcher() {
    }

    /** True when an atlas of {@code spriteCount} sprites is worth packing here. */
    public static boolean isWorthPacking(int spriteCount) {
        return spriteCount >= MIN_SPRITES;
    }

    /** One sprite's atlas-space position, in texels. */
    public record PackedSprite(Stitcher.Entry entry, int x, int y) {
    }

    /** A packed atlas: power-of-two dimensions plus the position of every sprite. */
    public record PackedAtlas(int width, int height, List<PackedSprite> sprites) {
    }

    /**
     * Packs {@code holders} into an atlas of at most {@code maxWidth} by
     * {@code maxHeight} texels, or returns {@code null} when vanilla should
     * handle the atlas.
     *
     * <p>{@code holders} must already be sorted by descending size, like
     * vanilla sorts them, because the skyline heuristic relies on that
     * ordering.</p>
     */
    public static @Nullable PackedAtlas pack(
        List<?> holders, int maxWidth, int maxHeight, int mipLevel) {
        int count = holders.size();
        if (!isWorthPacking(count) || maxWidth <= 0 || maxHeight <= 0) {
            return null;
        }

        int unit = 1 << mipLevel;
        int maxUnitWidth = maxWidth / unit;
        int maxUnitHeight = maxHeight / unit;
        if (maxUnitWidth <= 0 || maxUnitHeight <= 0) {
            return null;
        }

        try (STBRPRect.Buffer rects = STBRPRect.malloc(count);
             STBRPContext context = STBRPContext.malloc()) {
            long totalArea = 0L;
            int longestWidth = 0;
            int longestHeight = 0;
            for (int i = 0; i < count; i++) {
                if (!(holders.get(i) instanceof StitcherHolderView holder)) {
                    return null;
                }
                int width = holder.dx11$width();
                int height = holder.dx11$height();
                // Vanilla always hands out mip-aligned sizes.  Anything else
                // means the assumptions here no longer hold, so stay vanilla.
                if (width <= 0 || height <= 0
                    || (width & (unit - 1)) != 0
                    || (height & (unit - 1)) != 0) {
                    return null;
                }
                int unitWidth = width / unit;
                int unitHeight = height / unit;
                rects.get(i).set(i, unitWidth, unitHeight, 0, 0, false);
                totalArea += (long) unitWidth * unitHeight;
                longestWidth = Math.max(longestWidth, unitWidth);
                longestHeight = Math.max(longestHeight, unitHeight);
            }

            // Seed with the smallest power of two that fits the widest and
            // tallest sprite, then widen the shorter axis until the area fits.
            int atlasWidth = Mth.smallestEncompassingPowerOfTwo(longestWidth);
            int atlasHeight = Mth.smallestEncompassingPowerOfTwo(longestHeight);
            while ((long) atlasWidth * atlasHeight < totalArea) {
                if (atlasWidth <= atlasHeight) {
                    atlasWidth <<= 1;
                } else {
                    atlasHeight <<= 1;
                }
                if (atlasWidth > maxUnitWidth && atlasHeight > maxUnitHeight) {
                    return null;
                }
            }

            for (int attempt = 0; attempt < MAX_GROWTH_ATTEMPTS; attempt++) {
                if (atlasWidth > maxUnitWidth || atlasHeight > maxUnitHeight) {
                    return null;
                }
                try (STBRPNode.Buffer nodes = STBRPNode.malloc(atlasWidth + 10)) {
                    STBRectPack.stbrp_init_target(context, atlasWidth, atlasHeight, nodes);
                    STBRectPack.stbrp_pack_rects(context, rects);
                }

                if (allPacked(rects, count)) {
                    return buildResult(rects, holders, count, atlasWidth, atlasHeight, unit);
                }
                // The area guess can be too optimistic for badly shaped sets;
                // widen the shorter axis and try again a few times.
                if (atlasWidth <= atlasHeight) {
                    atlasWidth <<= 1;
                } else {
                    atlasHeight <<= 1;
                }
            }
            return null;
        } catch (LinkageError | RuntimeException e) {
            // Missing natives or an stb failure must not break atlas loading.
            LOGGER.debug("[dx11-perf] stb atlas packing failed; using vanilla stitching", e);
            return null;
        }
    }

    private static boolean allPacked(STBRPRect.Buffer rects, int count) {
        for (int i = 0; i < count; i++) {
            if (!rects.get(i).was_packed()) {
                return false;
            }
        }
        return true;
    }

    private static PackedAtlas buildResult(
        STBRPRect.Buffer rects,
        List<?> holders,
        int count,
        int atlasWidth,
        int atlasHeight,
        int unit) {
        List<PackedSprite> sprites = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            STBRPRect rect = rects.get(i);
            StitcherHolderView holder = (StitcherHolderView) holders.get(rect.id());
            sprites.add(new PackedSprite(holder.dx11$entry(), rect.x() * unit, rect.y() * unit));
        }
        return new PackedAtlas(atlasWidth * unit, atlasHeight * unit, sprites);
    }
}
