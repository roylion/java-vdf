package io.github.roylion.source;

import java.io.IOException;
import java.io.InputStream;

/**
 * 宏资源解析器：把 {@code #include} / {@code #base} 的路径参数解析成可读的流。
 * <p>相对路径的基准由实现<b>自行管理</b>——典型的实现会在打开资源时记录
 * "当前解析的文件", 该文件内部的宏以它所在目录为基准解析, 流关闭时清理上下文
 * （调用方用 try-with-resources 即可保证层级正确）。
 * <p>非线程安全：同一实例不应并发解析多个文档。
 *
 * @author liugenxin 2026/9/24
 */
public interface VdfResourceResolver {

    /**
     * 打开宏引用的资源, 调用方负责关闭返回的流（关闭即清理相对路径上下文）。
     *
     * @param path 宏的路径参数（引号已剥除）
     * @throws IOException 资源不存在或无法打开时
     */
    InputStream open(String path) throws IOException;
}
