package io.github.roylion.parse;

import io.github.roylion.VdfConfig;
import io.github.roylion.condition.VdfCondition;
import io.github.roylion.condition.VdfConditionParser;
import io.github.roylion.node.VdfNode;
import io.github.roylion.node.VdfNodeFactory;
import io.github.roylion.node.VdfObjectNode;
import io.github.roylion.node.VdfVirtualNode;
import io.github.roylion.token.VdfToken;
import io.github.roylion.token.VdfTokenizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author liugenxin 2026/9/22 11:02
 */
public class DefaultVdfParser implements VdfParser {

    private final VdfTokenizer tokenizer;
    private final VdfConfig config;
    private final Stack<VdfObjectNode> stack = new Stack<>();
    private final List<VdfToken> macros = new ArrayList<>();
    private VdfToken key;

    /**
     * 使用默认配置（宏相对路径基于工作目录）
     */
    public DefaultVdfParser(VdfTokenizer tokenizer) {
        this(tokenizer, VdfConfig.defaults());
    }

    /**
     * @param config 解析管线配置（含宏资源解析器, 相对路径基准由它自行管理）;
     *               处理宏时复用同一份, 嵌套文件行为与主文件一致
     */
    public DefaultVdfParser(VdfTokenizer tokenizer, VdfConfig config) {
        this.tokenizer = tokenizer;
        this.config = config;
    }

    @Override
    public VdfVirtualNode parse() {
        VdfVirtualNode virtual = VdfNodeFactory.virtual();
        stack.push(virtual);
        VdfNode last = null;
        boolean loop = true;
        while (loop) {
            VdfToken next = tokenizer.next();
            switch (next.getType()) {
                case QUOTED_STRING:
                case UNQUOTED_STRING:
                    if (key == null) {
                        key = next;
                    } else {
                        VdfObjectNode parent = peek();
                        if (parent != null) {
                            last = VdfNodeFactory.val(key.getValue(), next.getValue());
                            parent.put(last);
                        }
                        key = null;
                    }
                    break;
                case LBRACE:
                    if (key == null) {
                        throw VdfParseException.atToken("'{', 前面缺少键名", next);
                    }
                    VdfObjectNode parent = peek();
                    VdfObjectNode obj = push(key.getValue());
                    if (parent != null) {
                        parent.put(obj);
                    }
                    key = null;
                    break;
                case RBRACE:
                    if (stack.size() <= 1) {
                        throw VdfParseException.atToken("多余的 '}', 没有与之匹配的 '{'", next);
                    }
                    last = pop();
                    break;
                case EOF:
                    if (key != null) {
                        throw VdfParseException.atToken("文件结束但键 '" + key.getValue() + "' 没有对应的值或 '{'", key);
                    }
                    if (stack.size() > 1) {
                        throw VdfParseException.atToken("文件结束但还有 " + (stack.size() - 1) + " 个 '{' 未闭合", next);
                    }
                    loop = false;
                    break;
                case COMMENT:
                    break;
                case CONDITION:
                    VdfCondition condition = VdfConditionParser.parse(next.getValue());
                    if (last != null) {
                        last.setCondition(condition);
                    }
                    break;
                case MACRO:
                    macros.add(next);
                    break;
                default:
                    throw VdfParseException.atToken("无法处理的词元: " + next, next);
            }
        }

        for (VdfToken macro : macros) {
            String[] tmp = macro.getValue().trim().split("\\s+", 2);
            String cmd = tmp[0];
            if (tmp.length < 2 || tmp[1].trim().isEmpty()) {
                throw VdfParseException.atToken("宏 #" + cmd + " 缺少参数", macro);
            }
            String param = tmp[1].trim().replace("\"", "");
            switch (cmd) {
                case "include":
                    include(virtual, param);
                    break;
                case "base":
                    base(virtual, param);
                    break;
                default:
                    throw VdfParseException.atToken("不支持的宏: #" + cmd, macro);
            }
        }
        return virtual;
    }

    private void include(VdfVirtualNode root, String param) {
        VdfVirtualNode doc = parseMacro(param, "#include");
        root.include(doc);
    }

    private void base(VdfVirtualNode root, String param) {
        VdfVirtualNode doc = parseMacro(param, "#base");
        root.base(doc);
    }

    /**
     * 打开宏引用的资源, 按当前 config 组装子解析管线（字符集/转义/工厂与主文件一致）。
     * 相对路径基准由 resolver 自行管理（以当前解析文件所在目录为基准）, parser 不感知。
     */
    private VdfVirtualNode parseMacro(String param, String macroName) {
        try (InputStream stream = config.resolver().open(param)) {
            return config.parser(stream).parse();
        } catch (FileNotFoundException | NoSuchFileException e) {
            throw new VdfParseException(macroName + " 引用的文件不存在: " + param, e);
        } catch (IOException e) {
            throw new VdfParseException(macroName + " 读取失败: " + param, e);
        }
    }

    private VdfObjectNode push(String key) {
        VdfObjectNode obj = VdfNodeFactory.obj(key);
        stack.push(obj);
        return obj;
    }

    private VdfObjectNode pop() {
        return stack.pop();
    }

    private VdfObjectNode peek() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }
}
