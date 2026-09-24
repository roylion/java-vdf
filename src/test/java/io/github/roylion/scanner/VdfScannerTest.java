package io.github.roylion.scanner;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author liugenxin 2026/9/22
 */
public class VdfScannerTest extends TestCase {

    private static DefaultVdfScanner scannerOf(String text) {
        return new DefaultVdfScanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    public void testUnreadRestoresPosLineCol() {
        DefaultVdfScanner scanner = scannerOf("ab\nc");

        assertEquals('a', scanner.readChar());
        assertEquals('b', scanner.readChar());
        assertEquals('\n', scanner.readChar());
        // 此时位于第2行开头（行号从0计）
        assertEquals(1, scanner.getLine());
        assertEquals(0, scanner.getCol());

        assertEquals('c', scanner.readChar());
        long line = scanner.getLine();
        long col = scanner.getCol();
        long pos = scanner.getPos();

        scanner.unreadChar();
        // 回退后行、列、位置应与读取前一致
        assertEquals(1, scanner.getLine());
        assertEquals(0, scanner.getCol());
        assertEquals(pos - 1, scanner.getPos());

        // 回退后再读，应得到同一个字符、同样的位置
        assertEquals('c', scanner.readChar());
        assertEquals(line, scanner.getLine());
        assertEquals(col, scanner.getCol());
        assertEquals(pos, scanner.getPos());
    }

    public void testPrevChar() {
        DefaultVdfScanner scanner = scannerOf("ab");
        assertEquals(-1, scanner.prevChar());

        // 只消费 1 个字符, 没有上一个
        assertEquals('a', scanner.readChar());
        assertEquals(-1, scanner.prevChar());

        // 读完 a、b, 上一个字符是 a
        assertEquals('b', scanner.readChar());
        assertEquals('a', scanner.prevChar());

        // 回退掉 b 后只剩 a 被消费, 又没有上一个了
        scanner.unreadChar();
        assertEquals(-1, scanner.prevChar());
    }

    public void testPeekOnlyDoesNotNpe() {
        DefaultVdfScanner scanner = scannerOf("ab");
        assertEquals('a', scanner.peekChar());
        // 只 peek 没消费过, 行列/前一个字符应返回初始值而不是 NPE
        assertEquals(0, scanner.getLine());
        assertEquals(0, scanner.getCol());
        assertEquals(-1, scanner.prevChar());
    }

    public void testPeekDoesNotConsume() {
        DefaultVdfScanner scanner = scannerOf("ab");

        assertEquals('a', scanner.peekChar());
        assertEquals('a', scanner.peekChar());
        assertEquals('a', scanner.readChar());
        assertEquals('b', scanner.readChar());
        assertEquals(-1, scanner.peekChar());
        assertEquals(-1, scanner.readChar());
        // EOF 之后重复读取应稳定返回 -1
        assertEquals(-1, scanner.readChar());
    }

    public void testPeekThenUnreadWorks() {
        DefaultVdfScanner scanner = scannerOf("abc");
        scanner.readChar();          // 'a'
        assertEquals('b', scanner.peekChar());
        scanner.unreadChar();        // 回退 'a', peek 的预读不受影响
        // 重新读到的顺序应为 a, b, c
        assertEquals('a', scanner.readChar());
        assertEquals('b', scanner.readChar());
        assertEquals('c', scanner.readChar());
        assertEquals(-1, scanner.readChar());
    }

    public void testUnreadMultiple() {
        DefaultVdfScanner scanner = scannerOf("abc\n");
        assertEquals('a', scanner.readChar());
        assertEquals('b', scanner.readChar());
        assertEquals('c', scanner.readChar());
        long line = scanner.getLine();
        long col = scanner.getCol();

        scanner.unreadChar();
        scanner.unreadChar();
        scanner.unreadChar();
        assertEquals(0, scanner.getPos());
        assertEquals(0, scanner.getLine());
        assertEquals(0, scanner.getCol());

        // 全部回退后重读, 字符与位置完全一致
        assertEquals('a', scanner.readChar());
        assertEquals('b', scanner.readChar());
        assertEquals('c', scanner.readChar());
        assertEquals(line, scanner.getLine());
        assertEquals(col, scanner.getCol());
    }

    public void testUnreadBeyondHistoryThrows() {
        // 容量 4 向上取整仍为 4, 回退深度上限为 3
        DefaultVdfScanner scanner = new DefaultVdfScanner(
                new ByteArrayInputStream("abcdefg".getBytes(StandardCharsets.UTF_8)), 4);
        for (int i = 0; i < 4; i++) {
            scanner.readChar();
        }
        for (int i = 0; i < 3; i++) {
            scanner.unreadChar();
        }
        try {
            scanner.unreadChar();
            fail("超出快照历史上限的回退应抛出 IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    public void testUnreadWithoutHistoryThrows() {
        DefaultVdfScanner scanner = scannerOf("ab");
        try {
            scanner.unreadChar();
            fail("未读取时回退应抛出 IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    public void testUtf8MultibyteChars() {
        // 中文字符是 3 字节 UTF-8, 不应被拆成乱码字节
        DefaultVdfScanner scanner = scannerOf("中a");
        assertEquals('中', scanner.readChar());
        assertEquals('a', scanner.readChar());
        assertEquals(-1, scanner.readChar());
    }

    public void testInvalidBufferSizeThrows() {
        try {
            new DefaultVdfScanner(new ByteArrayInputStream(new byte[0]), 0);
            fail("非法 bufferSize 应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new DefaultVdfScanner(new ByteArrayInputStream(new byte[0]), (1 << 30) + 1);
            fail("非法 bufferSize 应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCustomCharset() {
        // GBK 的 "中" 是 2 字节, 和 UTF-8 的 3 字节不同, 可验证字符集确实生效
        DefaultVdfScanner scanner = new DefaultVdfScanner(
                new ByteArrayInputStream("中".getBytes(Charset.forName("GBK"))),
                Charset.forName("GBK"));
        assertEquals('中', scanner.readChar());
        assertEquals(-1, scanner.readChar());
    }

}
