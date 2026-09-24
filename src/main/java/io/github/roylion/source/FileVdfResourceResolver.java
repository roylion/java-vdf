package io.github.roylion.source;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 基于文件系统的宏资源解析器。
 * <p>内部维护"当前解析文件所在目录"的栈：打开资源时以其为相对路径基准并压入
 * 该资源所在目录, 返回的流关闭时弹栈——嵌套宏的相对路径自然以被引用文件为基准,
 * 栈空时（顶层流）以 defaultDir（默认进程工作目录）为基准。
 * <p>非线程安全。
 *
 * @author liugenxin 2026/9/24
 */
public class FileVdfResourceResolver implements VdfResourceResolver {

    private final Path defaultDir;
    /** 当前解析文件所在目录栈, 栈顶即嵌套宏的相对路径基准 */
    private final Deque<Path> context = new ArrayDeque<>();

    /**
     * 顶层流的相对路径基准默认为进程工作目录
     */
    public FileVdfResourceResolver() {
        this(Paths.get(System.getProperty("user.dir")));
    }

    /**
     * @param defaultDir 顶层流（没有宏上下文时）的相对路径基准, 通常是主文件所在目录
     */
    public FileVdfResourceResolver(Path defaultDir) {
        this.defaultDir = defaultDir;
    }

    @Override
    public InputStream open(String path) throws IOException {
        Path base = context.isEmpty() ? defaultDir : context.peek();
        Path resolved = resolve(path, base);
        Path dir = resolved.toAbsolutePath().getParent();
        context.push(dir);
        return new ContextStream(Files.newInputStream(resolved), dir);
    }

    private Path resolve(String path, Path base) {
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        return base.resolve(p);
    }

    /**
     * 关闭时弹出对应的目录上下文, 保证嵌套层级与 try-with-resources 的作用域一致
     */
    private class ContextStream extends FilterInputStream {
        private final Path dir;
        private boolean closed = false;

        ContextStream(InputStream in, Path dir) {
            super(in);
            this.dir = dir;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!closed) {
                    closed = true;
                    // 严格 LIFO: 解析过程的 open/close 按 try-with-resources 嵌套配对
                    context.pop();
                }
            }
        }
    }
}
