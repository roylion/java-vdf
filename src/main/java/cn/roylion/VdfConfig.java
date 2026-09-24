package cn.roylion;

import cn.roylion.factory.VdfParserFactory;
import cn.roylion.factory.VdfScannerFactory;
import cn.roylion.factory.VdfTokenizerFactory;
import cn.roylion.parse.VdfParser;
import cn.roylion.scanner.VdfScanner;
import cn.roylion.source.VdfResourceResolver;
import cn.roylion.token.VdfTokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 解析管线配置（不可变的纯数据）：字符集、转义、缓冲、宏资源解析器和三层工厂。
 * <p>只承载数据, 不具备解析能力; 组装与解析由门面 {@link Vdf} 完成。
 * <p>解析器处理宏（#include/#base）时复用<b>同一个</b> config,
 * 保证嵌套文件与主文件的解析行为一致。
 *
 * @author liugenxin 2026/9/24
 */
public final class VdfConfig {

    private final Charset charset;
    private final int bufferSize;
    private final boolean escapeSequences;
    private final VdfResourceResolver resolver;
    private final VdfScannerFactory scannerFactory;
    private final VdfTokenizerFactory tokenizerFactory;
    private final VdfParserFactory parserFactory;

    VdfConfig(Charset charset, int bufferSize, boolean escapeSequences,
              VdfResourceResolver resolver,
              VdfScannerFactory scannerFactory, VdfTokenizerFactory tokenizerFactory,
              VdfParserFactory parserFactory) {
        this.charset = charset;
        this.bufferSize = bufferSize;
        this.escapeSequences = escapeSequences;
        this.resolver = resolver;
        this.scannerFactory = scannerFactory;
        this.tokenizerFactory = tokenizerFactory;
        this.parserFactory = parserFactory;
    }

    /**
     * 全默认配置（UTF-8 / 不转义 / 1024 / 文件系统解析器 / Default 工厂）
     */
    public static VdfConfig defaults() {
        return Vdf.builder().build().config();
    }

    public Charset charset() {
        return charset;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public boolean escapeSequences() {
        return escapeSequences;
    }

    /**
     * 宏资源解析器（无状态, 各嵌套层级共用同一实例, 相对路径基准由调用方传参）
     */
    public VdfResourceResolver resolver() {
        return resolver;
    }

    public VdfScannerFactory scannerFactory() {
        return scannerFactory;
    }

    public VdfTokenizerFactory tokenizerFactory() {
        return tokenizerFactory;
    }

    public VdfParserFactory parserFactory() {
        return parserFactory;
    }

    public InputStream resolve(String path) throws IOException {
        return resolver.open(path);
    }

    public VdfScanner scanner(InputStream in) {
        return scannerFactory.create(in, bufferSize, charset);
    }

    public VdfTokenizer tokenizer(VdfScanner scanner) {
        return tokenizerFactory.create(scanner, escapeSequences);
    }

    public VdfTokenizer tokenizer(InputStream in) {
        return tokenizerFactory.create(scanner(in), escapeSequences);
    }

    public VdfParser parser(VdfTokenizer tokenizer) {
        return parserFactory.create(tokenizer, this);
    }

    public VdfParser parser(InputStream in) {
        return parser(tokenizer(in));
    }

}
