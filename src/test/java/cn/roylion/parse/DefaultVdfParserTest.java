package cn.roylion.parse;

import cn.roylion.Vdf;
import cn.roylion.node.VdfVirtualNode;
import cn.roylion.source.FileVdfResourceResolver;
import cn.roylion.source.VdfResourceResolver;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 宏（#include/#base）与资源解析测试。数据文件在项目根目录 data/ 下。
 *
 * @author liugenxin 2026/9/24
 */
public class DefaultVdfParserTest extends TestCase {

    private static final Path DATA = Paths.get("data");

    /** 在临时目录写入一个 VDF 文件, 返回其路径（测试内容非文件资源时用） */
    private static Path writeTemp(String dirPrefix, String name, String content) throws IOException {
        Path dir = Files.createTempDirectory(dirPrefix);
        Path file = dir.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    public void testRelativeBasePath() {
        // parse(String) 也经由 resolver 打开主文件, 打开后宏的相对路径以主文件所在目录为基准
        VdfVirtualNode root = Vdf.builder()
                .resolver(new FileVdfResourceResolver(Paths.get(".")))   // 顶层基准: 项目根目录
                .build()
                .parse("data/vdf_test_relative_main.txt");

        // 子类优先: Key1 保留主文件的值
        assertEquals("Value1", root.getOne("Key1").asText());
        // base 补齐: Key2 来自 extras
        assertEquals("Extra2", root.getOne("Key2").asText());
        // 递归合并: List.InnerKey1 保留主文件, InnerKey2 来自 extras
        assertEquals("InnerValue1", root.getOne("List").getOne("InnerKey1").asText());
        assertEquals("InnerExtra2", root.getOne("List").getOne("InnerKey2").asText());
    }

    public void testNestedRelativePath() throws IOException {
        // 子目录里的文件再 include 同目录的文件: 相对路径以被引用文件所在目录为基准
        Path dir = Files.createTempDirectory("vdf-nested");
        Files.write(dir.resolve("main.txt"),
                "#include \"sub/inner.txt\"\n\"k\" \"v\"\n".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub/inner.txt"),
                "#base \"leaf.txt\"\n\"inner\" \"1\"\n".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("sub/leaf.txt"),
                "\"leaf\" \"2\"\n".getBytes(StandardCharsets.UTF_8));

        VdfVirtualNode root = Vdf.builder()
                .resolver(new FileVdfResourceResolver(dir))
                .build()
                .parse(dir.resolve("main.txt").toString());
        assertEquals("v", root.getOne("k").asText());
        assertEquals("1", root.getOne("inner").asText());
        assertEquals("2", root.getOne("leaf").asText());   // sub/inner.txt 的 #base 以 sub/ 为基准
    }

    public void testCustomResolver() {
        // 自定义解析器: 主文件与宏引用都从内存返回内容（classpath 等来源同理）
        VdfResourceResolverInMemory resolver = new VdfResourceResolverInMemory("injected");
        VdfVirtualNode root = Vdf.builder()
                .resolver(resolver)
                .build()
                .parse("main");
        assertEquals("main", root.getOne("host").asText());
        assertEquals("injected", root.getOne("mem").asText());
    }

    public void testMissingResourceThrows() throws IOException {
        Path main = writeTemp("vdf-missing", "main.txt", "#base \"no/such/file.txt\"\n");
        try {
            Vdf.builder()
                    .resolver(new FileVdfResourceResolver(main.getParent()))
                    .build()
                    .parse(main.toString());
            fail("引用不存在的文件应抛出 VdfParseException");
        } catch (VdfParseException expected) {
            assertTrue(expected.getMessage().contains("不存在"));
        }
    }

    /** 内存解析器: 主文件返回固定文本, 其余路径（宏引用）返回注入内容 */
    private static class VdfResourceResolverInMemory implements VdfResourceResolver {
        private static final String MAIN = "#include \"memory:demo\"\n\"host\" \"main\"\n";
        private final String value;

        VdfResourceResolverInMemory(String value) {
            this.value = value;
        }

        @Override
        public InputStream open(String path) {
            String text = "main".equals(path) ? MAIN : "\"mem\" \"" + value + "\"\n";
            return new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
