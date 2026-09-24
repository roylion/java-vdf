package cn.roylion.node;

import cn.roylion.merge.VdfMergeStrategy;
import cn.roylion.merge.VdfMergeDecision;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liugenxin 2026/9/23
 */
public class VdfNodeTest extends TestCase {

    /** 测试助手: 取第一个匹配的子节点 */
    private static VdfNode first(VdfNode node, String key) {
        List<VdfNode> all = node.get(key);
        return all.isEmpty() ? null : all.get(0);
    }

    public void testStringNodeConversions() {
        VdfStringNode node = VdfNodeFactory.val("k", "42");

        assertEquals("42", node.asText());
        assertEquals(42, node.asByte());
        assertEquals(42, node.asShort());
        assertEquals(42, node.asInteger());
        assertEquals(42L, node.asLong());
        assertEquals(42.0, node.asDouble());
        assertEquals(42.0f, node.asFloat(), 1e-6f);
        assertTrue(VdfNodeFactory.val("b", "1").asBoolean());
    }

    public void testOutOfRangeThrows() {
        // byte 上限 127, short 上限 32767, 超范围抛 NumberFormatException
        VdfStringNode node = VdfNodeFactory.val("k", "128");
        try {
            node.asByte();
            fail("超出 byte 范围应抛出 NumberFormatException");
        } catch (NumberFormatException expected) {
        }
        assertEquals(128, node.asShort());

        VdfStringNode big = VdfNodeFactory.val("k", "32768");
        try {
            big.asShort();
            fail("超出 short 范围应抛出 NumberFormatException");
        } catch (NumberFormatException expected) {
        }
    }

