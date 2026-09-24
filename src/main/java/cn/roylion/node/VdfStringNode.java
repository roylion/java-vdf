package cn.roylion.node;

/**
 * VDF 叶子节点：key = value
 *
 * @author liugenxin 2026/9/23
 */
public class VdfStringNode extends VdfNode {

    private final String value;

    public VdfStringNode(String key, String value) {
        super(key);
        this.value = value;
    }

    // ==================== 基本信息 ====================

    public String getValue() {
        return value;
    }

    // ==================== 类型转换 ====================

    @Override
    public String asText() {
        return value;
    }

    @Override
    public byte asByte() {
        return Byte.parseByte(value.trim());
    }

    @Override
    public short asShort() {
        return Short.parseShort(value.trim());
    }

    @Override
    public int asInteger() {
        return Integer.parseInt(value.trim());
    }

    @Override
    public long asLong() {
        return Long.parseLong(value.trim());
    }

    @Override
    public float asFloat() {
        return Float.parseFloat(value.trim());
    }

    @Override
    public double asDouble() {
        return Double.parseDouble(value.trim());
    }

    /**
     * 接受 "1"/"true" 为真, "0"/"false" 为假（不区分大小写）, 其余值抛 IllegalArgumentException
     */
    @Override
    public boolean asBoolean() {
        String v = value.trim().toLowerCase();
        if ("1".equals(v) || "true".equals(v)) return true;
        if ("0".equals(v) || "false".equals(v)) return false;
        throw new IllegalArgumentException("无法转为 boolean: \"" + value + "\", key=" + getKey());
    }

    // ==================== 拷贝 / 输出 ====================

    @Override
    public VdfStringNode deepCopy() {
        VdfStringNode copy = new VdfStringNode(getKey(), value);
        copy.setCondition(getCondition());
        return copy;
    }

    @Override
    public String toString() {
        return getKey() + " = " + value;
    }
}
