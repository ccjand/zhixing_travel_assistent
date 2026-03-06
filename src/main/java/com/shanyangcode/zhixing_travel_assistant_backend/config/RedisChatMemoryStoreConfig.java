package com.shanyangcode.zhixing_travel_assistant_backend.config;

import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@Data
public class RedisChatMemoryStoreConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.timeout}")
    private long ttl;

    @Bean
    public JedisPooled jedisPooled() {
        HostAndPort hostAndPort = new HostAndPort(host, port);
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder().user("default");
        if (password != null && !password.isBlank()) {
            configBuilder.password(password);
        }
        return new JedisPooled(hostAndPort, configBuilder.build(), new GenericObjectPoolConfig<Connection>());
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public ChatMemoryStore chatMemoryStore(JedisPooled jedisPooled) {
        return new JedisChatMemoryStore(jedisPooled, ttl);
    }

    static class JedisChatMemoryStore implements ChatMemoryStore {

        private final JedisPooled jedisPooled;
        private final long ttlSeconds;
        private final ChatMessageJsonCodec chatMessageJsonCodec = new JacksonChatMessageJsonCodec();

        JedisChatMemoryStore(JedisPooled jedisPooled, long ttlSeconds) {
            this.jedisPooled = jedisPooled;
            this.ttlSeconds = ttlSeconds;
        }

        @Override
        public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
            String json;
            try {
                json = jedisPooled.get(key(memoryId));
            } catch (Exception e) {
                return Collections.emptyList();
            }
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            try {
                return normalizeMessages(chatMessageJsonCodec.messagesFromJson(json));
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }

        @Override
        public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
            String json = chatMessageJsonCodec.messagesToJson(normalizeMessages(messages));
            String key = key(memoryId);
            try {
                jedisPooled.set(key, json);
                if (ttlSeconds > 0) {
                    jedisPooled.expire(key, ttlSeconds);
                }
            } catch (Exception e) {
                return;
            }
        }

        @Override
        public void deleteMessages(Object memoryId) {
            try {
                jedisPooled.del(key(memoryId));
            } catch (Exception e) {
                return;
            }
        }

        private static String key(Object memoryId) {
            return "chat_memory:" + String.valueOf(memoryId);
        }

        private static List<dev.langchain4j.data.message.ChatMessage> normalizeMessages(
                List<dev.langchain4j.data.message.ChatMessage> messages
        ) {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }

            ArrayList<dev.langchain4j.data.message.ChatMessage> cleaned = new ArrayList<>(messages.size());

            for (dev.langchain4j.data.message.ChatMessage message : messages) {
                if (message == null) {
                    continue;
                }
                if (message instanceof dev.langchain4j.data.message.SystemMessage) {
                    continue;
                }
                cleaned.add(message);
            }

            int firstUserIndex = -1;
            for (int i = 0; i < cleaned.size(); i++) {
                if (cleaned.get(i) instanceof dev.langchain4j.data.message.UserMessage) {
                    firstUserIndex = i;
                    break;
                }
            }

            if (firstUserIndex < 0) {
                return Collections.emptyList();
            }

            return new ArrayList<>(cleaned.subList(firstUserIndex, cleaned.size()));
        }
    }
}
