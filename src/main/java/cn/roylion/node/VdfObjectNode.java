package cn.roylion.node;

import cn.roylion.merge.VdfMergeStrategy;
import cn.roylion.merge.VdfMergeDecision;
import cn.roylion.merge.VdfMergeContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * VDF 容器节点：key + 子节点列表，对应 VDF 的 {@code key { ... }} 块。
 * <p>key 为 null 仅允许出现在 {@link VdfVirtualNode}（虚拟根）上。
 *
 * @author liugenxin 2026/9/23
 */
public class VdfObjectNode extends VdfNode {

    /**
     * 子节点列表; protected 供子类（如 VdfVirtualNode）访问, 外部通过 get/has/put 操作
     */
    protected final List<VdfNode> children = new ArrayList<>();

    public VdfObjectNode(String key) {
        super(key);
    }

    // ==================== 添加 ====================

    /**
     * 追加一个子节点（保留重复 key, get/has 查询时只取首个）
     */
    public void put(VdfNode node) {
        children.add(node);
    }

    /**
     * 追加一个文本子节点
     *
     * @throws NullPointerException value 为 null 时
     */
    public void put(String key, String value) {
        Objects.requireNonNull(value, "value 不能为 null");
        children.add(VdfNodeFactory.val(key, value));
    }

    // ==================== 子节点查询 ====================

    /**
     * 获取第一个子节点; 没有子节点时返回 null
     */
    @Override
    public VdfNode get() {
        if (children.isEmpty()) return null;
        return children.get(0);
    }

