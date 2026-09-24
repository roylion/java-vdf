package io.github.roylion;

import io.github.roylion.node.VdfVirtualNode;
import io.github.roylion.parse.VdfParseException;

import java.io.IOException;
import java.io.InputStream;

/**
 * VDF 解析门面：持有 {@link VdfConfig}, 每次 {@link #parse(InputStream)} 按配置
 * 组装 scanner → tokenizer → parser 三层管线并解析。
 * <p>实例可复用（对多个流反复 parse）; 配置不可变, 需要变更时通过
 * {@code Vdf.builder().config(old).charset(...)...build()} 派生。
 *
 * <pre>{@code
 * // 最简
 * VdfVirtualNode root = Vdf.parse(in);
 *
 * // 定制
 * Vdf vdf = Vdf.builder()
 *         .charset(StandardCharsets.UTF_8)
 *         .escapeSequences(true)
 *         .build();
 * VdfVirtualNode root = vdf.parse(in);
 * }</pre>
 *
 * @author liugenxin 2026/9/24
 */
public final class Vdf {

    private final VdfConfig config;

    Vdf(VdfConfig config) {
        this.config = config;
    }

    /**
     * 默认配置的门面实例; 一行解析可用 {@code Vdf.defaults().parse(in)}
     */
    public static Vdf defaults() {
        return builder().build();
    }

    /**
     * 创建 builder 定制解析配置
     */
    public static VdfBuilder builder() {
        return new VdfBuilder();
    }

    /**
     * 持有的配置（可用于派生新配置）
     */
    public VdfConfig config() {
        return config;
    }

    /**
     * 按配置组装解析管线并解析, 输入流由调用方负责关闭。
     * 每次调用都新建 scanner/tokenizer/parser, 本实例可对多个流复用。
     */
    public VdfVirtualNode parse(InputStream in) {
        return config.parser(in).parse();
    }

    /**
     * 按配置解析文件路径指向的文档, 流由门面负责打开和关闭。
     *
     * @throws VdfParseException 文件不存在、读取失败或解析失败时
     */
    public VdfVirtualNode parse(String path) {
        try (InputStream in = config.resolve(path)) {
            return parse(in);
        } catch (IOException e) {
            throw new VdfParseException("读取文件失败: " + path, e);
        }
    }
}
