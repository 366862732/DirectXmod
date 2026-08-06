/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.ibm.icu.lang.UCharacter
 *  com.ibm.icu.text.ArabicShaping
 *  com.ibm.icu.text.Bidi
 *  com.ibm.icu.text.BidiRun
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.language;

import com.google.common.collect.Lists;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;

@Environment(value=EnvType.CLIENT)
public class FormattedBidiReorder {
    public static FormattedCharSequence reorder(FormattedText text, boolean defaultRightToLeft) {
        SubStringSource source = SubStringSource.create(text, UCharacter::getMirror, FormattedBidiReorder::shape);
        Bidi bidi = new Bidi(source.getPlainText(), defaultRightToLeft ? 127 : 126);
        bidi.setReorderingMode(0);
        ArrayList result = Lists.newArrayList();
        int runCount = bidi.countRuns();
        for (int i = 0; i < runCount; ++i) {
            BidiRun run = bidi.getVisualRun(i);
            result.addAll(source.substring(run.getStart(), run.getLength(), run.isOddRun()));
        }
        return FormattedCharSequence.composite(result);
    }

    private static String shape(String text) {
        try {
            return new ArabicShaping(8).shape(text);
        }
        catch (Exception e) {
            return text;
        }
    }
}

