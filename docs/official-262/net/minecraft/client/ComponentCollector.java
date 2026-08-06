/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.FormattedText;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ComponentCollector {
    private final List<FormattedText> parts = Lists.newArrayList();

    public void append(FormattedText component) {
        this.parts.add(component);
    }

    public @Nullable FormattedText getResult() {
        if (this.parts.isEmpty()) {
            return null;
        }
        if (this.parts.size() == 1) {
            return this.parts.get(0);
        }
        return FormattedText.composite(this.parts);
    }

    public FormattedText getResultOrEmpty() {
        FormattedText result = this.getResult();
        return result != null ? result : FormattedText.EMPTY;
    }

    public void reset() {
        this.parts.clear();
    }
}

