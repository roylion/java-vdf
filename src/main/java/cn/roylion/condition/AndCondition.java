package cn.roylion.condition;

import java.util.Set;

/**
 * @author liugenxin 2026/9/22 14:15
 */
public class AndCondition implements VdfCondition {

    private final VdfCondition leftCondition;
    private final VdfCondition rightCondition;

    public AndCondition(VdfCondition leftCondition, VdfCondition rightCondition) {
        this.leftCondition = leftCondition;
        this.rightCondition = rightCondition;
    }

    @Override
    public boolean evaluate(Set<String> conditions) {
        return leftCondition.evaluate(conditions)
                && rightCondition.evaluate(conditions);
    }
}