    /**
     * 按 key 查找子节点, 返回<b>全部</b>匹配的节点列表（按插入顺序, VDF 允许重复 key）；
     * 不存在时返回空列表
     */
    @Override
    public List<VdfNode> get(String propertyName) {
        List<VdfNode> result = new ArrayList<>();
        for (VdfNode child : children) {
            if (child.getKey().equals(propertyName)) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * 按 key 查找第一个匹配的子节点（重复 key 只取首个）；不存在返回 null
     */
    @Override
    public VdfNode getOne(String propertyName) {
        for (VdfNode child : children) {
            if (child.getKey().equals(propertyName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * 按 key 查找第一个匹配的子节点的 Optional 形式
     */
    @Override
    public Optional<VdfNode> getOneOptional(String propertyName) {
        return Optional.ofNullable(getOne(propertyName));
    }

    /**
     * 按 key 取出<b>全部</b>同名子节点, 合并成一个新容器返回。
     * <p>子类优先（后面的同名块只补齐前面缺失的属性）; 每次调用都重新合并, 无缓存。
     * 子节点先深拷贝再并入, 结果与原树互不影响。
     *
     * @throws IllegalStateException 同名子节点中存在叶子节点时
     */
    @Override
    public VdfNode mergeOne(String propertyName) {
        List<VdfNode> nodes = get(propertyName);
        if (nodes.isEmpty()) return null;
        VdfObjectNode result = VdfNodeFactory.obj(propertyName);
        for (VdfNode node : nodes) {
            if (!node.isObj()) {
                throw new IllegalStateException("子节点 '" + node.getKey() + "' 不是容器节点, 无法合并");
            }
            result.merge((VdfObjectNode) node.deepCopy(), VdfMergeStrategy.MERGE_OVERRIDE, new VdfMergeContext());
        }
        return result;
    }

    /**
     * {@link #mergeOne(String)} 的 Optional 形式
     */
    @Override
    public Optional<VdfNode> mergeOneOptional(String propertyName) {
        return Optional.ofNullable(mergeOne(propertyName));
    }

    @Override
    public boolean has(String propertyName) {
        return getOne(propertyName) != null;
    }

    // ==================== 宏操作 / 继承 ====================

    /**
     * include 语义：把 other（一份完整文档）的全部子节点追加到自己末尾（不合并）
     *
     * @throws IllegalArgumentException other 与 this 是同一个节点时
     */
    @Override
    public void include(VdfVirtualNode other) {
        Objects.requireNonNull(other, "other 不能为 null");
        if (other == this) {
            throw new IllegalArgumentException("不能把文档 include 到它自身");
        }
        other.children.forEach(this::put);
    }

    /**
     * base 语义（Valve 协议钉死, 不接受策略定制）：子类优先的递归合并——
     * 同 key 的容器节点递归合并子节点, 同 key 但类型不同（或都是叶子）时保留自己的,
     * other 独有的子节点追加到末尾。就地修改当前节点。
     *
     * @throws IllegalArgumentException other 与 this 是同一个节点时
     */
    @Override
    public void base(VdfVirtualNode other) {
        Objects.requireNonNull(other, "other 不能为 null");
        if (other == this) {
            throw new IllegalArgumentException("不能把文档 base 到它自身");
        }
        merge(other, VdfMergeStrategy.MERGE_OVERRIDE, new VdfMergeContext());
    }

    /**
     * 继承式合并（默认子类优先策略）：{@link #extend(VdfObjectNode, VdfMergeStrategy)} 的便捷形式
     */
    @Override
    public VdfObjectNode extend(VdfObjectNode parent) {
        return extend(parent, VdfMergeStrategy.MERGE_OVERRIDE);
    }

    /**
     * 继承式合并：递归继承 parent 的属性, 同名子节点的冲突由 strategy 决策
     * （忽略 / 覆盖 / 递归合并）, parent 独有的子节点继承进来。
     * <p>
     * 深拷贝语义：返回全新节点, 原树完全不受影响。
     * 合并双方的 condition 不参与继承; 被继承的子节点各自携带自己的 condition。
     * <p>
     * this == parent 时合法, 等价于 deepCopy()。
     *
     * @param parent   被继承的对象
     * @param strategy 冲突解决策略, 如 {@link VdfMergeStrategy#OVERRIDE}
     * @return 合并结果 新节点
     * @throws NullPointerException parent 或 strategy 为 null 时
     */
    @Override
    public VdfObjectNode extend(VdfObjectNode parent, VdfMergeStrategy strategy) {
        Objects.requireNonNull(parent, "parent 不能为 null");
        Objects.requireNonNull(strategy, "strategy 不能为 null");
        VdfObjectNode result = deepCopy();
        // 源也拷贝一份, 策略返回 INHERIT 时直接引用放入也不会污染原树
        result.merge(parent.deepCopy(), strategy, new VdfMergeContext());
        return result;
    }

    // ==================== 拷贝 / 输出 ====================

    @Override
    public VdfObjectNode deepCopy() {
        VdfObjectNode copy = createCopy();
        copy.setCondition(getCondition());
        for (VdfNode child : children) {
            copy.children.add(child.deepCopy());
        }
        return copy;
    }

    /**
     * 供子类重写以保持拷贝结果的类型（如虚拟根拷贝仍是虚拟根）
     */
    protected VdfObjectNode createCopy() {
        return new VdfObjectNode(getKey());
    }

    @Override
    public String toString() {
        return getKey();
    }

    // ==================== 私有辅助 ====================

    /**
     * 递归合并骨架：把 parent 的子节点并入当前节点,
     * 同名冲突由 strategy 决策, 冲突点随下钻压入 ctx 的节点栈。
     * <p>约定：parent 的子节点会被<b>按引用</b>放入当前节点（调用方负责保证不污染——
     * extend 已预先深拷贝双方, base 本身就是就地共享语义）。
     * <p>兜底规则：决策为 MERGE 但任一方不是容器时, 降级为 OVERRIDE（保留子类自己的）。
     */
    private void merge(VdfObjectNode target, VdfMergeStrategy strategy, VdfMergeContext ctx) {
        // 构建属性索引, 属性名称冲突时保留第一个子节点
        Map<String, VdfNode> childrenMap = children.stream()
                .collect(Collectors.toMap(VdfNode::getKey, Function.identity(), (a, b) -> a));

        for (VdfNode targetProp : target.children) {
            VdfNode sourceProp = childrenMap.get(targetProp.getKey());
            if (sourceProp == null) {
                // 独有属性: 继承进来
                childrenMap.put(targetProp.getKey(), targetProp);
                put(targetProp);
                continue;
            }
            // 冲突点入栈, 决策/递归结束后出栈
            ctx.push(sourceProp);
            try {
                VdfMergeDecision decision = strategy.resolve(sourceProp, targetProp, ctx);
                switch (decision) {
                    case PRESERVE:
                        break;
                    case MERGE:
                        if (sourceProp.isObj() && targetProp.isObj()) {
                            ((VdfObjectNode) sourceProp).merge((VdfObjectNode) targetProp, strategy, ctx);
                        }
                        break;
                    case OVERRIDE:
                        childrenMap.put(targetProp.getKey(), targetProp);
                        int index = children.indexOf(sourceProp);
                        children.set(index, targetProp);
                        break;
                }
            } finally {
                ctx.pop();
            }
        }
    }
}
