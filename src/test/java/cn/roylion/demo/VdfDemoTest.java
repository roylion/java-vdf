package cn.roylion.demo;

import cn.roylion.Vdf;
import cn.roylion.node.VdfNode;
import cn.roylion.node.VdfObjectNode;
import cn.roylion.node.VdfVirtualNode;
import junit.framework.TestCase;

/**
 * 真实 Valve 文件的示例: items_game.txt 物品数据 + csgo_english/schinese 本地化。
 * 数据文件在项目根目录 data/ 下。
 *
 * @author liugenxin 2026/9/24
 */
public class VdfDemoTest extends TestCase {

    private static VdfVirtualNode parseData(String name) {
        return Vdf.defaults().parse("data/" + name);
    }

    /** Demo 1: 查询 items_game 的物品数据 */
    public void testItemsGameDemo() {
        VdfNode itemsGame = parseData("items_game.txt").getOne("items_game");

        // 数值转换
        assertEquals(2, itemsGame.getOne("game_info").getOne("first_valid_class").asInteger());

        // 物品表: 编号 -> 属性; 武器自身很薄, 详细属性通过 prefab 引用
        VdfNode deagle = itemsGame.getOne("items").getOne("1");
        assertEquals("weapon_deagle", deagle.getOne("name").asText());
        assertEquals("weapon_deagle_prefab", deagle.getOne("prefab").asText());
        assertEquals(1, deagle.getOne("baseitem").asInteger());
    }

    /** Demo 2: 本地化文件的中英文查询 */
    public void testLocalizationDemo() {
        VdfNode enTokens = parseData("csgo_english.txt").getOne("lang").getOne("Tokens");
        VdfNode zhTokens = parseData("csgo_schinese.txt").getOne("lang").getOne("Tokens");

        assertEquals("Pistol", enTokens.getOne("SFUI_WPNHUD_Pistol").asText());
        assertEquals("手枪", zhTokens.getOne("SFUI_WPNHUD_Pistol").asText());
        assertEquals("Desert Eagle", enTokens.getOne("SFUI_WPNHUD_DesertEagle").asText());
        assertEquals("沙漠之鹰", zhTokens.getOne("SFUI_WPNHUD_DesertEagle").asText());
    }

    /** Demo 3: 物品 × prefab 继承 × 本地化——沙鹰的完整显示名解析 */
    public void testItemsWithLocalizationDemo() {
        VdfNode itemsGame = parseData("items_game.txt").getOne("items_game");
        VdfObjectNode prefabs = (VdfObjectNode) itemsGame.getOne("prefabs");
        VdfNode enTokens = parseData("csgo_english.txt").getOne("lang").getOne("Tokens");
        VdfNode zhTokens = parseData("csgo_schinese.txt").getOne("lang").getOne("Tokens");

        VdfObjectNode deagle = (VdfObjectNode) itemsGame.getOne("items").getOne("1");

        // 武器节点本身没有 item_name——通过 prefab 继承补全（深拷贝, 原树不受影响）
        VdfObjectNode prefab = (VdfObjectNode) prefabs.getOne(deagle.getOne("prefab").asText());
        VdfObjectNode full = deagle.extend(prefab);
        String token = full.getOne("item_name").asText().substring(1);   // 去掉 #

        // #SFUI_WPNHUD_DesertEagle -> Desert Eagle / 沙漠之鹰
        String en = enTokens.getOne(token).asText();
        String zh = zhTokens.getOne(token).asText();
        assertEquals("Desert Eagle", en);
        assertEquals("沙漠之鹰", zh);
        System.out.printf("%s -> en=%s, zh=%s%n", full.getOne("name").asText(), en, zh);

        // 原树未被污染: deagle 节点仍然没有 item_name
        assertNull(deagle.getOne("item_name"));
    }
}
