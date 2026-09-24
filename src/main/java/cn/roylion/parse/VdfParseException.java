package cn.roylion.parse;

import cn.roylion.token.VdfToken;

/**
 * VDF 解析异常，附带出错词元的位置信息
 *
 * @author liugenxin 2026/9/22
 */
public class VdfParseException extends RuntimeException {

    public VdfParseException(String message) {
        super(message);
    }

    public VdfParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 以词元位置构造异常，message 会自动拼接 "第x行y列" 前缀
     */
    public static VdfParseException atToken(String message, VdfToken token) {
        return new VdfParseException(String.format("[第%d行 第%d列] %s",
                token.getLine(), token.getCol(), message));
    }
}
