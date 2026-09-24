package io.github.roylion.node;

/**
 * 虚拟根节点：不对应文档中的任何块, 仅用于挂载多个顶层节点。
 * <p>
 * key 为 null（不占用任何 key, 与文档中的真实节点不会碰撞）；
 * 序列化时应跳过自身, 只输出其子节点。构造器包私有, 只能通过
 * {@link VdfNodeFactory#virtual()} 创建。
 *
 * @author liugenxin 2026/9/23
 */
public final class VdfVirtualNode extends VdfObjectNode {

    VdfVirtualNode() {
        super(null);
    }

    @Override
    protected VdfObjectNode createCopy() {
        return new VdfVirtualNode();
    }

    @Override
    public String toString() {
        return "_virtual_root";
    }
}
