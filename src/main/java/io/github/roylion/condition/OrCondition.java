package io.github.roylion.condition;

import java.util.Set;

/**
 * @author liugenxin 2026/9/22 14:14
 */
public class OrCondition implements VdfCondition {

    private final VdfCondition leftCondition;
    private final VdfCondition rightCondition;

    public OrCondition(VdfCondition leftCondition, VdfCondition rightCondition) {
        this.leftCondition = leftCondition;
        this.rightCondition = rightCondition;
    }

    @Override
    public boolean evaluate(Set<String> conditions) {
        return leftCondition.evaluate(conditions)
                || rightCondition.evaluate(conditions);
    }
}
