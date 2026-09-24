package io.github.roylion.factory;

import io.github.roylion.scanner.DefaultVdfScanner;
import io.github.roylion.scanner.VdfScanner;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 扫描器工厂：从输入流创建 {@link VdfScanner}。
 * <p>自定义实现可注入 {@code Vdf.builder()} 以替换默认扫描器。
 *
 * @author liugenxin 2026/9/24
 */
@FunctionalInterface
public interface VdfScannerFactory {

    /**
     * @param in         输入流（由调用方负责关闭）
     * @param bufferSize 快照缓冲容量建议值, 实现可忽略
     * @param charset    字符集, 实现可忽略（如自行解码）
     */
    VdfScanner create(InputStream in, int bufferSize, Charset charset);

    /**
     * 默认实现: {@link DefaultVdfScanner}
     */
    VdfScannerFactory DEFAULT = DefaultVdfScanner::new;
}
