# java-vdf

[![CI](https://github.com/roylion/java-vdf/actions/workflows/ci.yml/badge.svg)](https://github.com/roylion/java-vdf/actions/workflows/ci.yml)

Steam VDF / KeyValues 格式的 Java 解析库。

面向 `items_game.txt`、`csgo_english.txt` 等 Valve 游戏配置文件：流式读取、树模型查询、`#include` / `#base` 宏展开，以及继承式的节点合并。

## 架构：门面模式

日常使用只需要和门面 `Vdf` 打交道——它持有不可变的 `VdfConfig`，每次 `parse` 时按配置组装三层解析管线，内部实现对外完全透明：

```
        ┌─────────────────────────────────────────────┐
        │  Vdf（门面）                                  │
        │  └─ VdfConfig（纯数据：字符集/转义/缓冲/工厂）  │
        │       └─ 每次 parse(in) 现场组装 ↓             │
        │          VdfScanner ─→ VdfTokenizer ─→ VdfParser   │
        │          （扫描字符）   （解析词元）      （语法+宏） │
        └─────────────────────────────────────────────┘
                     VdfResourceResolver（宏资源来源）
```

- **`Vdf`**：门面，`parse(in)` 一招走天下；实例可复用（解析多个流）
- **`VdfConfig`**：不可变配置，通过 builder 构建，也可整体传入再逐项覆盖
- **三层管线**：都是接口，通过工厂注入即可替换任意一层（见[扩展](#扩展解析管线)）
- **`VdfResourceResolver`**：`#include` / `#base` 引用的资源从哪读，默认文件系统，可换 classpath / 内存等

## 快速开始

```java
    VdfVirtualNode root = Vdf.defaults().parse("items_game.txt");

    // 查询: key = value
    VdfNode name = root.getOne("name");
    String text = name.asText();
    int price = name.asInteger();

    // 重复 key 的同名块, 合并成一个容器
    VdfNode items = root.mergeOne("items");

    // 不存在时静默返回空, 链式安全
    boolean has = root.has("prefab");
```

## 门面用法

### 定制解析

```java
Vdf vdf = Vdf.builder()
        .charset(StandardCharsets.UTF_8)      // 输入字符集, 默认 UTF-8
        .escapeSequences(true)                // 开启引号内转义 (\n \t 等), 默认关闭
        .bufferSize(2048)                     // 扫描器快照容量, 默认 1024
        .build();

VdfVirtualNode root = vdf.parse("items_game.txt");   // 路径重载, 流由门面管理
VdfVirtualNode root2 = vdf.parse(inputStream);       // 流重载, 同一实例可解析多个输入
```

### 配置派生

配置不可变；需要变更时从现有配置派生，未覆盖的项全部继承：

```java
Vdf base = Vdf.builder().escapeSequences(true).bufferSize(2048).build();
Vdf lite = Vdf.builder().config(base.config())   // 整体传入
        .bufferSize(64)                          // 只覆盖需要的项
        .build();
```

### 宏资源的来源（#include / #base）

`VdfResourceResolver` 只有一个 `open(path)` 方法，相对路径基准由实现自行管理——默认的文件系统实现以"当前解析文件所在目录"为基准（嵌套 include 自动正确），顶层流以工作目录为基准：

```java
Vdf.builder()
   .resolver(new FileVdfResourceResolver(mainFileDir))   // 指定顶层基准目录
   .build()
   .parse(in);
```

也可以自定义 resolver 从 classpath、内存等任意来源提供宏引用的内容：

```java
VdfResourceResolver resolver = path -> ClassLoader.getSystemResourceAsStream(path);
```

注意：resolver 实例非线程安全，同一实例不要并发解析多个文档。

### 继承式合并

```java
// 深拷贝合并: 已有属性不被覆盖, 缺失属性从 parent 继承, 原树不受影响
VdfObjectNode merged = child.extend(parent);

// 自定义冲突策略: 叶子冲突时继承父类
child.extend(parent, VdfConflictStrategy.MERGE_INHERIT);

// 路径感知策略: 只在指定子树里采用父类属性
child.extend(parent, (ours, p, ctx) ->
        "sub".equals(ctx.getPath()) ? VdfInheritDecision.INHERIT : VdfInheritDecision.OVERRIDE);
```

## 扩展解析管线

扫描 / 词法 / 语法三层都是接口（`VdfScanner` / `VdfTokenizer` / `VdfParser`），通过工厂注入门面即可整体替换某一层；嵌套宏文件的解析会复用同一份配置和工厂：

```java
VdfVirtualNode root = Vdf.builder()
        .scannerFactory((in, size, charset) -> new MyScanner(in, size, charset))
        .tokenizerFactory((scanner, escape) -> new MyTokenizer(scanner, escape))
        .parserFactory((tokenizer, config) -> new MyParser(tokenizer, config))
        .build()
        .parse(in);
```

三个工厂接口（`cn.roylion.factory` 包）都是 `@FunctionalInterface`，lambda 即工厂；不设置时使用默认的 `Default*` 实现。

## 示例：真实 Valve 文件

以下示例可直接运行，测试类见 `src/test/java/cn/roylion/demo/VdfDemoTest.java`（数据文件在项目根目录 `data/` 下）。

### 查询 items_game 的物品数据

```java
VdfNode itemsGame = Vdf.defaults().parse("items_game.txt").getOne("items_game");

// 数值转换
int firstValidClass = itemsGame.getOne("game_info").getOne("first_valid_class").asInteger();

// 物品表: 编号 -> 属性; 武器自身很薄, 详细属性通过 prefab 引用
VdfNode deagle = itemsGame.getOne("items").getOne("1");
deagle.getOne("name").asText();     // "weapon_deagle"
deagle.getOne("prefab").asText();   // "weapon_deagle_prefab"
```

### 本地化文件的中英文查询

```java
VdfNode enTokens = Vdf.defaults().parse("csgo_english.txt").getOne("lang").getOne("Tokens");
VdfNode zhTokens = Vdf.defaults().parse("csgo_schinese.txt").getOne("lang").getOne("Tokens");

enTokens.getOne("SFUI_WPNHUD_Pistol").asText();      // "Pistol"
zhTokens.getOne("SFUI_WPNHUD_Pistol").asText();      // "手枪"
```

### 物品 × prefab 继承 × 本地化

items_game 里的武器节点很薄，显示名等属性在 `prefabs` 块里——用 `extend` 继承补全，再拿 `item_name` 的 token 去本地化表翻译，就是游戏里真实的武器名解析流程：

```java
VdfNode itemsGame = Vdf.defaults().parse("items_game.txt").getOne("items_game");
VdfObjectNode deagle = (VdfObjectNode) itemsGame.getOne("items").getOne("1");
VdfObjectNode prefab = (VdfObjectNode) itemsGame.getOne("prefabs")
        .getOne(deagle.getOne("prefab").asText());

// 深拷贝继承: 原树不受影响
VdfObjectNode full = deagle.extend(prefab);
String token = full.getOne("item_name").asText().substring(1);   // "SFUI_WPNHUD_DesertEagle"

enTokens.getOne(token).asText();   // "Desert Eagle"
zhTokens.getOne(token).asText();   // "沙漠之鹰"
```

## 特性

- **流式解析**：基于 `InputStream` 逐字符扫描（环形缓冲 + 快照回退），大文件不占双份内存；UTF-8 解码，支持指定字符集
- **完整词法**：带引号/裸字符串、`//` 注释、转义序列（可选开启）、`[$condition]` 条件标签、`#` 宏
- **错误定位**：解析失败抛出带 `[行, 列]` 位置的异常，直接指向出错处
- **树模型查询**：`get` / `getOne` / `mergeOne`，重复 key 全部保留、按需取首个或合并
- **类型转换**：`asText` / `asInteger` / `asLong` / `asDouble` / `asBoolean` 等按需转换
- **宏支持**：`#include`（追加）、`#base`（子类优先的递归合并，符合 Valve 语义）
- **继承式合并**：`extend` 深拷贝合并，支持自定义冲突策略（重写 / 继承 / 递归合并）和路径感知
- **条件表达式**：`[$WIN32&&!$PS3]` 解析为可求值的组合子树（And / Or / Not）
- 零第三方依赖，Java 8+

## 注意事项

- `#include` / `#base` 默认按**进程工作目录**解析相对路径，需要以主文件为基准时请传入 `FileVdfResourceResolver(主文件目录)`
- 转义序列（`\n` `\t` 等）默认关闭，与 Valve 官方实现一致；但 `\"` 和 `\\` **始终识别**——否则 value 里的转义引号会破坏分词（Valve 本地化文件大量使用）
- UTF-8 BOM 自动跳过（Valve 本地化文件常见）

## 构建

```bash
mvn test
```

## License

[MIT](LICENSE)
