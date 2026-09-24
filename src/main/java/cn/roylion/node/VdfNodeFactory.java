package cn.roylion.node;

/**
 * VDF 节点的静态工厂
 *
 * @author liugenxin 2026/9/23
 */
public final class VdfNodeFactory {

    private VdfNodeFactory() {
    }

    /**
     * 构造一个虚拟根节点（key 为 null 的 {@link VdfVirtualNode}），用于挂载多个顶层节点
     */
    public static VdfVirtualNode virtual() {
        return new VdfVirtualNode();
    }

    /**
     * 构造容器节点
     */
    public static VdfObjectNode obj(String key) {
        return new VdfObjectNode(key);
    }

    /**
     * 构造叶子节点
     */
    public static VdfStringNode val(String key, String value) {
        return new VdfStringNode(key, value);
    }
}
