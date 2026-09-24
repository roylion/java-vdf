package cn.roylion.node;

import cn.roylion.condition.VdfCondition;
import cn.roylion.merge.VdfMergeStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * VDF 节点的抽象基类。
 * <p>
 * 两类具体节点：
 * <ul>
 *   <li>{@link VdfStringNode}：叶子节点，key = value；</li>
 *   <li>{@link VdfObjectNode}：容器节点，key + 子节点列表（对应 VDF 的 {@code key { ... }} 块）。</li>
 * </ul>
 * 可选能力均以"安全的默认实现"形式提供：查询类方法默认静默返回空，
 * 转换类方法默认抛 {@link UnsupportedOperationException}，由子类按需重写。
 *
 * @author liugenxin 2026/9/22 10:57
 */
public abstract class VdfNode {

    private final String key;
    private VdfCondition condition;

    protected VdfNode(String key) {
        this.key = key;
    }

    // ==================== 基本信息 ====================

    public String getKey() {
        return key;
    }

    public VdfCondition getCondition() {
        return condition;
    }

    public void setCondition(VdfCondition condition) {
        this.condition = condition;
    }

    /**
     * 是否为容器节点（等价于 {@code this instanceof VdfObjectNode}）
     */
    public boolean isObj() {
        return this instanceof VdfObjectNode;
    }

    /**
     * 是否为叶子节点（等价于 {@code this instanceof VdfStringNode}）
     */
    public boolean isValue() {
        return this instanceof VdfStringNode;
    }

    /**
     * 是否为虚拟根节点（等价于 {@code this instanceof VdfVirtualNode}）
     */
    public boolean isVirtual() {
        return this instanceof VdfVirtualNode;
    }

    // ==================== 子节点查询（默认静默, 容器节点重写） ====================

    /**
     * 获取第一个子节点; 不是容器或没有子节点时返回 null
     */
    public VdfNode get() {
        return null;
    }

    /**
     * 按 key 查找子节点, 返回<b>全部</b>匹配的节点列表（VDF 允许重复 key）；
     * 不存在或当前节点不是容器时返回空列表
     */
    public List<VdfNode> get(String propertyName) {
        return Collections.emptyList();
    }

    /**
     * 按 key 查找第一个匹配的子节点（重复 key 只取首个）；
     * 不存在或当前节点不是容器时返回 null
     */
    public VdfNode getOne(String propertyName) {
        return null;
    }

    /**
     * 按 key 查找第一个匹配的子节点的 Optional 形式；
     * 不存在或当前节点不是容器时返回 {@link Optional#empty()}
     */
    public Optional<VdfNode> getOneOptional(String propertyName) {
        return Optional.empty();
    }

    /**
     * 按 key 取出<b>全部</b>同名子节点, 合并成一个新容器返回（子类优先, 不修改原树）;
     * 不存在匹配的子节点时返回 null
     *
     * @throws IllegalStateException 同名子节点中存在叶子节点时
     */
    public VdfNode mergeOne(String propertyName) {
        return null;
    }

    /**
     * {@link #mergeOne(String)} 的 Optional 形式
     */
    public Optional<VdfNode> mergeOneOptional(String propertyName) {
        return Optional.empty();
    }

    /**
     * 是否存在指定 key 的子节点；当前节点不是容器时返回 false
     */
    public boolean has(String propertyName) {
        return false;
    }

    // ==================== 宏操作 / 继承（默认静默或抛异常, 容器节点重写） ====================

    /**
     * include 宏指令
     */
    public void include(VdfVirtualNode other) {
    }

    /**
     * base 宏指令
     */
    public void base(VdfVirtualNode other) {
    }

    /**
     * extend 父节点属性（默认子类优先策略）
     */
    public VdfObjectNode extend(VdfObjectNode parent) {
        throw unsupported("extend");
    }

    /**
     * extend 父节点属性, 指定冲突解决策略
     */
    public VdfObjectNode extend(VdfObjectNode parent, VdfMergeStrategy strategy) {
        throw unsupported("extend");
    }

    // ==================== 类型转换（默认不支持, 叶子节点重写） ====================

    /**
     * 以文本形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public String asText() {
        throw unsupported("asText");
    }

    /**
     * 以 byte 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public byte asByte() {
        throw unsupported("asByte");
    }

    /**
     * 以 short 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public short asShort() {
        throw unsupported("asShort");
    }

    /**
     * 以 int 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public int asInteger() {
        throw unsupported("asInteger");
    }

    /**
     * 以 long 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public long asLong() {
        throw unsupported("asLong");
    }

    /**
     * 以 float 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public float asFloat() {
        throw unsupported("asFloat");
    }

    /**
     * 以 double 形式获取节点值
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public double asDouble() {
        throw unsupported("asDouble");
    }

    /**
     * 以 boolean 形式获取节点值, 接受 "1"/"true" 为真, "0"/"false" 为假（VDF 惯例）
     *
     * @throws UnsupportedOperationException 当前节点不支持该转换时
     */
    public boolean asBoolean() {
        throw unsupported("asBoolean");
    }

    // ==================== 拷贝 ====================

    /**
     * 深拷贝当前节点及其整棵子树, 返回的节点与原树不共享任何内部引用,
     * 对任一棵的修改不会影响另一棵。
     * <p>condition 为不可变对象, 拷贝的是引用（共享安全）。
     */
    public abstract VdfNode deepCopy();

    // ==================== 私有辅助 ====================

    private UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException(
                getClass().getSimpleName() + " 节点不支持 " + method + "(), key=" + getKey());
    }
}
