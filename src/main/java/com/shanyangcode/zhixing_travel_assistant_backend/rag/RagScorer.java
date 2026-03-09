package com.shanyangcode.zhixing_travel_assistant_backend.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RagScorer {

    private RagScorer() {
    }

    /**
     * 对候选片段进行打分并排序，返回 Top-N。
     * <p>
     * 打分逻辑：
     * - 语义分（sem）：来自向量召回阶段写入的 {@link ContentMetadata#SCORE}
     * - 词面分（lex）：轻量关键词命中度（包含匹配 + token 命中比例）
     * - Hybrid 融合：score = alpha * sem + (1 - alpha) * lex
     * <p>
     * 注意：
     * - 本方法会把融合后的 score 覆盖写回到 {@link ContentMetadata#SCORE}，后续排序/裁剪都基于该值
     * - alpha 会被 clamp 到 [0,1]，limit 至少为 1
     */
    static List<Content> hybridRank(String query, List<Content> candidates, boolean hybridEnabled, double alpha, int limit) {
        if (candidates == null) {
            return List.of();
        }

        // cap：最终最多返回多少条（limit 最小为 1）
        int cap = Math.max(1, limit);
        // a：alpha 的安全值（限制在 [0,1]，避免配置异常导致融合权重失真）
        double a = clamp01(alpha);

        ArrayList<Content> scored = new ArrayList<>(candidates.size());
        for (Content c : candidates) {
            // 向量召回分数（语义相关性），由上游 retriever 写入 metadata。
            double sem = semanticScore(c);
            // 轻量词面分数：用于弥补向量召回在专有名词/数字/短查询上的不稳定。
            double lex = hybridEnabled ? lexicalScore(query, c.textSegment().text()) : 0.0;
            // Hybrid 融合：
            // - alpha 越大越偏向语义分（dense/embedding 相似度）
            // - alpha 越小越偏向词面分（lexical/关键词命中度）
            double s = hybridEnabled ? a * sem + (1.0 - a) * lex : sem;
            // 把融合后的分数写回 ContentMetadata.SCORE，便于统一排序与后续裁剪。
            scored.add(withScore(c, s));
        }

        // 这里按 metadata 中的 SCORE 倒序排序；该值已被 withScore 覆盖为最终融合分数。
        scored.sort((x, y) -> Double.compare(semanticScore(y), semanticScore(x)));
        return scored.size() <= cap ? scored : scored.subList(0, cap);
    }

    /**
     * 使用 RRF（Reciprocal Rank Fusion）对候选片段做排序融合，返回 Top-N。
     * <p>
     * 参数含义：
     * - query：用户原始问题（用于计算词面 lexicalScore）
     * - candidates：候选片段集合（来自向量召回；可能包含 multi-query 合并后的重复项）
     * - lexicalEnabled：是否启用“词面路”（false 时只用语义路排序）
     * - rrfK：RRF 常量 k，控制名次差异的“平滑程度”（k 越大越平滑，越小越强调头部名次）
     * - semanticWeight：语义路权重（dense/embedding 相似度的排名贡献）
     * - lexicalWeight：词面路权重（lexicalScore 排名贡献）
     * - limit：最终返回条数上限
     */
    static List<Content> rrfRank(
            String query,
            List<Content> candidates,
            boolean lexicalEnabled,
            int rrfK,
            double semanticWeight,
            double lexicalWeight,
            int limit
    ) {
        if (candidates == null) {
            return List.of();
        }

        // keyOf：用于“同一段文本”的稳定标识（优先 child_id，其次 parent_id+text，最后退化到 text）
        java.util.function.Function<Content, String> keyOf = c -> {
            String childId = c.textSegment().metadata().getString("child_id");
            if (childId != null && !childId.isBlank()) {
                return childId;
            }
            String parentId = c.textSegment().metadata().getString("parent_id");
            if (parentId != null && !parentId.isBlank()) {
                return parentId + "|" + c.textSegment().text();
            }
            return c.textSegment().text();
        };

        // cap：最终最多返回多少条（limit 最小为 1）
        int cap = Math.max(1, limit);
        // k：RRF 常量，最小为 1（避免除 0）
        int k = Math.max(1, rrfK);
        // wSem：语义路权重，负数按 0 处理（避免配置异常让贡献变为负）
        double wSem = Math.max(0.0, semanticWeight);
        // wLex：词面路权重；如果 lexicalEnabled=false，则强制为 0（相当于关闭词面路）
        double wLex = lexicalEnabled ? Math.max(0.0, lexicalWeight) : 0.0;

        // multi-query 合并会出现重复候选（同一 child 段被多个 query 召回）
        // 这里先按 child_id 去重，并保留语义分更高的那条，避免重复内容把排序挤爆。
        LinkedHashMap<String, Content> bestByKey = new LinkedHashMap<>();
        for (Content c : candidates) {
            String key = keyOf.apply(c);
            Content prev = bestByKey.get(key);
            if (prev == null || semanticScore(c) > semanticScore(prev)) {
                bestByKey.put(key, c);
            }
        }
        List<Content> unique = new ArrayList<>(bestByKey.values());
        if (unique.isEmpty()) {
            return List.of();
        }

        // 语义路：直接沿用向量召回阶段写入的 ContentMetadata.SCORE
        ArrayList<Content> semSorted = new ArrayList<>(unique);

        // semRank：候选 -> 名次（从 1 开始；名次越小越相关）
        HashMap<String, Integer> semRank = new HashMap<>(semSorted.size() * 2);
        // semSorted：按语义分从高到低排序后的列表
        semSorted.sort((x, y) -> Double.compare(semanticScore(y), semanticScore(x)));
        int initRank = 1;
        for (Content c : semSorted) {
            semRank.put(keyOf.apply(c), initRank++);
        }

        // 词面路：用轻量 lexicalScore 做一个“候选池内排序”，再和语义路一起做 RRF 融合
        Map<String, Integer> lexRank = Map.of();
        if (wLex > 0.0) {
            // lexScoreByKey：缓存每个候选的词面分，避免排序 comparator 里重复计算
            HashMap<String, Double> lexScoreByKey = new HashMap<>(unique.size() * 2);
            for (Content c : unique) {
                lexScoreByKey.put(keyOf.apply(c), lexicalScore(query, c.textSegment().text()));
            }

            ArrayList<Content> lexSorted = new ArrayList<>(unique);
            // lexSorted：按词面分从高到低排序；若词面分相同，用语义分做二次排序，减少并列抖动
            lexSorted.sort((a, b) -> {
                double la = lexScoreByKey.getOrDefault(keyOf.apply(a), 0.0);
                double lb = lexScoreByKey.getOrDefault(keyOf.apply(b), 0.0);
                int cmp = Double.compare(lb, la);
                if (cmp != 0) {
                    return cmp;
                }
                return Double.compare(semanticScore(b), semanticScore(a));
            });
            // lexRank：候选 -> 词面路名次（从 1 开始）
            HashMap<String, Integer> tmp = new HashMap<>(lexSorted.size() * 2);
            int rank = 1;
            for (Content c : lexSorted) {
                tmp.put(keyOf.apply(c), rank++);
            }
            lexRank = tmp;
        }

        // RRF 融合：score = sum_i (w_i / (k + rank_i))
        // - k 用于抑制“名次差一点就巨大落差”的情况（k 越大越平滑）
        // - wSem / wLex 允许对两路检索的重要性做单独加权
        ArrayList<Content> fused = new ArrayList<>(unique.size());
        // missingRank：某一路没给出名次时使用的“缺省名次”（放到队尾附近）
        int missingRank = unique.size() + 1;
        for (Content c : unique) {
            String key = keyOf.apply(c);
            // rSem：语义路名次（1 表示最相关）
            int rSem = semRank.getOrDefault(key, missingRank);
            // rLex：词面路名次（1 表示最相关；若词面路关闭则使用缺省名次）
            int rLex = wLex > 0.0 ? lexRank.getOrDefault(key, missingRank) : missingRank;
            // s：融合后的最终分数（会写回 ContentMetadata.SCORE，便于统一排序与后续裁剪）
            double s = 0.0;
            if (wSem > 0.0) {
                s += wSem / (double) (k + rSem);
            }
            if (wLex > 0.0) {
                s += wLex / (double) (k + rLex);
            }
            fused.add(withScore(c, s));
        }

        // 最终按融合分从高到低排序（withScore 已覆盖 ContentMetadata.SCORE）
        fused.sort((x, y) -> Double.compare(semanticScore(y), semanticScore(x)));
        return fused.size() <= cap ? fused : fused.subList(0, cap);
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
     * <p>
     * 规则：
     * - 若 text 直接包含 query（归一化后），返回 1
     * - 否则把 query 拆成 tokens，计算 tokens 在 text tokens 中的命中比例
     * <p>
     * 说明：
     * - 这是一个非常轻量的“关键词补偿”，不是全文检索，也不引入额外依赖
     */
    private static double lexicalScore(String query, String text) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }

        String q = query == null ? "" : query.trim().toLowerCase();
        String t = text == null ? "" : text.trim().toLowerCase();

        // 直接包含命中
        if (t.contains(q)) {
            return 1.0;
        }

        List<String> qTokens = tokens(q);
        Set<String> tTokens = new HashSet<>(tokens(t));
        int hit = 0; //命中了多少个 query 的 token ”的计数器
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
