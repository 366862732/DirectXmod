/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TextRenderingUtils {
    private TextRenderingUtils() {
    }

    @VisibleForTesting
    protected static List<String> lineBreak(String text) {
        return Arrays.asList(text.split("\\n"));
    }

    public static List<Line> decompose(String text, LineSegment ... links) {
        return TextRenderingUtils.decompose(text, Arrays.asList(links));
    }

    private static List<Line> decompose(String text, List<LineSegment> links) {
        List<String> brokenLines = TextRenderingUtils.lineBreak(text);
        return TextRenderingUtils.insertLinks(brokenLines, links);
    }

    private static List<Line> insertLinks(List<String> lines, List<LineSegment> links) {
        int linkCount = 0;
        ArrayList processedLines = Lists.newArrayList();
        for (String line : lines) {
            ArrayList segments = Lists.newArrayList();
            List<String> parts = TextRenderingUtils.split(line, "%link");
            for (String part : parts) {
                if ("%link".equals(part)) {
                    segments.add(links.get(linkCount++));
                    continue;
                }
                segments.add(LineSegment.text(part));
            }
            processedLines.add(new Line(segments));
        }
        return processedLines;
    }

    public static List<String> split(String line, String delimiter) {
        int matchIndex;
        if (delimiter.isEmpty()) {
            throw new IllegalArgumentException("Delimiter cannot be the empty string");
        }
        ArrayList parts = Lists.newArrayList();
        int searchStart = 0;
        while ((matchIndex = line.indexOf(delimiter, searchStart)) != -1) {
            if (matchIndex > searchStart) {
                parts.add(line.substring(searchStart, matchIndex));
            }
            parts.add(delimiter);
            searchStart = matchIndex + delimiter.length();
        }
        if (searchStart < line.length()) {
            parts.add(line.substring(searchStart));
        }
        return parts;
    }

    @Environment(value=EnvType.CLIENT)
    public static class LineSegment {
        private final String fullText;
        private final @Nullable String linkTitle;
        private final @Nullable String linkUrl;

        private LineSegment(String fullText) {
            this.fullText = fullText;
            this.linkTitle = null;
            this.linkUrl = null;
        }

        private LineSegment(String fullText, @Nullable String linkTitle, @Nullable String linkUrl) {
            this.fullText = fullText;
            this.linkTitle = linkTitle;
            this.linkUrl = linkUrl;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            LineSegment segment = (LineSegment)o;
            return Objects.equals(this.fullText, segment.fullText) && Objects.equals(this.linkTitle, segment.linkTitle) && Objects.equals(this.linkUrl, segment.linkUrl);
        }

        public int hashCode() {
            return Objects.hash(this.fullText, this.linkTitle, this.linkUrl);
        }

        public String toString() {
            return "Segment{fullText='" + this.fullText + "', linkTitle='" + this.linkTitle + "', linkUrl='" + this.linkUrl + "'}";
        }

        public String renderedText() {
            return this.isLink() ? this.linkTitle : this.fullText;
        }

        public boolean isLink() {
            return this.linkTitle != null;
        }

        public String getLinkUrl() {
            if (!this.isLink()) {
                throw new IllegalStateException("Not a link: " + String.valueOf(this));
            }
            return this.linkUrl;
        }

        public static LineSegment link(String linkTitle, String linkUrl) {
            return new LineSegment(null, linkTitle, linkUrl);
        }

        @VisibleForTesting
        protected static LineSegment text(String fullText) {
            return new LineSegment(fullText);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Line {
        public final List<LineSegment> segments;

        public Line(LineSegment ... segments) {
            this(Arrays.asList(segments));
        }

        public Line(List<LineSegment> segments) {
            this.segments = segments;
        }

        public String toString() {
            return "Line{segments=" + String.valueOf(this.segments) + "}";
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            Line line = (Line)o;
            return Objects.equals(this.segments, line.segments);
        }

        public int hashCode() {
            return Objects.hash(this.segments);
        }
    }
}

