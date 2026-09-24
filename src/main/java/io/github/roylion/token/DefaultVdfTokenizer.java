package io.github.roylion.token;

import io.github.roylion.scanner.DefaultVdfScanner;
import io.github.roylion.scanner.VdfScanner;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author liugenxin 2026/9/21 16:14
 */
public class DefaultVdfTokenizer implements VdfTokenizer {

    private final VdfScanner scanner;
    private final boolean usesEscapeSequences; // 是否使用转义字符
    private State state = State.INIT;

    private VdfToken current;
    private VdfToken pending;
    private StringBuilder buffers;

    public DefaultVdfTokenizer(InputStream stream) {
        this.scanner = new DefaultVdfScanner(stream);
        this.usesEscapeSequences = false;
    }

    public DefaultVdfTokenizer(InputStream stream, Charset charset) {
        this.scanner = new DefaultVdfScanner(stream, charset);
        this.usesEscapeSequences = false;
    }

    public DefaultVdfTokenizer(VdfScanner scanner) {
        this.scanner = scanner;
        this.usesEscapeSequences = false;
    }

    public DefaultVdfTokenizer(VdfScanner scanner, boolean usesEscapeSequences) {
        this.scanner = scanner;
        this.usesEscapeSequences = usesEscapeSequences;
    }

    @Override
    public VdfToken next() {
        current = null;
        if (pending != null) {
            VdfToken temp = pending;
            pending = null;
            return temp;
        }

        while (true) {
            switch (state) {
                case INIT:
                    handleInit();
                    break;
                case IN_QUOTE:
                    handleInQuote();
                    break;
                case IN_ESCAPE:
                    handleEscape();
                    break;
                case IN_UNQUOTED:
                    handleUnquotedString();
                    break;
                case IN_COMMENT:
                    handleComment();
                    break;
                case IN_CONDITION:
                    handleCondition();
                    break;
                case IN_MACRO:
                    handleMacro();
                    break;
            }
            if (current != null) {
                return current;
            }
        }
    }

    private void handleInit() {
        int c = scanner.readChar();
        if (c == -1) {
            current = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            return;
        }
        switch (c) {
            case ' ':
            case '\r':
            case '\n':
            case '\t':
            case '\uFEFF':   // UTF-8 BOM, 跳过（Valve 本地化文件常见）
                break;
            case '"':
                state = State.IN_QUOTE;
                buffers = new StringBuilder();
                break;
            case '{':
                current = new VdfToken(VdfTokenType.LBRACE, scanner.getLine(), scanner.getCol(), scanner.getPos());
                return;
            case '}':
                current = new VdfToken(VdfTokenType.RBRACE, scanner.getLine(), scanner.getCol(), scanner.getPos());
                return;
            case '/':
                if (scanner.readChar() == '/') {
                    state = State.IN_COMMENT;
                    buffers = new StringBuilder();
                } else {
                    scanner.unreadChar();
                    state = State.IN_UNQUOTED;
                    buffers = new StringBuilder();
                    buffers.append((char) c);
                }
                break;
            case '[':
                state = State.IN_CONDITION;
                buffers = new StringBuilder();
                break;
            case '#':
                state = State.IN_MACRO;
                buffers = new StringBuilder();
                break;
            default:
                state = State.IN_UNQUOTED;
                buffers = new StringBuilder();
                buffers.append((char) c);
        }
    }

