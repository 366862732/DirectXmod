/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.ObjectUtils
 */
package net.minecraft.util;

import java.util.function.Supplier;
import org.apache.commons.lang3.ObjectUtils;

public record ModCheck(Confidence confidence, String description) {
    public static ModCheck identify(String expectedBrand, Supplier<String> actualBrand, String component, Class<?> canaryClass) {
        String mod = actualBrand.get();
        if (!expectedBrand.equals(mod)) {
            return new ModCheck(Confidence.DEFINITELY, component + " brand changed to '" + mod + "'");
        }
        if (canaryClass.getSigners() == null) {
            return new ModCheck(Confidence.VERY_LIKELY, component + " jar signature invalidated");
        }
        return new ModCheck(Confidence.PROBABLY_NOT, component + " jar signature and brand is untouched");
    }

    public boolean shouldReportAsModified() {
        return this.confidence.shouldReportAsModified;
    }

    public ModCheck merge(ModCheck other) {
        return new ModCheck((Confidence)((Object)ObjectUtils.max((Comparable[])new Confidence[]{this.confidence, other.confidence})), this.description + "; " + other.description);
    }

    public String fullDescription() {
        return this.confidence.description + " " + this.description;
    }

    public static enum Confidence {
        PROBABLY_NOT("Probably not.", false),
        VERY_LIKELY("Very likely;", true),
        DEFINITELY("Definitely;", true);

        private final String description;
        private final boolean shouldReportAsModified;

        private Confidence(String description, boolean shouldReportAsModified) {
            this.description = description;
            this.shouldReportAsModified = shouldReportAsModified;
        }
    }
}

