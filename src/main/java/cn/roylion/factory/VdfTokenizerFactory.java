package cn.roylion.factory;

import cn.roylion.scanner.VdfScanner;
import cn.roylion.token.DefaultVdfTokenizer;
import cn.roylion.token.VdfTokenizer;

/**
 * 词元分析器工厂：从扫描器创建 {@link VdfTokenizer}。
 * <p>自定义实现可注入 {@code Vdf.builder()} 以替换默认词法分析。
 *
 * @author liugenxin 2026/9/24
 */
@FunctionalInterface
public interface VdfTokenizerFactory {

    /**
     * @param scanner            扫描器
     * @param usesEscapeSequences 是否开启引号内转义序列
     */
    VdfTokenizer create(VdfScanner scanner, boolean usesEscapeSequences);

    /**
     * 默认实现: {@link DefaultVdfTokenizer}
     */
    VdfTokenizerFactory DEFAULT = DefaultVdfTokenizer::new;
}
