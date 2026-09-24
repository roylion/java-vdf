package cn.roylion.parse;

import cn.roylion.node.VdfVirtualNode;

/**
 * @author liugenxin 2026/9/22 11:32
 */
public interface VdfParser {

    /**
     * 解析 VDF 文本, 返回虚拟根节点（挂载全部顶层节点）
     */
    VdfVirtualNode parse();
}
