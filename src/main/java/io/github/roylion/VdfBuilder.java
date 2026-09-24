package io.github.roylion;

import io.github.roylion.factory.VdfParserFactory;
import io.github.roylion.factory.VdfScannerFactory;
import io.github.roylion.factory.VdfTokenizerFactory;
import io.github.roylion.parse.DefaultVdfParser;
import io.github.roylion.scanner.DefaultVdfScanner;
import io.github.roylion.source.FileVdfResourceResolver;
import io.github.roylion.source.VdfResourceResolver;
import io.github.roylion.token.DefaultVdfTokenizer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 门面构建器：可整体传入一份 {@link VdfConfig} 再逐项覆盖,
 * {@link #build()} 产出持有最终配置的门面 {@link Vdf}。
 *
 * @author liugenxin 2026/9/24
 */
public final class VdfBuilder {

    private Charset charset = StandardCharsets.UTF_8;
    private int bufferSize = 1024;
    private boolean escapeSequences = false;
    private VdfResourceResolver resolver = new FileVdfResourceResolver();
    private VdfScannerFactory scannerFactory = VdfScannerFactory.DEFAULT;
    private VdfTokenizerFactory tokenizerFactory = VdfTokenizerFactory.DEFAULT;
    private VdfParserFactory parserFactory = VdfParserFactory.DEFAULT;

    /**
     * 以一份现成配置为起点（之后仍可用各项 setter 覆盖）
     */
    public VdfBuilder config(VdfConfig config) {
        this.charset = config.charset();
        this.bufferSize = config.bufferSize();
        this.escapeSequences = config.escapeSequences();
        this.resolver = config.resolver();
        this.scannerFactory = config.scannerFactory();
        this.tokenizerFactory = config.tokenizerFactory();
        this.parserFactory = config.parserFactory();
        return this;
    }

    /**
     * 输入流字符集, 默认 UTF-8
     */
    public VdfBuilder charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * 扫描器快照缓冲容量, 默认 1024（决定可回退的深度）
     */
    public VdfBuilder bufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * 是否开启引号内转义序列（\n \t \\ \") , 默认关闭（与 Valve 官方一致）
     */
    public VdfBuilder escapeSequences(boolean escapeSequences) {
        this.escapeSequences = escapeSequences;
        return this;
    }

    /**
     * 宏资源解析器（#include/#base 的路径如何打开, 无状态实例）,
     * 默认基于工作目录的文件系统解析
     */
    public VdfBuilder resolver(VdfResourceResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    /**
     * 扫描器工厂, 默认 {@link DefaultVdfScanner}
     */
    public VdfBuilder scannerFactory(VdfScannerFactory scannerFactory) {
        this.scannerFactory = scannerFactory;
        return this;
    }

    /**
     * 词元分析器工厂, 默认 {@link DefaultVdfTokenizer}
     */
    public VdfBuilder tokenizerFactory(VdfTokenizerFactory tokenizerFactory) {
        this.tokenizerFactory = tokenizerFactory;
        return this;
    }

    /**
     * 解析器工厂, 默认 {@link DefaultVdfParser}
     */
    public VdfBuilder parserFactory(VdfParserFactory parserFactory) {
        this.parserFactory = parserFactory;
        return this;
    }

    /**
     * 构建门面; 宏（#include/#base）递归解析时复用同一份配置
     */
    public Vdf build() {
        return new Vdf(new VdfConfig(charset, bufferSize, escapeSequences, resolver,
                scannerFactory, tokenizerFactory, parserFactory));
    }
}
