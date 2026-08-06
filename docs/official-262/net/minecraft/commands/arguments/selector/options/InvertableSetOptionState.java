/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands.arguments.selector.options;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

public class InvertableSetOptionState {
    private Limitation state = Limitation.NONE;
    private final Set<Identifier> tags = new HashSet<Identifier>();

    private boolean canLimitToSingle() {
        return this.state == Limitation.NONE;
    }

    private boolean canLimitToMultiple() {
        return this.state != Limitation.SINGLE;
    }

    public boolean canParsePositiveElement() {
        return this.canLimitToSingle();
    }

    public boolean canParseNegativeElement() {
        return this.canLimitToMultiple();
    }

    public boolean canParseAnyTag() {
        return this.canLimitToMultiple();
    }

    public boolean canParseTag(Identifier tag) {
        return this.canParseAnyTag() && !this.tags.contains(tag);
    }

    public boolean canParseAny() {
        return this.state != Limitation.SINGLE;
    }

    public boolean canParseElement(boolean inverted) {
        return inverted ? this.canParseNegativeElement() : this.canParsePositiveElement();
    }

    private void markParsedSingle() {
        this.state = Limitation.SINGLE;
    }

    private void markParsedMultiple() {
        this.state = Limitation.MULTIPLE;
    }

    public void markParsedPositiveElement() {
        this.markParsedSingle();
    }

    public void markParsedNegativeElement() {
        this.markParsedMultiple();
    }

    public void markParsedTag(Identifier tag) {
        this.markParsedMultiple();
        this.tags.add(tag);
    }

    public void markParsedElement(boolean inverted) {
        if (inverted) {
            this.markParsedNegativeElement();
        } else {
            this.markParsedPositiveElement();
        }
    }

    private static enum Limitation {
        NONE,
        SINGLE,
        MULTIPLE;

    }
}

