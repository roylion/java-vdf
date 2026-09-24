package io.github.roylion.condition;

/**
 * @author liugenxin 2026/9/22 14:16
 */
public class VdfConditionParser {

    public static VdfCondition parse(String condition) {
        if (condition.startsWith("[") && condition.endsWith("]")) {
            condition = condition.substring(1, condition.length() - 1);
        }
        return doParse(condition);
    }

    private static VdfCondition parseOr(String condition) {
        int index = condition.indexOf("||");
        String substring = condition.substring(0, index);
        VdfCondition leftCondition = doParse(substring);
        VdfCondition rightCondition = doParse(condition.substring(index + 2));
        return new OrCondition(leftCondition, rightCondition);
    }

    private static VdfCondition parseAnd(String condition) {
        int index = condition.indexOf("&&");
        String substring = condition.substring(0, index);
        VdfCondition leftCondition = doParse(substring);
        VdfCondition rightCondition = doParse(condition.substring(index + 2));
        return new AndCondition(leftCondition, rightCondition);
    }

    private static VdfCondition parseNot(String condition) {
        return new NotCondition(doParse(condition.substring(1)));
    }

    private static VdfCondition doParse(String condition) {
        if (condition.contains("||")) {
            return parseOr(condition);
        } else if (condition.contains("&&")) {
            return parseAnd(condition);
        } else if (condition.contains("!")) {
            return parseNot(condition);
        }
        return new AtomCondition(condition.substring(1));
    }

}
