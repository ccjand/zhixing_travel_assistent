package com.shanyangcode.zhixing_travel_assistant_backend.config;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.state.TravelWorkflowState;
import org.bsc.langgraph4j.checkpoint.PostgresSaver;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.sql.SQLException;

/**
 * @Author ccj
 * @Description
 */
@Configuration
public class CheckpointConfig {
    @Value("${pgcheckpoint.host}")
    private String host;

    @Value("${pgcheckpoint.username}")
    private String username;

    @Value("${pgcheckpoint.password}")
    private String password;

    @Value("${pgcheckpoint.database}")
    private String pgCheckpointDatabase;

    @Value("${pgcheckpoint.port}")
    private Integer port;

    @Bean
    public PostgresSaver checkpointSaver() throws SQLException {
        return PostgresSaver.builder()
                .host(host)
                .port(port)
                .database(pgCheckpointDatabase)
                .user(username)
                .password(password)
                .stateSerializer(new ObjectStreamStateSerializer<>(TravelWorkflowState::new))
                .createTables(false) //后续启动必须禁用，第一次启动开启
                .build();
    }

}
