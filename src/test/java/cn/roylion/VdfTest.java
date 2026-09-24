package cn.roylion;

import cn.roylion.factory.VdfParserFactory;
import cn.roylion.factory.VdfScannerFactory;
import cn.roylion.factory.VdfTokenizerFactory;
import cn.roylion.node.VdfVirtualNode;
import cn.roylion.scanner.DefaultVdfScanner;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liugenxin 2026/9/24
 */
public class VdfTest extends TestCase {

    private static InputStream in(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    public void testDefaultParse() {
        VdfVirtualNode root = Vdf.defaults().parse(in("\"name\" \"ak47\"\n\"price\" \"650\"\n"));
        assertEquals("ak47", root.getOne("name").asText());
        assertEquals(650, root.getOne("price").asInteger());
    }

    public void testBuilderEscapeSequences() {
        // 默认关闭: 反斜杠是字面量
        VdfVirtualNode plain = Vdf.builder().build().parse(in("\"k\" \"a\\nb\""));
        assertEquals("a\\nb", plain.getOne("k").asText());

        // 开启: \n 是换行
        Vdf escaped = Vdf.builder().escapeSequences(true).build();
        assertEquals("a\nb", escaped.parse(in("\"k\" \"a\\nb\"")).getOne("k").asText());
    }

    public void testBuilderCharset() {
        byte[] all = ("\"k\" \"" + "中" + "\"").getBytes(Charset.forName("GBK"));
        Vdf vdf = Vdf.builder().charset(Charset.forName("GBK")).build();
        assertEquals("中", vdf.parse(new ByteArrayInputStream(all)).getOne("k").asText());
    }

    public void testConfigDerivation() {
        // build 产出门面; 门面暴露 config, 可作为新 builder 的起点逐项覆盖
        Vdf base = Vdf.builder().escapeSequences(true).bufferSize(2048).build();
        Vdf derived = Vdf.builder().config(base.config()).bufferSize(64).build();

        assertTrue(base.config().escapeSequences());      // 保留
        assertEquals(2048, base.config().bufferSize());
        assertTrue(derived.config().escapeSequences());   // 继承
        assertEquals(64, derived.config().bufferSize());  // 覆盖
    }

    public void testNestedMacroInheritsCharset() throws Exception {
        // 修复的回归: 嵌套 #include 的文件此前写死 UTF-8, 主文件的 GBK 配置会丢失
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("vdf-gbk");
        Charset gbk = Charset.forName("GBK");
        java.nio.file.Files.write(dir.resolve("main.txt"),
                "#include \"sub.txt\"\n\"k\" \"主\"".getBytes(gbk));
        java.nio.file.Files.write(dir.resolve("sub.txt"),
                "\"s\" \"中\"".getBytes(gbk));

        VdfVirtualNode root = Vdf.builder()
                .charset(gbk)
                .resolver(new cn.roylion.source.FileVdfResourceResolver(dir))
                .build()
                .parse(dir.resolve("main.txt").toString());

        assertEquals("主", root.getOne("k").asText());
        assertEquals("中", root.getOne("s").asText());   // 嵌套文件同样按 GBK 解析
    }

    public void testCustomFactories() {
        List<String> calls = new ArrayList<>();

        // 三个工厂全部替换: 自定义扫描器包一层默认实现, 词法/语法工厂记录调用
        VdfScannerFactory scannerFactory = (in, size, charset) -> {
            calls.add("scanner:" + size + ":" + charset);
            return new DefaultVdfScanner(in, size, charset);
        };
        VdfTokenizerFactory tokenizerFactory = (scanner, escape) -> {
            calls.add("tokenizer:" + escape);
            return new cn.roylion.token.DefaultVdfTokenizer(scanner, escape);
        };
        VdfParserFactory parserFactory = (tokenizer, cfg) -> {
            calls.add("parser");
            return new cn.roylion.parse.DefaultVdfParser(tokenizer, cfg);
        };

        Vdf vdf = Vdf.builder()
                .bufferSize(64)
                .escapeSequences(true)
                .scannerFactory(scannerFactory)
                .tokenizerFactory(tokenizerFactory)
                .parserFactory(parserFactory)
                .build();
        VdfVirtualNode root = vdf.parse(in("\"k\" \"v\""));

        // 自定义工厂确实被调用, 且参数透传正确
        assertEquals(3, calls.size());
        assertTrue(calls.get(0).contains("scanner:64"));
        assertTrue(calls.get(1).contains("tokenizer:true"));
        assertEquals("v", root.getOne("k").asText());

        // 同一门面实例可对多个流复用
        assertEquals("v2", vdf.parse(in("\"k\" \"v2\"")).getOne("k").asText());
        assertEquals(6, calls.size());
    }
}
