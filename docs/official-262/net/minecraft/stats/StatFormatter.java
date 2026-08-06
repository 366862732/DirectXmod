/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public interface StatFormatter {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
    public static final StatFormatter DEFAULT = NumberFormat.getIntegerInstance(Locale.US)::format;
    public static final StatFormatter DIVIDE_BY_TEN = value -> DECIMAL_FORMAT.format((double)value * 0.1);
    public static final StatFormatter DISTANCE = cm -> {
        double meters = (double)cm / 100.0;
        double kilometers = meters / 1000.0;
        if (kilometers > 0.5) {
            return DECIMAL_FORMAT.format(kilometers) + " km";
        }
        if (meters > 0.5) {
            return DECIMAL_FORMAT.format(meters) + " m";
        }
        return cm + " cm";
    };
    public static final StatFormatter TIME = value -> {
        double seconds = (double)value / 20.0;
        double minutes = seconds / 60.0;
        double hours = minutes / 60.0;
        double days = hours / 24.0;
        double years = days / 365.0;
        if (years > 0.5) {
            return DECIMAL_FORMAT.format(years) + " y";
        }
        if (days > 0.5) {
            return DECIMAL_FORMAT.format(days) + " d";
        }
        if (hours > 0.5) {
            return DECIMAL_FORMAT.format(hours) + " h";
        }
        if (minutes > 0.5) {
            return DECIMAL_FORMAT.format(minutes) + " min";
        }
        return seconds + " s";
    };

    public String format(int var1);
}

