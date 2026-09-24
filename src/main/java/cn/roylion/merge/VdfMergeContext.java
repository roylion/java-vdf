package cn.roylion.merge;

import cn.roylion.node.VdfNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 合并上下文：记录从合并根到当前冲突点的节点栈, 供 {@link VdfMergeStrategy}
 * 做路径感知的决策（如只在特定子树覆盖）。
 * <p>每次 extend / base 创建一个新实例, 由合并骨架在下钻/回退时
 * {@link #push(VdfNode)} / {@link #pop()} 维护（包私有, 必须配对调用）, 策略只能读。
 * <p>非线程安全, 归属单次合并操作。
 *
 * @author liugenxin 2026/9/23
 */
public final class VdfMergeContext {

    /**
     * 节点栈, 栈底是合并根的直接子节点, 栈顶是当前冲突节点;
     * 用尾插尾删实现, 迭代顺序即从根到当前的路径顺序
     */
    private final Deque<VdfNode> stack = new ArrayDeque<>();

    /**
     * 由合并骨架（VdfObjectNode）创建, 使用方不应直接构造
     */
    public VdfMergeContext() {
    }

    /**
     * 从合并根到当前节点的 key 路径, 如 {@code items/paint/wear}（虚拟根的 null key 被跳过）
     */
    public String getPath() {
        return stack.stream()
                .map(VdfNode::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));
    }

    /**
     * 路径分段 key 列表, 从根到当前顺序（不可变副本, key 为 null 的段被跳过）
     */
    public List<String> getSegments() {
        List<String> keys = new ArrayList<>();
        for (VdfNode node : stack) {
            if (node.getKey() != null) {
                keys.add(node.getKey());
            }
        }
        return Collections.unmodifiableList(keys);
    }

    /**
     * 从根到当前的路径节点（含当前冲突节点, 不可变副本）——
     * 策略可借此读取祖先的 condition 等信息
     */
    public List<VdfNode> getNodes() {
        return Collections.unmodifiableList(new ArrayList<>(stack));
    }

    /**
     * 合并骨架下钻时入栈当前节点; 供合并骨架调用, 使用方不应调用
     */
    public void push(VdfNode node) {
        stack.addLast(node);
    }

    /**
     * 合并骨架回退时出栈, 必须与 {@link #push(VdfNode)} 配对; 供合并骨架调用, 使用方不应调用
     */
    public void pop() {
        stack.removeLast();
    }
}
