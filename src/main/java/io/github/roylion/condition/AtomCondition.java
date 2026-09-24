package io.github.roylion.condition;

import java.util.Set;

/**
 * @author liugenxin 2026/9/22 14:09
 */
public class AtomCondition implements VdfCondition {

    private final String condition;

    public AtomCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public boolean evaluate(Set<String> conditions) {
        return conditions.contains(condition);
    }
}
