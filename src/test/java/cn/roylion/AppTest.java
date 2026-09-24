package cn.roylion;

import cn.roylion.node.VdfVirtualNode;
import junit.framework.TestCase;

/**
 * VDF 解析冒烟测试
 */
public class AppTest extends TestCase {

    public void testParseVdf() {
        VdfVirtualNode parse = Vdf.defaults().parse("data/items_game.txt");
        System.out.println(parse);
    }
}
