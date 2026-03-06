package com.shanyangcode.zhixing_travel_assistant_backend;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = RedisEmbeddingStoreAutoConfiguration.class)
@MapperScan("com.shanyangcode.zhixing_travel_assistant_backend.mapper")
public class ZhixingTravelAssistantBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhixingTravelAssistantBackendApplication.class, args);
    }

}
