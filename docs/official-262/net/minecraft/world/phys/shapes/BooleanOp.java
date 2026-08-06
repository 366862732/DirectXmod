/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.phys.shapes;

public interface BooleanOp {
    public static final BooleanOp FALSE = (first, second) -> false;
    public static final BooleanOp NOT_OR = (first, second) -> !first && !second;
    public static final BooleanOp ONLY_SECOND = (first, second) -> second && !first;
    public static final BooleanOp NOT_FIRST = (first, second) -> !first;
    public static final BooleanOp ONLY_FIRST = (first, second) -> first && !second;
    public static final BooleanOp NOT_SECOND = (first, second) -> !second;
    public static final BooleanOp NOT_SAME = (first, second) -> first != second;
    public static final BooleanOp NOT_AND = (first, second) -> !first || !second;
    public static final BooleanOp AND = (first, second) -> first && second;
    public static final BooleanOp SAME = (first, second) -> first == second;
    public static final BooleanOp SECOND = (first, second) -> second;
    public static final BooleanOp CAUSES = (first, second) -> !first || second;
    public static final BooleanOp FIRST = (first, second) -> first;
    public static final BooleanOp CAUSED_BY = (first, second) -> first || !second;
    public static final BooleanOp OR = (first, second) -> first || second;
    public static final BooleanOp TRUE = (first, second) -> true;

    public boolean apply(boolean var1, boolean var2);
}