    private void handleInQuote() {
        int c = scanner.readChar();
        if (c == -1) {
            String value = buffers.toString();
            current = new VdfToken(VdfTokenType.QUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
            pending = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            state = State.INIT;
            return;
        }
        switch (c) {
            case '"':
                state = State.INIT;
                String value = buffers.toString();
                current = new VdfToken(VdfTokenType.QUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                break;
            case '\\':
                if (usesEscapeSequences) {
                    state = State.IN_ESCAPE;
                } else {
                    // 转义序列关闭时, \" 和 \\ 仍然必须识别——
                    // 否则 value 里的转义引号会被当成字符串结束符, 文件无法正确分词
                    // (Valve 官方文件大量使用 \" 包裹富文本, 如 csgo_english.txt)
                    int next = scanner.readChar();
                    if (next == '"' || next == '\\') {
                        buffers.append((char) next);
                    } else {
                        scanner.unreadChar();
                        buffers.append('\\');
                    }
                }
                break;
            default:
                buffers.append((char) c);
        }
    }

    private void handleEscape() {
        int c = scanner.readChar();
        if (c == -1) {
            // 文件结束，转义符没有后续字符，按字面量处理
            buffers.append('\\');
            state = State.IN_QUOTE;
            return;
        }
        switch (c) {
            case 'n':
                buffers.append('\n');
                break;
            case 'r':
                buffers.append('\r');
                break;
            case 't':
                buffers.append('\t');
                break;
            case '\\':
                buffers.append('\\');
                break;
            case '"':
                buffers.append('"');
                break;
            default:
                buffers.append("\\");
                buffers.append((char) c);
        }
        state = State.IN_QUOTE;
    }

    private void handleUnquotedString() {
        int c = scanner.readChar();
        if (c == -1) {
            String value = buffers.toString();
            current = new VdfToken(VdfTokenType.UNQUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
            pending = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            state = State.INIT;
            return;
        }
        switch (c) {
            case ' ':
            case '\r':
            case '\n':
            case '\t':
                String value = buffers.toString();
                current = new VdfToken(VdfTokenType.UNQUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            case '{':
                value = buffers.toString();
                current = new VdfToken(VdfTokenType.UNQUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                pending = new VdfToken(VdfTokenType.LBRACE, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            case '}':
                value = buffers.toString();
                current = new VdfToken(VdfTokenType.UNQUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                pending = new VdfToken(VdfTokenType.RBRACE, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            case '"':
                value = buffers.toString();
                current = new VdfToken(VdfTokenType.UNQUOTED_STRING, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.IN_QUOTE;
                buffers = new StringBuilder();
                break;
            default:
                buffers.append((char) c);
        }
    }

    private void handleComment() {
        int c = scanner.readChar();
        if (c == -1) {
            String value = buffers.toString();
            current = new VdfToken(VdfTokenType.COMMENT, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
            pending = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            state = State.INIT;
            return;
        }
        switch (c) {
            case '\n':
                String value = buffers.toString();
                current = new VdfToken(VdfTokenType.COMMENT, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            default:
                buffers.append((char) c);
        }
    }

    private void handleCondition() {
        int c = scanner.readChar();
        if (c == -1) {
            String value = buffers.toString();
            current = new VdfToken(VdfTokenType.CONDITION, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
            pending = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            state = State.INIT;
            return;
        }
        switch (c) {
            case ']':
                String value = buffers.toString();
                current = new VdfToken(VdfTokenType.CONDITION, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            default:
                buffers.append((char) c);
        }
    }

    private void handleMacro() {
        int c = scanner.readChar();
        if (c == -1) {
            String value = buffers.toString();
            current = new VdfToken(VdfTokenType.MACRO, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
            pending = new VdfToken(VdfTokenType.EOF, scanner.getLine(), scanner.getCol(), scanner.getPos());
            state = State.INIT;
            return;
        }
        switch (c) {
            case '\n':
                String value = buffers.toString();
                current = new VdfToken(VdfTokenType.MACRO, value, scanner.getLine(), scanner.getCol(), scanner.getPos());
                state = State.INIT;
                break;
            default:
                buffers.append((char) c);
        }
    }

    private enum State {
        INIT,
        IN_QUOTE,
        IN_ESCAPE,
        IN_UNQUOTED,
        IN_COMMENT,
        IN_CONDITION,
        IN_MACRO,
    }

}
