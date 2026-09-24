package io.github.roylion.scanner;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 基于流的字符扫描器，支持 peek（预读）和 unread（回退），数据源不需要随机访问。
 *
 * <h2>核心设计</h2>
 * 所有读过的字符连同位置信息以 {@link CharSnapshot} 快照的形式存进一个环形数组：
 * <ul>
 *   <li><b>第 k 个字符的快照存在 {@code buffer[k & mask]}</b>（k 从 1 开始，下标 0 的槽位不使用）；
 *       快照里记录的是该字符<b>消费之后</b>的行号、列号。</li>
 *   <li>{@code bufferWritePos}：已写入的快照数（只增）。每次从流里读出一个新字符，写入第
 *       {@code writePos + 1} 号槽位后自增。EOF（-1）也会占用一个快照，用于重复返回 -1。</li>
 *   <li>{@code bufferReadPos}：已消费的字符数（可增可减）。{@code readChar} 消费成功后自增，
 *       {@code unreadChar} 自减。</li>
 * </ul>
 * 由这两个指针可以推出所有状态：
 * <ul>
 *   <li>{@code readPos == writePos}：没有未消费的快照，下次 read/peek 需要从流里读；</li>
 *   <li>{@code readPos < writePos}：有未消费的快照（peek 预读或 unread 回退产生的），
 *       read/peek 直接取 {@code buffer[readPos + 1]}，不再碰流；</li>
 *   <li>{@code readPos == 0}：还没有消费过任何字符。</li>
 * </ul>
 *
 * <h2>几个刻意的实现选择</h2>
 * <ul>
 *   <li><b>指针是 long 且只依赖差值</b>：long 不会溢出（需要 9.2×10¹⁸ 次读取），因此
 *       "readPos == 0 就是没有消费过" 永远成立；所有合法性判断都是指针差值，不做绝对大小比较。</li>
 *   <li><b>容量是 2 的幂，索引用 {@code & mask}</b>：等价于取模但更快，且对负数也正确
 *       （虽然本实现中指针不会为负，这只是双保险）。</li>
 *   <li><b>回退深度上限是 {@code buffer.length - 1}</b> 而不是 {@code buffer.length}：
 *       getLine/getCol/prevChar 需要读"最近消费的字符"（第 readPos 号快照），它必须还在环里，
 *       所以窗口 [readPos, writePos] 的长度最多是 {@code length - 1}。</li>
 *   <li><b>下标 0 的槽位永远为 null</b>：快照从 1 开始编号，所有按 readPos 取快照的读操作
 *       都必须先排除 {@code readPos == 0}。</li>
 * </ul>
 *
 * @author liugenxin 2026/9/21 17:08
 */
public class DefaultVdfScanner implements VdfScanner {

    private final Reader reader;

    /**
     * 是否已从流里读出过至少一个字符（含 EOF），用于 writeBuffer 计算初始行列
     */
    private boolean hasReadStream = false;
    /**
     * 流是否已读到末尾（EOF 快照已写入）
     */
    private boolean eof = false;
    /**
     * 环形数组容量掩码，buffer.length 必须是 2 的幂
     */
    private final int mask;
    /**
     * 快照环形数组，第 k 个字符的快照在 buffer[k & mask]
     */
    private final CharSnapshot[] buffer;
    /**
     * 读指针：已消费的字符数
     */
    private long bufferReadPos;
    /**
     * 写指针：已写入的快照数（只增）
     */
    private long bufferWritePos;

    public DefaultVdfScanner(InputStream stream) {
        this(stream, 1024, StandardCharsets.UTF_8);
    }

    public DefaultVdfScanner(InputStream stream, int bufferSize) {
        this(stream, bufferSize, StandardCharsets.UTF_8);
    }

    public DefaultVdfScanner(InputStream stream, Charset charset) {
        this(stream, 1024, charset);
    }

    public DefaultVdfScanner(InputStream stream, int bufferSize, Charset charset) {
        this(new InputStreamReader(stream, charset), bufferSize);
    }

    public DefaultVdfScanner(Reader reader, int bufferSize) {
        if (bufferSize < 1 || bufferSize > (1 << 30)) {
            throw new IllegalArgumentException("bufferSize 非法: " + bufferSize + ", 允许范围 [1, 2^30]");
        }
        this.reader = reader;
        this.buffer = new CharSnapshot[calcBufferSize(bufferSize)];
        this.mask = buffer.length - 1;
    }

    // ==================== 读取 ====================

