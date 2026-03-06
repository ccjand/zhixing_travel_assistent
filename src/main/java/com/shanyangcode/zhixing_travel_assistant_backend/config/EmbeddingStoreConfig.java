package com.shanyangcode.zhixing_travel_assistant_backend.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EmbeddingStoreConfig {

//    @Bean
//    public EmbeddingStore<TextSegment> embeddingStore() {
//        //基于内存的向量数组库
//        return new InMemoryEmbeddingStore<>();
//    }

    @Value("${pgvector.host}")
    private String host;

    @Value("${pgvector.port}")
    private Integer port;

    @Value("${pgvector.database}")
    private String database;

    @Value("${pgvector.user}")
    private String user;

    @Value("${pgvector.password}")
    private String password;

    @Value("${pgvector.table}")
    private String table;

    @Value("${pgvector.drop-table-first:false}")
    private Boolean dropTableFirst;

    @Value("${pgvector.create-table:true}")
    private Boolean createTable;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .table(table)
                .dropTableFirst(Boolean.TRUE.equals(dropTableFirst))
                .createTable(Boolean.TRUE.equals(createTable))
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .dimension(1024)
                .database(database)
                .build();
    }


}
