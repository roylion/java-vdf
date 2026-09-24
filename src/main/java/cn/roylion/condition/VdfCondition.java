package cn.roylion.condition;

import java.util.Set;

/**
 * @author liugenxin 2026/9/22 14:07
 */
public interface VdfCondition {

    boolean evaluate(Set<String> conditions);
}
