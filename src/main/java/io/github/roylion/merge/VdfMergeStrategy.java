package io.github.roylion.merge;

import io.github.roylion.node.VdfNode;

/**
 * 继承/合并时的同名子节点冲突解决策略。
 * <p>策略只做<b>决策</b>（返回 {@link VdfMergeDecision}）, 不做搬移——
 * 继承时的节点替换、递归下钻、深拷贝等由合并骨架统一执行,
 * 以保证"合并结果不污染原数据"的契约不被破坏。
 *
 * @author liugenxin 2026/9/23
 */
@FunctionalInterface
public interface VdfMergeStrategy {

    /**
     * @param source 子侧同名节点
     * @param target 父侧同名节点
     * @param ctx    继承上下文, 含从合并根到当前冲突点的路径
     */
    VdfMergeDecision resolve(VdfNode source, VdfNode target, VdfMergeContext ctx);

    /**
     * 子类覆盖父类属性
     */
    VdfMergeStrategy OVERRIDE = (s, t, ctx) -> VdfMergeDecision.PRESERVE;

    /**
     * 子类合并父类属性, 非对象则覆盖
     */
    VdfMergeStrategy MERGE_OVERRIDE = (s, t, ctx) ->
            s.isObj() && t.isObj() ? VdfMergeDecision.MERGE : VdfMergeDecision.PRESERVE;

    /**
     * 子类继承父类属性
     */
    VdfMergeStrategy INHERIT = (s, t, ctx) -> VdfMergeDecision.OVERRIDE;

    /**
     * 子类合并父类属性, 非对象则继承
     */
    VdfMergeStrategy MERGE_INHERIT = (s, t, ctx) ->
            s.isObj() && t.isObj() ? VdfMergeDecision.MERGE : VdfMergeDecision.OVERRIDE;
}
