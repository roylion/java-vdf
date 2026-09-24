package io.github.roylion.merge;

/**
 * 继承/合并时同名子节点的冲突决策, 由 {@link VdfMergeStrategy} 返回, 由合并骨架执行。
 * <p>命名对齐 Java 继承语义：
 * <ul>
 *   <li>{@link #PRESERVE}：子类<b>重写</b>——保留自己的, 忽略（丢弃）父节点属性；</li>
 *   <li>{@link #OVERRIDE}：子类<b>继承</b>——采用父节点的属性覆盖自己的；</li>
 *   <li>{@link #MERGE}：双方都是容器时, 子节点递归合并父节点。</li>
 * </ul>
 *
 * @author liugenxin 2026/9/23
 */
public enum VdfMergeDecision {

    /**
     * 保留当前节点属性, 丢弃目标节点属性
     */
    PRESERVE,

    /**
     * 覆盖: 用父节点属性覆盖子节点属性
     */
    OVERRIDE,

    /**
     * 递归合并双方的子节点（仅当双方都是容器时有效, 否则降级为 PRESERVE）
     */
    MERGE
}
