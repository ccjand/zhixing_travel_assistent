package com.shanyangcode.zhixing_travel_assistant_backend.rag;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;

@Component
public class RagQueryAugmenter {

    @Autowired
    private ChatModel chatModel;

    @Value("${rag.multi-query.count:3}")
    private Integer multiQueryCount;

    @Value("${rag.hyde.enabled:false}")
    private Boolean hydeEnabled;

    @Value("${rag.hyde.max-chars:600}")
    private Integer hydeMaxChars;

    @Value("${rag.multi-query.enabled:true}")
    private Boolean multiQueryEnabled;

    List<String> buildQueryVariants(String original) {
        String base = original == null ? "" : original.trim();
        if (base.isBlank()) {
            return List.of();
        }

        ArrayList<String> queries = new ArrayList<>();
        queries.add(base);

        if (Boolean.TRUE.equals(multiQueryEnabled) && multiQueryCount > 0) {
            queries.addAll(rewriteQueries(base, multiQueryCount));
        }

        if (Boolean.TRUE.equals(hydeEnabled)) {
            String hyde = hydeQuery(base, hydeMaxChars);
            queries.add(hyde);
        }

        return dedupeStrings(queries);
    }

    private List<String> rewriteQueries(String query, int count) {
        // 让 LLM 生成若干“更像检索关键词”的短 query，提升召回覆盖率
        String prompt = """
                你是检索查询改写器。
                给定用户问题，生成 %d 条不同的检索查询（尽量短、偏关键词、避免长句）。
                要求：只输出查询本身，每行一条；不要输出序号、不要输出解释。
                用户问题：%s
                """.formatted(count, query);

        String out = safeChat(prompt);
        if (out == null || out.isBlank()) {
            return List.of();
        }

        String[] lines = out.split("\\r?\\n");
        ArrayList<String> cleaned = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String s = line.trim().replaceFirst("^\\s*(?:[-*]|\\d+[.)、])\\s*", "").trim();
            if (!s.isBlank()) {
                cleaned.add(s);
            }
        }
        return dedupeStrings(cleaned);
    }

    private String hydeQuery(String query, int maxChars) {
        String prompt = """
                你是知识库检索辅助器。
                根据用户问题，写一段可能出现在知识库里的“背景说明/答案草稿”，用于向量检索召回。
                要求：只输出正文，不要标题，不要列表，不要解释；长度不超过 %d 字符。
                用户问题：%s
                """.formatted(maxChars, query);

        String out = safeChat(prompt);
        String trimmed = out == null ? "" : out.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.length() > maxChars ? trimmed.substring(0, maxChars) : trimmed;
    }

    private String safeChat(String prompt) {
        // LLM 失败时直接降级，不影响主检索流程
        try {
            String out = chatModel.chat(prompt);
            return out == null ? null : out.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> dedupeStrings(List<String> items) {
        if (items == null) {
            return List.of();
        }

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String s : items) {
            if (s != null) {
                String t = s.trim();
                if (!t.isBlank()) {
                    seen.add(t);
                }
            }
        }
        return new ArrayList<>(seen);
    }
}
