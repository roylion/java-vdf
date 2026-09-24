package cn.roylion.token;

/**
 * @author liugenxin 2026/9/21 16:13
 */
public interface VdfTokenizer {

    /**
     * 获取下一个词元
     *
     * @return 词元
     */
    VdfToken next();
}