    @Override
    public int readChar() {
        // readPos == writePos 说明没有未消费的快照, 需要从流里读一个（EOF 后 writeBuffer 内部会短路）
        if (bufferReadPos == bufferWritePos) {
            writeBuffer();
        }
        // EOF 快照已被消费过（readPos 停在 writePos）时不推进读指针, 保证 EOF 可重复读取且 pos 不再增长
        if (bufferReadPos < bufferWritePos) {
            bufferReadPos++;
        }
        return buffer[(int) (bufferReadPos & mask)].c;
    }

    @Override
    public int peekChar() {
        if (bufferReadPos == bufferWritePos) {
            if (eof) return -1;
            writeBuffer();
        }
        // 下一个待消费的快照是第 readPos + 1 号
        return buffer[(int) ((bufferReadPos + 1) & mask)].c;
    }

    @Override
    public void unreadChar() {
        // readPos == 0: 还没消费过任何字符（含只 peek 过的情况）
        if (bufferReadPos == 0) {
            throw new IllegalStateException("没有可回退的字符（尚未消费任何字符）");
        }
        // 再回退一格, 最早的有效快照（第 readPos 号, 供 getLine/prevChar 使用）就要被挤掉了
        if (bufferWritePos - bufferReadPos >= buffer.length - 1) {
            throw new IllegalStateException("没有可回退的字符（超出快照历史上限 " + (buffer.length - 1) + "）");
        }
        bufferReadPos--;
    }

    @Override
    public int prevChar() {
        // 至少消费过 2 个字符, 才存在"上一个字符"
        if (bufferReadPos < 2) return -1;
        // 窗口占满时, 第 readPos-1 号快照已被覆盖, 无从得知
        if (bufferWritePos - bufferReadPos >= buffer.length - 1) return -1;
        // 最近消费的是第 readPos 号, "上一个"就是第 readPos-1 号
        return buffer[(int) ((bufferReadPos - 1) & mask)].c;
    }

    // ==================== 位置查询 ====================

    @Override
    public long getLine() {
        if (bufferReadPos == 0) return 0;
        // 快照记录的是该字符消费之后的行列
        return buffer[(int) (bufferReadPos & mask)].line;
    }

    @Override
    public long getCol() {
        if (bufferReadPos == 0) return 0;
        return buffer[(int) (bufferReadPos & mask)].col;
    }

    @Override
    public long getPos() {
        return bufferReadPos;
    }

    // ==================== 私有实现 ====================

    /**
     * 向上取整到 2 的幂（HashMap#tableSizeFor 同款位运算）：
     * 5 次移位或把最高位 1 右侧的位全部涂成 1, 再 +1 进位。
     */
    private int calcBufferSize(int size) {
        int n = size - 1;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return (n < 0) ? 1 : n + 1;
    }

    /**
     * 从流里读出下一个字符, 连同行列信息写入第 writePos + 1 号快照槽位。
     * 行列基于上一个快照推导：普通字符列 +1；换行符行 +1、列归 0；EOF 不改变行列。
     * 已读到末尾时是空操作。
     */
    private void writeBuffer() {
        if (eof) return;

        int c;
        try {
            c = reader.read();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        long prevLine = 0;
        long prevCol = 0;
        if (hasReadStream) {
            CharSnapshot prev = buffer[(int) (bufferWritePos & mask)];
            prevLine = prev.line;
            prevCol = prev.col;
        }
        CharSnapshot newChar = new CharSnapshot();
        newChar.line = prevLine;
        newChar.col = prevCol + 1;
        newChar.c = c;
        if (c == -1) {
            eof = true;
        } else if (c == '\n') {
            newChar.line = prevLine + 1;
            newChar.col = 0;
        }
        bufferWritePos++;
        buffer[(int) (bufferWritePos & mask)] = newChar;
        hasReadStream = true;
    }

    /**
     * 单个字符的快照：字符本身 + 消费它之后的行号、列号
     */
    private static class CharSnapshot {
        int c;
        long line;
        long col;
    }

    // ==================== 手动验证 ====================

    public static void main(String[] args) {
        VdfScanner scanner = new DefaultVdfScanner(new ByteArrayInputStream("abcdefg".getBytes(StandardCharsets.UTF_8)));
        System.out.println((char) scanner.readChar()); // a
        System.out.println((char) scanner.readChar()); // b
        System.out.println((char) scanner.readChar()); // c
        System.out.println((char) scanner.prevChar()); // b
    }
}
