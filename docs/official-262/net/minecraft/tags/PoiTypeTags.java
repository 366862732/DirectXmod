/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class PoiTypeTags {
    public static final TagKey<PoiType> ACQUIRABLE_JOB_SITE = PoiTypeTags.create("acquirable_job_site");
    public static final TagKey<PoiType> VILLAGE = PoiTypeTags.create("village");
    public static final TagKey<PoiType> BEE_HOME = PoiTypeTags.create("bee_home");

    private PoiTypeTags() {
    }

    private static TagKey<PoiType> create(String name) {
        return TagKey.create(Registries.POINT_OF_INTEREST_TYPE, Identifier.withDefaultNamespace(name));
    }
}

