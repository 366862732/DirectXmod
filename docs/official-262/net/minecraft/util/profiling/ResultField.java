/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling;

public final class ResultField
implements Comparable<ResultField> {
    public final double percentage;
    public final double globalPercentage;
    public final long count;
    public final String name;

    public ResultField(String name, double percentage, double globalPercentage, long count) {
        this.name = name;
        this.percentage = percentage;
        this.globalPercentage = globalPercentage;
        this.count = count;
    }

    @Override
    public int compareTo(ResultField resultField) {
        if (resultField.percentage < this.percentage) {
            return -1;
        }
        if (resultField.percentage > this.percentage) {
            return 1;
        }
        return resultField.name.compareTo(this.name);
    }

    public int getColor() {
        return (this.name.hashCode() & 0xAAAAAA) + -12303292;
    }
}

