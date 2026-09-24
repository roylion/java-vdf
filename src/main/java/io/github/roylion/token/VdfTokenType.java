package io.github.roylion.token;

/**
 * @author liugenxin 2026/9/21 15:30
 */
// @formatter:off
public enum VdfTokenType {
    /** 左大括号 { */
    LBRACE,
    /** 右大括号 } */
    RBRACE,
    /** 双引号字符串 "hello" 代表键/值 需要处理 \"、\\、\n、\t */
    QUOTED_STRING,
    /** 裸字符串 hello 代表键/值 读到空白、{、}、" 结束 */
    UNQUOTED_STRING,
    /** 条件判断 */
    CONDITION,
    /** 左中括号 [ */
    LBRACKET,
    /** 右中括号 ] */
    RBRACKET,
    /** 井号 # ... */
    HASH,
    /** 注释 // ... */
    COMMENT,
    /** 宏定义 */
    MACRO,
    /** 文件结束 */
    EOF,
    ;
}
