package com.shanyangcode.zhixing_travel_assistant_backend.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.JsonUtil;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.RedisKeys;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.RedisUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RagContentRetriever implements ContentRetriever {

    // Redis 缓存只存 List<String>（每段文本），避免缓存结构复杂化
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    // primary：正常阈值向量检索；fallback：阈值更低，仅在 primary 无结果时兜底
    private ContentRetriever primaryCandidates;
    private ContentRetriever fallbackCandidates;

    @Autowired
    private RagQueryAugmenter queryAugmenter;

    @Autowired
    private RagReranker reranker;

    // ===== 可配置参数（application.yml）=====
    @Value("${rag.max-results:8}")
    private Integer ragMaxResults;

    @Value("${rag.min-score:0.6}")
    private Double ragMinScore;

    @Value("${rag.fallback-min-score:0.45}")
    private Double ragFallbackMinScore;

    @Value("${rag.parent-context.enabled:true}")
    private Boolean ragParentContextEnabled;

    @Value("${rag.parent-context.max-results:4}")
    private Integer ragParentContextMaxResults;

    @Value("${rag.cache.enabled:true}")
    private Boolean ragCacheEnabled;

    @Value("${rag.cache.version:v2}")
    private String ragCacheVersion;

    @Value("${rag.cache.ttl-seconds:3600}")
    private Long ragCacheTtlSeconds;

    @Value("${rag.hybrid.enabled:true}")
    private Boolean ragHybridEnabled;

    @Value("${rag.hybrid.alpha:0.85}")
    private Double ragHybridAlpha;

    @Value("${rag.hybrid.candidate-multiplier:4}")
    private Integer ragHybridCandidateMultiplier;

    // 排序融合策略（对 multi-query 合并后的候选做“统一排序”）：
    // - rrf：Reciprocal Rank Fusion（默认）。把“语义路排序”和“词面路排序”转成名次再融合，
    //        对分数尺度不敏感，multi-query/不同 queryVariant 导致的分数分布不一致时更稳。
    // - hybrid：线性加权。对同一候选同时计算语义分与词面分，按 alpha 融合：
    //        score = alpha * sem + (1 - alpha) * lex
    //        更直观可控，但更依赖两路分数尺度相对可比（否则 alpha 难调）。
    @Value("${rag.rank.mode:rrf}")
    private String ragRankMode;

    // RRF 常量 k：控制名次差异的“平滑程度”
    // - k 越大：头部名次差一点带来的分数差异越小（更稳）
    // - k 越小：更强调头部名次（更激进）
    // 常见默认取 60 左右。
    @Value("${rag.rrf.k:60}")
    private Integer ragRrfK;

    // RRF 融合权重：可以单独调两路贡献
    // - semantic-weight：语义路（dense/embedding 相似度排序）的权重
    // - lexical-weight：词面路（lexicalScore 排序）的权重
    @Value("${rag.rrf.semantic-weight:1.0}")
    private Double ragRrfSemanticWeight;

    @Value("${rag.rrf.lexical-weight:1.0}")
    private Double ragRrfLexicalWeight;

    @Value("${rag.rerank.enabled:false}")
    private Boolean ragRerankEnabled;

    @Value("${rag.rerank.candidates:12}")
    private Integer ragRerankCandidates;

    @Value("${rag.multi-query.max-merged-results:24}")
    private Integer ragMultiQueryMaxMergedResults;

    // parent_id -> parentText：写入发生在知识入库/切分阶段；读取发生在检索后处理阶段
    private final Map<String, String> parentTextById = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 这里先做“向量候选召回”，maxResults 会比最终返回数量大：
        // - multi-query 会把多个 query 的结果合并
        // - hybrid/rerank 只改变排序，需要足够候选才有效
        int candidateMaxResults = Math.max(ragMaxResults * ragHybridCandidateMultiplier, ragMultiQueryMaxMergedResults);

        this.primaryCandidates = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(candidateMaxResults)
                .minScore(ragMinScore)
                .build();

        this.fallbackCandidates = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(candidateMaxResults)
                .minScore(ragFallbackMinScore)
                .build();
    }

    public void putParentText(String parentId, String parentText) {
        // 数据准备阶段把父段文本放进内存索引；检索阶段才可以按 parent_id 回填父段
        if (parentId == null || parentId.isBlank() || parentText == null || parentText.isBlank()) {
            return;
        }
        parentTextById.put(parentId, parentText);
    }

    @Override
    public List<Content> retrieve(Query query) {
        // 主流程：
        // 1) 读缓存（命中直接返回）
        // 2) 候选召回（multi-query/hyde -> 向量）
        // 3) 排序（hybrid -> rerank）
        // 4) 后处理（父段补齐 + 裁剪）
        // 5) 写缓存（只缓存最终文本）
        String cacheKey = ragCacheEnabled ? cacheKey(query) : null;
        if (ragCacheEnabled) {
            try {
                String json = RedisUtil.get(cacheKey);
                if (json != null) {
                    List<String> texts = JsonUtil.toObj(json, STRING_LIST);
                    if (texts != null && !texts.isEmpty()) {
                        ArrayList<Content> out = new ArrayList<>(texts.size());
                        for (String s : texts) {
                            out.add(Content.from(TextSegment.from(s, new Metadata())));
                        }
                        return out;
                    }
                }
            } catch (Exception ignored) {
                // 缓存层任何异常都不影响主检索链路
            }
        }

        List<Content> result = retrieveOnce(primaryCandidates, query);
        if (result.isEmpty() && ragFallbackMinScore < ragMinScore) {
            result = retrieveOnce(fallbackCandidates, query);
        }

        if (ragCacheEnabled && !result.isEmpty()) {
            try {
                List<String> texts = result.stream().map(c -> c.textSegment().text()).toList();
                RedisUtil.set(cacheKey, texts, ragCacheTtlSeconds);
            } catch (Exception ignored) {
                // 同上：缓存失败不影响主链路
            }
        }
        return result;
    }

    private List<Content> retrieveOnce(ContentRetriever candidatesRetriever, Query query) {
        // 1) 生成查询变体（multi-query / hyde），扩大召回覆盖
        List<String> queryVariants = queryAugmenter.buildQueryVariants(query.text());

        // 2) 对每个变体做向量召回，合并为候选池（达到上限就停止）
        int mergedLimit = Math.max(ragMaxResults, ragMultiQueryMaxMergedResults);
        ArrayList<Content> candidates = new ArrayList<>(mergedLimit);
        for (String qText : queryVariants) {
            Query q = query.metadata() == null ? Query.from(qText) : Query.from(qText, query.metadata());
            candidates.addAll(candidatesRetriever.retrieve(q));
            if (candidates.size() >= mergedLimit) {
                break;
            }
        }

        String originalQuery = query.text().trim();
        String mode = ragRankMode == null ? "rrf" : ragRankMode.trim();
        List<Content> merged;
        if ("rrf".equalsIgnoreCase(mode)) {
            // RRF：把“语义排序”和“词面排序”分别转为名次，再做融合。
            // 适合 multi-query 后候选分数分布不一致的情况（更稳、更不吃分数尺度）。
            merged = RagScorer.rrfRank(
                    originalQuery,
                    candidates,
                    ragHybridEnabled,
                    ragRrfK,
                    ragRrfSemanticWeight,
                    ragRrfLexicalWeight,
                    mergedLimit
            );
        } else {
            // Hybrid：对同一候选池同时算语义分与词面分，用 alpha 做线性融合。
            // 更可控，但依赖两路分数尺度相对可比。
            merged = RagScorer.hybridRank(originalQuery, candidates, ragHybridEnabled, ragHybridAlpha, mergedLimit);
        }

        // 4) Rerank：用 LLM 在 TopN 里再排一遍，只改变顺序
        if (ragRerankEnabled) {
            merged = reranker.rerank(originalQuery, merged, ragRerankCandidates);
        }

        // 5) 裁剪到最终条数 +（可选）父段补齐
        List<Content> children = merged.size() > ragMaxResults
                ? new ArrayList<>(merged.subList(0, ragMaxResults))
                : new ArrayList<>(merged);

        if (!ragParentContextEnabled) {
            return children;
        }
        return applyParentContext(children, ragMaxResults);
    }

    private String cacheKey(Query query) {
        // cacheKey = RedisKeys 前缀 + md5(版本|query|metadata)
        // version 用于隔离不同参数/策略下的缓存（手动 bump version 即可整体失效）
        Object meta = query.metadata();
        String mode = ragRankMode == null ? "hybrid" : ragRankMode.trim().toLowerCase();
        String base = ragCacheVersion.trim() + "|" + mode + "|" + query.text().trim() + "|" + (meta == null ? "" : meta);
        String digest = md5Hex(base);
        return RedisKeys.getRagCacheKey(digest);
    }

    private String md5Hex(String s) {
        // 仅用于生成稳定短 key（无安全诉求）
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Content> applyParentContext(List<Content> childContents, int maxTotal) {
        // 父段补齐是“增强上下文连续性”的 best-effort：
        // - 保留子段（更精确）
        // - 额外补充少量父段（更完整）
        // - 总条数不超过 rag.max-results
        int parentBudget = ragParentContextMaxResults;
        if (parentBudget <= 0 || childContents.isEmpty()) {
            return childContents;
        }

        ArrayList<Content> merged = new ArrayList<>(childContents);
        Set<String> added = new HashSet<>();

        for (Content content : childContents) {
            if (merged.size() >= maxTotal || parentBudget <= 0) {
                break;
            }
            String parentId = content.textSegment().metadata().getString("parent_id");
            if (parentId == null || parentId.isBlank()) {
                continue;
            }
            if (!added.add(parentId)) {
                continue;
            }
            String parentText = parentTextById.get(parentId);
            if (parentText == null || parentText.isBlank()) {
                continue;
            }
            Metadata metadata = content.textSegment().metadata().copy().put("chunk_type", "parent");
            merged.add(Content.from(TextSegment.from(parentText, metadata)));
            parentBudget--;
        }

        if (merged.size() > maxTotal) {
            return new ArrayList<>(merged.subList(0, maxTotal));
        }
        return merged;
    }

}
