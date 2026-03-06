package com.shanyangcode.zhixing_travel_assistant_backend.rag;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RagReranker {

    // 从模型输出里提取数字序号（尽量容错，不强依赖严格 JSON）。
    private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

    @Autowired
    private ChatModel chatModel;

    // 用大模型对候选片段做“更相关的排前面”的重排。
    // 只重排前 topN 个，失败就回退到原顺序，topN 之外保持不动。
    List<Content> rerank(String query, List<Content> contents, int topN) {
        if (contents.size() < 2) {
            return contents;
        }

        // 本次实际参与重排的数量：至少 1，最多不超过候选总数。
        int n = Math.min(contents.size(), Math.max(1, topN));

        // 只把前 n 个交给模型重排，避免 prompt 太长、成本太高。
        String prompt = buildPrompt(query, contents.subList(0, n));
        String out = safeChat(prompt);
        if (out == null || out.isBlank()) {
            // 模型调用失败或空输出，直接回退。
            return contents;
        }

        // 从模型输出中解析出排序下标（从 1 开始），解析不到就回退。
        List<Integer> order = parseIndices(out, n);
        if (order.isEmpty()) {
            return contents;
        }

        ArrayList<Content> reranked = new ArrayList<>(contents.size());
        boolean[] used = new boolean[n];
        for (Integer idx : order) {
            int i = idx - 1;
            if (i >= 0 && i < n && !used[i]) {
                used[i] = true;
                reranked.add(contents.get(i));
            }
        }

        // 模型可能漏掉某些下标，把漏掉的按原顺序补齐，保证数量不变。
        for (int i = 0; i < n; i++) {
            if (!used[i]) {
                reranked.add(contents.get(i));
            }
        }

        // topN 之外的不参与重排，保持原顺序拼接到末尾。
        if (contents.size() > n) {
            reranked.addAll(contents.subList(n, contents.size()));
        }
        return reranked;
    }

    private String buildPrompt(String query, List<Content> candidates) {
        StringBuilder sb = new StringBuilder("""
                你是检索结果重排序器。
                给定用户问题和候选片段，按“与问题的相关性”从高到低排序。
                要求：只输出 JSON 数组，内容为候选序号（从 1 开始），例如：[3,1,2]。不要输出任何解释。
                用户问题：""");
        sb.append(query).append("\n\n候选片段：\n");

        for (int i = 0; i < candidates.size(); i++) {
            String t = candidates.get(i).textSegment().text().trim();
            if (t.length() > 380) {
                // 截断候选文本，避免 prompt 太长。
                t = t.substring(0, 380);
            }
            sb.append(i + 1).append(". ").append(t).append("\n\n");
        }
        return sb.toString();
    }

    private List<Integer> parseIndices(String s, int maxIndex) {
        Matcher m = INT_PATTERN.matcher(s);
        ArrayList<Integer> out = new ArrayList<>();
        boolean[] seen = new boolean[maxIndex + 1];
        while (m.find()) {
            int v = Integer.parseInt(m.group());
            if (v >= 1 && v <= maxIndex && !seen[v]) {
                seen[v] = true;
                out.add(v);
            }
        }
        return out;
    }

    private String safeChat(String prompt) {
        try {
            String out = chatModel.chat(prompt);
            return out == null ? null : out.trim();
        } catch (Exception e) {
            // 出错就返回 null，让上层回退到原顺序。
            return null;
        }
    }

}
