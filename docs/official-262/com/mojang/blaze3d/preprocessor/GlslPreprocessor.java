/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.preprocessor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class GlslPreprocessor {
    private static final String C_COMMENT = "/\\*(?:[^*]|\\*+[^*/])*\\*+/";
    private static final String LINE_COMMENT = "//[^\\v]*";
    private static final Pattern REGEX_MOJ_IMPORT = Pattern.compile("(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(?:\"(.*)\"|<(.*)>))");
    private static final Pattern REGEX_VERSION = Pattern.compile("(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(\\d+))\\b");
    private static final Pattern REGEX_ENDS_WITH_WHITESPACE = Pattern.compile("(?:^|\\v)(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//[^\\v]*))*\\z");

    public List<String> process(String source) {
        Context context = new Context();
        List<String> sourceList = this.processImports(source, context, "");
        sourceList.set(0, this.setVersion(sourceList.get(0), context.glslVersion));
        return sourceList;
    }

    private List<String> processImports(String source, Context context, String parentPath) {
        int thisSourceId = context.sourceId;
        int previousMatchEnd = 0;
        String lineMacro = "";
        ArrayList sourceList = Lists.newArrayList();
        Matcher matcher = REGEX_MOJ_IMPORT.matcher(source);
        while (matcher.find()) {
            boolean isRelative;
            if (GlslPreprocessor.isDirectiveDisabled(source, matcher, previousMatchEnd)) continue;
            String path = matcher.group(2);
            boolean bl = isRelative = path != null;
            if (!isRelative) {
                path = matcher.group(3);
            }
            if (path == null) continue;
            String sourceBeforeImport = source.substring(previousMatchEnd, matcher.start(1));
            String importPath = parentPath + path;
            Object contents = this.applyImport(isRelative, importPath);
            if (!Strings.isNullOrEmpty((String)contents)) {
                if (!StringUtil.endsWithNewLine((String)contents)) {
                    contents = (String)contents + System.lineSeparator();
                }
                int importSourceId = ++context.sourceId;
                List<String> importedSources = this.processImports((String)contents, context, isRelative ? FileUtil.getFullResourcePath(importPath) : "");
                importedSources.set(0, String.format(Locale.ROOT, "#line %d %d\n%s", 0, importSourceId, this.processVersions(importedSources.get(0), context)));
                if (!StringUtil.isBlank(sourceBeforeImport)) {
                    sourceList.add(sourceBeforeImport);
                }
                sourceList.addAll(importedSources);
            } else {
                String disabledImport = isRelative ? String.format(Locale.ROOT, "/*#moj_import \"%s\"*/", path) : String.format(Locale.ROOT, "/*#moj_import <%s>*/", path);
                sourceList.add(lineMacro + sourceBeforeImport + disabledImport);
            }
            int lineCount = StringUtil.lineCount(source.substring(0, matcher.end(1)));
            lineMacro = String.format(Locale.ROOT, "#line %d %d", lineCount, thisSourceId);
            previousMatchEnd = matcher.end(1);
        }
        String remaining = source.substring(previousMatchEnd);
        if (!StringUtil.isBlank(remaining)) {
            sourceList.add(lineMacro + remaining);
        }
        return sourceList;
    }

    private String processVersions(String source, Context context) {
        Matcher matcher = REGEX_VERSION.matcher(source);
        if (matcher.find() && GlslPreprocessor.isDirectiveEnabled(source, matcher)) {
            context.glslVersion = Math.max(context.glslVersion, Integer.parseInt(matcher.group(2)));
            return source.substring(0, matcher.start(1)) + "/*" + source.substring(matcher.start(1), matcher.end(1)) + "*/" + source.substring(matcher.end(1));
        }
        return source;
    }

    private String setVersion(String source, int version) {
        Matcher matcher = REGEX_VERSION.matcher(source);
        if (matcher.find() && GlslPreprocessor.isDirectiveEnabled(source, matcher)) {
            return source.substring(0, matcher.start(2)) + Math.max(version, Integer.parseInt(matcher.group(2))) + source.substring(matcher.end(2));
        }
        return source;
    }

    private static boolean isDirectiveEnabled(String source, Matcher matcher) {
        return !GlslPreprocessor.isDirectiveDisabled(source, matcher, 0);
    }

    private static boolean isDirectiveDisabled(String source, Matcher matcher, int start) {
        int checkLength = matcher.start() - start;
        if (checkLength == 0) {
            return false;
        }
        Matcher preceedingWhiteSpace = REGEX_ENDS_WITH_WHITESPACE.matcher(source.substring(start, matcher.start()));
        if (!preceedingWhiteSpace.find()) {
            return true;
        }
        int lineCommentEnd = preceedingWhiteSpace.end(1);
        return lineCommentEnd == matcher.start();
    }

    public abstract @Nullable String applyImport(boolean var1, String var2);

    public static String injectDefines(String source, ShaderDefines defines) {
        if (defines.isEmpty()) {
            return source;
        }
        int versionLineEnd = source.indexOf(10);
        int injectIndex = versionLineEnd + 1;
        return source.substring(0, injectIndex) + defines.asSourceDirectives() + "#line 1 0\n" + source.substring(injectIndex);
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Context {
        private int glslVersion;
        private int sourceId;

        private Context() {
        }
    }
}

