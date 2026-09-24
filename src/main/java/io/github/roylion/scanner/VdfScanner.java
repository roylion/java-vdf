package io.github.roylion.scanner;

/**
 * @author liugenxin 2026/9/21 17:04
 */
// @formatter:off
public interface VdfScanner {

    /**
     * 读取下一个字符，到达流末尾时返回 -1
     */
    int readChar();

    /**
     * 查看下一个字符但不消费它，流末尾时返回 -1
     */
    int peekChar();

    /**
     * 回退最近一次 {@link #readChar()} 返回的字符，行号、列号、位置一并回退。
     * <p>依赖快照历史而非数据源本身，流式读取同样可用；可以连续回退多次，
     * 上限为实现保留的快照深度（{@link DefaultVdfScanner} 为缓冲区容量减一），
     * 超出后或尚未消费过字符时抛出 {@link IllegalStateException}。
     */
    void unreadChar();

    /**
     * 获取上一个字符（倒数第二个被消费的字符；最近消费的是第 readPos 号，"上一个"是第 readPos-1 号）。
     * 消费不足 2 个字符或对应快照已被环形覆盖时返回 -1。
     */
    int prevChar();

    /**
     * 获取当前行号（从 0 开始）
     */
    long getLine();
    /**
     * 获取当前列号（从 0 开始）
     */
    long getCol();
    /**
     * 获取当前字符位置（已消费的字符数）
     */
    long getPos();
}
