package io.github.roylion.condition;

import java.util.Set;

/**
 * @author liugenxin 2026/9/22 14:13
 */
public class NotCondition implements VdfCondition {

    private final VdfCondition condition;

    public NotCondition(VdfCondition condition) {
        this.condition = condition;
    }

    @Override
    public boolean evaluate(Set<String> conditions) {
        return !condition.evaluate(conditions);
    }
}
