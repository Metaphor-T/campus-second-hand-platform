package com.example.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 敏感词工具类 —— 基于 DFA（确定性有限自动机）算法实现
 * <p>
 * 核心思想：将敏感词词典构建为一棵 Trie 树（前缀树），
 * 扫描文本时逐字符在树中匹配，命中则替换为等长 '*'。
 * 时间复杂度 O(n)，空间换时间，适合大规模实时过滤场景。
 * </p>
 * <p>
 * 敏感词典直接定义在本类中，类加载时自动初始化，无需外部配置。
 * </p>
 */
@Slf4j
public class SensitiveWordUtil {

    /**
     * Trie 树根节点
     */
    private static final TrieNode ROOT = new TrieNode();

    /**
     * 替换字符
     */
    private static final char REPLACE_CHAR = '*';

    /**
     * 词典是否已初始化
     */
    private static volatile boolean initialized = false;

    /**
     * 敏感词列表-可自行拓展
     */
    private static final List<String> SENSITIVE_WORDS = List.of(
            "TMD", "MD","暴力"
    );

    // 类加载时自动初始化敏感词词典
    static {
        init();
    }

    /**
     * Trie 树节点
     */
    private static class TrieNode {
        /**
         * 子节点映射：字符 -> 节点
         */
        private final Map<Character, TrieNode> children = new HashMap<>();
        /**
         * 是否为某个敏感词的结尾
         */
        private boolean isEnd = false;
    }

    /**
     * 初始化敏感词词典（类加载时自动调用）
     */
    private static void init() {
        synchronized (SensitiveWordUtil.class) {
            ROOT.children.clear();
            for (String word : SENSITIVE_WORDS) {
                if (word == null || word.trim().isEmpty()) {
                    continue;
                }
                addWord(word.trim().toLowerCase());
            }
            initialized = true;
            log.info("敏感词词典初始化完成，共加载 {} 个敏感词", SENSITIVE_WORDS.size());
        }
    }

    /**
     * 向 Trie 树中添加一个敏感词
     */
    private static void addWord(String word) {
        TrieNode current = ROOT;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.isEnd = true;
    }

    /**
     * 判断文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true 包含敏感词
     */
    public static boolean contains(String text) {
        if (!initialized || text == null || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return findFirstMatch(lowerText) != null;
    }

    /**
     * 查找文本中第一个命中的敏感词
     *
     * @param text 待检测文本
     * @return 命中的敏感词，无匹配返回 null
     */
    public static String findFirst(String text) {
        if (!initialized || text == null || text.isEmpty()) {
            return null;
        }
        return findFirstMatch(text.toLowerCase());
    }

    /**
     * 查找文本中所有命中的敏感词
     *
     * @param text 待检测文本
     * @return 命中的敏感词列表（去重、按首次出现顺序）
     */
    public static List<String> findAll(String text) {
        if (!initialized || text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return findAllMatches(text.toLowerCase());
    }

    /**
     * 替换文本中的敏感词为 '*'
     * <p>每个敏感词字符替换为等长的 '*'，保持原文本长度不变</p>
     *
     * @param text 待处理的原始文本
     * @return 替换后的安全文本
     */
    public static String replace(String text) {
        if (!initialized || text == null || text.isEmpty()) {
            return text;
        }
        String lowerText = text.toLowerCase();
        char[] result = text.toCharArray(); // 保留原始大小写结果
        int len = text.length();

        // 遍历每个起始位置
        for (int i = 0; i < len; i++) {
            TrieNode current = ROOT;

            for (int j = i; j < len; j++) {
                char c = lowerText.charAt(j);
                TrieNode next = current.children.get(c);
                if (next == null) {
                    break;
                }
                if (next.isEnd) {
                    // 将匹配到的敏感词替换为 *
                    for (int k = i; k <= j; k++) {
                        result[k] = REPLACE_CHAR;
                    }
                    // 跳过已替换的字符，避免内部嵌套重复匹配
                    i = j;
                    break;
                }
                current = next;
            }
        }

        String resultText = new String(result);
        // 如果有替换，记录日志
        if (!resultText.equals(text)) {
            log.info("敏感词替换完成：原文本长度={}, 替换后文本长度={}", text.length(), resultText.length());
        }
        return resultText;
    }

    /**
     * 替换 + 获取替换详情
     *
     * @param text 待处理的文本
     * @return 替换结果封装，包含替换后的文本和命中的敏感词列表
     */
    public static ReplaceResult replaceWithDetail(String text) {
        if (!initialized || text == null || text.isEmpty()) {
            return new ReplaceResult(text, Collections.emptyList());
        }

        List<String> matched = findAll(text);
        String filtered = replace(text);
        return new ReplaceResult(filtered, matched);
    }

    /**
     * 获取敏感词库状态
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取词典中的敏感词数量（仅在初始化后可用）
     */
    public static int dictionarySize() {
        if (!initialized) {
            return 0;
        }
        return countWords(ROOT, "");
    }

    /**
     * 清空词典
     */
    public static void clear() {
        synchronized (SensitiveWordUtil.class) {
            ROOT.children.clear();
            initialized = false;
            log.info("敏感词词典已清空");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * DFA 单次匹配：从文本起始位置查找第一个敏感词
     */
    private static String findFirstMatch(String lowerText) {
        int len = lowerText.length();
        for (int i = 0; i < len; i++) {
            TrieNode current = ROOT;
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < len; j++) {
                char c = lowerText.charAt(j);
                TrieNode next = current.children.get(c);
                if (next == null) {
                    break;
                }
                sb.append(c);
                if (next.isEnd) {
                    return sb.toString();
                }
                current = next;
            }
        }
        return null;
    }

    /**
     * DFA 全量匹配：找出文本中所有敏感词（去重）
     */
    private static List<String> findAllMatches(String lowerText) {
        Set<String> matchedSet = new LinkedHashSet<>();
        int len = lowerText.length();
        for (int i = 0; i < len; i++) {
            TrieNode current = ROOT;
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < len; j++) {
                char c = lowerText.charAt(j);
                TrieNode next = current.children.get(c);
                if (next == null) {
                    break;
                }
                sb.append(c);
                if (next.isEnd) {
                    matchedSet.add(sb.toString());
                    // 找到后不立即break，继续查找更长的匹配
                }
                current = next;
            }
        }
        return new ArrayList<>(matchedSet);
    }

    /**
     * 递归统计 Trie 树中的敏感词数量
     */
    private static int countWords(TrieNode node, String prefix) {
        int count = node.isEnd ? 1 : 0;
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            count += countWords(entry.getValue(), prefix + entry.getKey());
        }
        return count;
    }

    /**
     * 替换结果封装
     */
    public record ReplaceResult(String filteredText, List<String> matchedWords) {
    }
}
