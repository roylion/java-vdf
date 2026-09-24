package io.github.roylion.token;

/**
 * VDF词元
 * <p>
 * Valve 官方文档对 KeyValues 格式的定义要点：
 * 控制字符只有三个：{、}、"
 * 名称和值可以带引号，也可以不带
 * 引号内不能直接包含 "，需要用转义序列 \"
 * 转义序列默认关闭，需要解析器主动调用 UsesEscapeSequences(true) 才启用
 * 非引号包裹的 token 以空白、{、}、" 作为结束符
 * 键名后的 { 表示子键列表的开始，} 结束
 * 空白字符包括：空格、回车、换行、Tab
 * 支持的转义序列：\n、\t、\\、\"
 * # 作为行首字符时用于宏，不要用在键名的开头
 *
 * @author liugenxin 2026/9/21 15:30
 */
public class VdfToken {
    /**
     * 词元类型
     */
    private VdfTokenType type;
    /**
     * 词元值
     */
    private String value;

    private long line;
    private long col;
    private long pos;

    public VdfToken(VdfTokenType type, long line, long col, long pos) {
        this.type = type;
        this.line = line;
        this.col = col;
        this.pos = pos;
    }

    public VdfToken(VdfTokenType type, String value, long line, long col, long pos) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.col = col;
        this.pos = pos;
    }

    public VdfTokenType getType() {
        return type;
    }

    public void setType(VdfTokenType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getLine() {
        return line;
    }

    public void setLine(long line) {
        this.line = line;
    }

    public long getCol() {
        return col;
    }

    public void setCol(long col) {
        this.col = col;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "VdfToken{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
