package cn.roylion.factory;

import cn.roylion.VdfConfig;
import cn.roylion.parse.DefaultVdfParser;
import cn.roylion.parse.VdfParser;
import cn.roylion.token.VdfTokenizer;

/**
 * 解析器工厂：从词元分析器创建 {@link VdfParser}。
 * <p>自定义实现可注入 {@code Vdf.builder()} 以替换默认语法分析;
 * config 需透传给解析器, 保证宏（#include/#base）递归解析时配置一致。
 *
 * @author liugenxin 2026/9/24
 */
@FunctionalInterface
public interface VdfParserFactory {

    /**
     * @param tokenizer 词元分析器
     * @param config    解析管线配置（含宏资源解析器, 相对路径基准由它自行管理）
     */
    VdfParser create(VdfTokenizer tokenizer, VdfConfig config);

    /**
     * 默认实现: {@link DefaultVdfParser}
     */
    VdfParserFactory DEFAULT = DefaultVdfParser::new;
}