    public void testBooleanConversion() {
        assertTrue(VdfNodeFactory.val("a", "1").asBoolean());
        assertTrue(VdfNodeFactory.val("a", "TRUE").asBoolean());
        assertFalse(VdfNodeFactory.val("a", "0").asBoolean());
        assertFalse(VdfNodeFactory.val("a", "False").asBoolean());
        try {
            VdfNodeFactory.val("a", "yes").asBoolean();
            fail("非法 boolean 值应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testObjectNodeConversionsThrow() {
        VdfObjectNode obj = VdfNodeFactory.obj("k");
        try {
            obj.asText();
            fail("容器节点 asText 应抛出 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            obj.asInteger();
            fail("容器节点 asInteger 应抛出 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    public void testLeafIncludeAndBaseAreSilent() {
        // 叶子节点调用 include/base 不做事、不抛异常
        VdfStringNode leaf = VdfNodeFactory.val("k", "v");
        VdfVirtualNode other = VdfNodeFactory.virtual();
        other.put(VdfNodeFactory.val("x", "1"));

        leaf.include(other);
        leaf.base(other);
        assertEquals("v", leaf.asText());
    }

    public void testChildQuery() {
        VdfObjectNode obj = VdfNodeFactory.obj("cfg");
        VdfObjectNode sub = VdfNodeFactory.obj("sub");
        sub.put(VdfNodeFactory.val("inner", "i"));
        obj.put(VdfNodeFactory.val("name", "ak47"));
        obj.put(VdfNodeFactory.val("dup", "first"));
        obj.put(VdfNodeFactory.val("dup", "second"));
        obj.put(sub);

        assertTrue(obj.has("name"));
        assertFalse(obj.has("nope"));
        assertEquals("ak47", first(obj, "name").asText());
        // getFirst: 重复 key 只取首个, 不存在返回 null
        assertEquals("first", obj.getOne("dup").asText());
        assertNull(obj.getOne("nope"));
        // 嵌套 getFirst 链式查询
        assertEquals("i", obj.getOne("sub").getOne("inner").asText());
        // 重复 key: 返回全部, 按插入顺序
        List<VdfNode> dups = obj.get("dup");
        assertEquals(2, dups.size());
        assertEquals("first", dups.get(0).asText());
        assertEquals("second", dups.get(1).asText());
        // 不存在的 key: 空列表而非 null
        assertTrue(obj.get("nope").isEmpty());
        // Optional 风格: 重复 key 只取首个
        assertEquals("first", obj.getOneOptional("dup").map(VdfNode::asText).orElse(null));
        assertFalse(obj.getOneOptional("nope").isPresent());

        // 叶子节点上调用查询: 静默返回空列表 / null
        VdfStringNode leaf = VdfNodeFactory.val("name", "ak47");
        assertTrue(leaf.get("anything").isEmpty());
        assertNull(leaf.getOne("anything"));
        assertFalse(leaf.has("anything"));
        assertFalse(leaf.getOneOptional("anything").isPresent());
    }

    public void testIncludeAndBase() {
        VdfObjectNode main = VdfNodeFactory.obj("main");
        main.put(VdfNodeFactory.val("a", "1"));
        main.put(VdfNodeFactory.obj("sub"));

        VdfVirtualNode extras = VdfNodeFactory.virtual();
        extras.put(VdfNodeFactory.val("b", "2"));
        extras.put(VdfNodeFactory.obj("sub2"));

        // include: 全部追加, 不合并
        main.include(extras);
        assertTrue(main.has("a"));
        assertTrue(main.has("b"));
        assertTrue(main.has("sub"));
        assertTrue(main.has("sub2"));

        // base: 递归合并
        VdfVirtualNode base = VdfNodeFactory.virtual();
        VdfObjectNode baseSub = VdfNodeFactory.obj("sub");
        baseSub.put(VdfNodeFactory.val("inner", "i"));
        base.put(baseSub);
        base.put(VdfNodeFactory.val("c", "3"));

        main.base(base);
        // sub 被合并进已有的 sub（inner 进去了）, c 是新节点被追加
        assertEquals("i", first(first(main, "sub"), "inner").asText());
        assertTrue(main.has("c"));
    }

    public void testDeepCopy() {
        VdfObjectNode obj = VdfNodeFactory.obj("cfg");
        VdfObjectNode sub = VdfNodeFactory.obj("sub");
        sub.put("inner", "i");
        obj.put(sub);
        obj.put("name", "ak47");

        VdfObjectNode copy = obj.deepCopy();
        // 内容一致
        assertEquals("ak47", first(copy, "name").asText());
        assertEquals("i", first(first(copy, "sub"), "inner").asText());
        // 修改拷贝不影响原树, 反之亦然
        ((VdfObjectNode) first(copy, "sub")).put("added", "1");
        copy.put("name", "changed");
        assertFalse(first(obj, "sub").has("added"));
        assertEquals("ak47", first(obj, "name").asText());

        ((VdfObjectNode) first(obj, "sub")).put("orig", "1");
        assertFalse(first(copy, "sub").has("orig"));

        // 虚拟根拷贝仍是虚拟根
        VdfVirtualNode root = VdfNodeFactory.virtual();
        root.put("k", "v");
        VdfVirtualNode rootCopy = (VdfVirtualNode) root.deepCopy();
        assertTrue(rootCopy.isVirtual());
        assertEquals("v", first(rootCopy, "k").asText());
    }

    public void testExtend() {
        VdfObjectNode child = VdfNodeFactory.obj("child");
        child.put("own", "keep");
        VdfObjectNode childSub = VdfNodeFactory.obj("sub");
        childSub.put("a", "1");
        child.put(childSub);

        VdfObjectNode parent = VdfNodeFactory.obj("parent");
        parent.put("own", "override");
        parent.put("extra", "from-parent");
        VdfObjectNode parentSub = VdfNodeFactory.obj("sub");
        parentSub.put("b", "2");
        parentSub.put("c", "3");
        parent.put(parentSub);

        VdfObjectNode merged = child.extend(parent);

        // 不是同一个对象, 深拷贝语义
        assertNotSame(child, merged);
        // 已有属性不被覆盖
        assertEquals("keep", first(merged, "own").asText());
        // 递归继承: sub 里补齐了 b/c, 保留 a
        assertEquals("1", first(first(merged, "sub"), "a").asText());
        assertEquals("2", first(first(merged, "sub"), "b").asText());
        assertEquals("3", first(first(merged, "sub"), "c").asText());
        // 父类独有
        assertEquals("from-parent", first(merged, "extra").asText());

        // 原树完全不受影响——包括从 parent 继承进来的子树
        assertFalse(child.has("extra"));
        assertEquals("keep", first(child, "own").asText());
        assertFalse(first(child, "sub").has("b"));
        ((VdfObjectNode) first(merged, "sub")).put("pollute", "1");
        assertFalse(first(parent, "sub").has("pollute"));
        assertFalse(first(child, "sub").has("pollute"));

        // extend 自身: 合法, 等价于 deepCopy
        VdfObjectNode selfMerged = child.extend(child);
        assertNotSame(child, selfMerged);
        assertEquals("keep", first(selfMerged, "own").asText());

        // null 防御
        try {
            child.extend(null);
            fail("extend(null) 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    public void testExtendWithStrategies() {
        VdfObjectNode child = VdfNodeFactory.obj("child");
        child.put("own", "keep");
        VdfObjectNode childSub = VdfNodeFactory.obj("sub");
        childSub.put("a", "1");
        child.put(childSub);

        VdfObjectNode parent = VdfNodeFactory.obj("parent");
        parent.put("own", "override");
        parent.put("extra", "from-parent");
        VdfObjectNode parentSub = VdfNodeFactory.obj("sub");
        parentSub.put("a", "999");
        parentSub.put("b", "2");
        parent.put(parentSub);

        // PARENT_WINS: 叶子冲突子类继承父类的（含递归层）, 容器仍递归合并
        VdfObjectNode merged = child.extend(parent, VdfMergeStrategy.INHERIT);
        assertEquals("override", first(merged, "own").asText());
        assertEquals("999", first(first(merged, "sub"), "a").asText());
        assertEquals("from-parent", first(merged, "extra").asText());

        // 继承不污染原树
        assertEquals("keep", first(child, "own").asText());
        assertEquals("override", first(parent, "own").asText());

        // 路径感知的自定义策略: 只在 sub 子树里继承父类叶子
        VdfObjectNode pathAware = child.extend(parent, (ours, p, ctx) -> {
            if ("sub".equals(ctx.getPath())) {
                return VdfMergeDecision.OVERRIDE;
            }
            return ours.isObj() && p.isObj() ? VdfMergeDecision.MERGE : VdfMergeDecision.PRESERVE;
        });
        assertEquals("999", first(first(pathAware, "sub"), "a").asText());  // sub/a 被父类覆盖
        assertEquals("keep", first(pathAware, "own").asText());             // 顶层叶子保留自己的

        // null 防御
        try {
            child.extend(null, VdfMergeStrategy.OVERRIDE);
            fail("extend(null, ...) 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
        try {
            child.extend(parent, null);
            fail("extend(parent, null) 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    public void testExtendContextStack() {
        VdfObjectNode child = VdfNodeFactory.obj("child");
        VdfObjectNode sub = VdfNodeFactory.obj("sub");
        VdfObjectNode deep = VdfNodeFactory.obj("deep");
        deep.put("y", "1");
        sub.put(deep);
        child.put(sub);
        child.put("own", "keep");

        VdfObjectNode parent = VdfNodeFactory.obj("parent");
        VdfObjectNode pSub = VdfNodeFactory.obj("sub");
        VdfObjectNode pDeep = VdfNodeFactory.obj("deep");
        pDeep.put("y", "2");
        pSub.put(pDeep);
        parent.put(pSub);
        parent.put("own", "override");

        List<String> seenPaths = new ArrayList<>();
        child.extend(parent, (ours, p, ctx) -> {
            seenPaths.add(ctx.getPath());
            return ours.isObj() && p.isObj() ? VdfMergeDecision.MERGE : VdfMergeDecision.PRESERVE;
        });

        // 冲突点路径: 顶层叶子, 以及递归下钻后的深层节点（栈随下钻增长、回退后复原）
        assertTrue(seenPaths.contains("own"));
        assertTrue(seenPaths.contains("sub/deep"));
        // 栈被正确弹空, 不残留上一个冲突的路径
        for (String path : seenPaths) {
            assertFalse(path.startsWith("/") || path.endsWith("/"));
        }
    }

    public void testMergeOne() {
        VdfObjectNode obj = VdfNodeFactory.obj("cfg");
        // 两个同名块: 第一个有 a/共有子块 sub, 第二个有 b
        VdfObjectNode items1 = VdfNodeFactory.obj("items");
        items1.put("a", "1");
        VdfObjectNode sub1 = VdfNodeFactory.obj("sub");
        sub1.put("x", "1");
        items1.put(sub1);
        VdfObjectNode items2 = VdfNodeFactory.obj("items");
        items2.put("b", "2");
        VdfObjectNode sub2 = VdfNodeFactory.obj("sub");
        sub2.put("y", "2");
        items2.put(sub2);
        obj.put(items1);
        obj.put(items2);
        obj.put("name", "ak47");

        // 同名块合并成一个: 属性取并集, 子类（前面的）优先
        VdfNode merged = obj.mergeOne("items");
        assertEquals("1", first(merged, "a").asText());
        assertEquals("2", first(merged, "b").asText());
        assertEquals("1", first(first(merged, "sub"), "x").asText());
        assertEquals("2", first(first(merged, "sub"), "y").asText());

        // 结果与原树隔离: 改结果不影响原树
        ((VdfObjectNode) merged).put("added", "1");
        assertFalse(items1.has("added"));
        assertFalse(items2.has("added"));

        // 叶子节点的同名值不参与 mergeOne（无法合并成容器）
        try {
            obj.mergeOne("name");
            fail("同名子节点含叶子时应抛出 IllegalStateException");
        } catch (IllegalStateException expected) {
        }

        // 不存在 / 叶子节点上调用: 静默返回 null / 空 Optional
        assertNull(obj.mergeOne("nope"));
        assertFalse(obj.mergeOneOptional("nope").isPresent());
        VdfStringNode leaf = VdfNodeFactory.val("k", "v");
        assertNull(leaf.mergeOne("k"));
        // Optional 形式
        assertTrue(obj.mergeOneOptional("items").isPresent());
    }

    public void testIncludeAndBaseDefenses() {
        VdfVirtualNode doc = VdfNodeFactory.virtual();
        try {
            doc.include(doc);
            fail("把文档 include 到它自身应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            doc.base(doc);
            fail("把文档 base 到它自身应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            doc.include(null);
            fail("include(null) 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
        try {
            doc.base(null);
            fail("base(null) 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
    }

    public void testVirtualRoot() {
        VdfVirtualNode root = VdfNodeFactory.virtual();
        assertTrue(root instanceof VdfObjectNode);
        // 类型判断: 虚拟根既是容器又是虚拟根, 不是叶子
        assertTrue(root.isObj());
        assertTrue(root.isVirtual());
        assertFalse(root.isValue());
        // 普通容器不是虚拟根
        assertFalse(VdfNodeFactory.obj("k").isVirtual());
        // 叶子节点
        VdfNode leaf = VdfNodeFactory.val("k", "v");
        assertTrue(leaf.isValue());
        assertFalse(leaf.isObj());
        // key 为 null, 不占用任何 key
        assertNull(root.getKey());
        // 挂载与查询正常
        root.put("name", "ak47");
        assertEquals("ak47", first(root, "name").asText());
        // 文档里就算真有 "_virtual" 节点也不会和根冲突
        root.put(VdfNodeFactory.val("_virtual", "real"));
        assertEquals("real", first(root, "_virtual").asText());
        assertEquals("_virtual_root", root.toString());
    }

    public void testPutValue() {
        VdfObjectNode obj = VdfNodeFactory.obj("cfg");
        obj.put("price", "650");
        obj.put("ratio", "1.5");

        assertEquals(650, first(obj, "price").asInteger());
        assertEquals(1.5, first(obj, "ratio").asDouble());

        try {
            obj.put("nil", null);
            fail("value 为 null 应抛出 NullPointerException");
        } catch (NullPointerException expected) {
        }
    }
}
