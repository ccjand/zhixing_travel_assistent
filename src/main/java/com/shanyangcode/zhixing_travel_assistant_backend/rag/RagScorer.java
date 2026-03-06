package com.shanyangcode.zhixing_travel_assistant_backend.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RagScorer {

    /**
     * 对候选片段进行打分并排序，返回 Top-N。
     * 打分逻辑：
     * - 语义分（sem）：来自向量召回阶段写入的 {@link ContentMetadata#SCORE}
     * - 词面分（lex）：轻量关键词命中度（包含匹配 + token 命中比例）
     * - Hybrid 融合：score = alpha * sem + (1 - alpha) * lex
     * 注意：
     * - 本方法会把融合后的 score 覆盖写回到 {@link ContentMetadata#SCORE}，后续排序/裁剪都基于该值
     * - alpha 会被 clamp 到 [0,1]，limit 至少为 1
     */
    static List<Content> hybridRank(String query, List<Content> candidates, boolean hybridEnabled, double alpha, int limit) {
        if (candidates == null) {
            return List.of();
        }

        int cap = Math.max(1, limit);
        double a = clamp01(alpha);

        ArrayList<Content> scored = new ArrayList<>(candidates.size());
        for (Content c : candidates) {
            // 向量召回分数（语义相关性），由上游 retriever 写入 metadata。
            double sem = semanticScore(c);
            // 轻量词面分数：用于弥补向量召回在专有名词/数字/短查询上的不稳定。
            double lex = hybridEnabled ? lexicalScore(query, c.textSegment().text()) : 0.0;
            // Hybrid 融合：alpha 越大越偏向语义分，越小越偏向词面分。
            double s = hybridEnabled ? a * sem + (1.0 - a) * lex : sem;
            // 把融合后的分数写回 ContentMetadata.SCORE，便于统一排序与后续裁剪。
            scored.add(withScore(c, s));
        }

        // 这里按 metadata 中的 SCORE 倒序排序；该值已被 withScore 覆盖为最终融合分数。
        scored.sort((x, y) -> Double.compare(semanticScore(y), semanticScore(x)));
        return scored.size() <= cap ? scored : scored.subList(0, cap);
    }

    /**
     * 读取候选片段的“语义相关性分数”。
     * <p>
     * 约定：
     * - 向量检索阶段会把相似度分数写入 {@link ContentMetadata#SCORE}
     * - {@link #hybridRank(String, List, boolean, double, int)} 内部会覆盖该字段为融合后的分数
     */
    static double semanticScore(Content c) {
        Object s = c.metadata().get(ContentMetadata.SCORE);
        return s instanceof Number n ? n.doubleValue() : 0.0;
    }

    /**
     * 复制一份 Content，并把分数写回到 metadata（覆盖 ContentMetadata.SCORE）。
     * 这样后续排序与裁剪可以统一基于 metadata 的 SCORE 进行。
     */
    private static Content withScore(Content c, double score) {
        Map<ContentMetadata, Object> meta = new HashMap<>(c.metadata());
        meta.put(ContentMetadata.SCORE, score);
        return Content.from(c.textSegment(), meta);
    }

    /**
     * 轻量词面匹配得分，返回 [0,1]。
     * 规则：
     * - 若 text 直接包含 query（归一化后），返回 1
     * - 否则把 query 拆成 tokens，计算 tokens 在 text tokens 中的命中比例
     * 说明：
     * - 这是一个非常轻量的“关键词补偿”，不是全文检索，也不引入额外依赖
     */
    private static double lexicalScore(String query, String text) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }

        String q = normalizeForMatch(query);
        String t = normalizeForMatch(text);

        // 直接包含命中（对短 query、专有名词很有效）。
        if (t.contains(q)) {
            return 1.0;
        }

        List<String> qTokens = tokens(q);
        Set<String> tTokens = new HashSet<>(tokens(t));
        int hit = 0;
        for (String token : qTokens) {
            if (tTokens.contains(token)) {
                hit++;
            }
        }
        return Math.min(1.0, (double) hit / (double) qTokens.size());
    }

    /**
     * 归一化：去首尾空格 + 小写化，便于做 contains 与 token 比较。
     */
    private static String normalizeForMatch(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    /**
     * 将文本拆分为 tokens：
     * - 优先按“非字母/数字”分割，保留长度 >= 2 的 token
     * - 若拆分后为空（例如中文/无分隔符短文本），回退到“相邻 2 字符 bigram”作为 token
     */
    private static List<String> tokens(String s) {
        ArrayList<String> tokens = new ArrayList<>();
        String cleaned = s.trim();
        if (cleaned.isEmpty()) {
            return tokens;
        }

        for (String t : cleaned.split("[^\\p{L}\\p{N}]+")) {
            if (t.length() >= 2) tokens.add(t);
        }
        if (!tokens.isEmpty()) {
            return tokens;
        }

        // 回退策略：用 bigram 近似 token，提升中文场景的词面命中能力。
        for (int i = 0; i < cleaned.length() - 1; i++) {
            char a = cleaned.charAt(i);
            char b = cleaned.charAt(i + 1);
            if (Character.isWhitespace(a) || Character.isWhitespace(b)) {
                continue;
            }
            tokens.add("" + a + b);
        }
        return tokens;
    }

    /**
     * 将值限制在 [0,1]。
     * 用于保证 alpha 合法，避免融合权重异常导致分数失真。
     */
    private static double clamp01(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }
}