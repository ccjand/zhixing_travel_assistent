package com.shanyangcode.zhixing_travel_assistant_backend.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

@Configuration
public class McpToolConfig {
        
    /**
     * 高德地图
     */
    @Bean
    @Lazy
    public McpClient amapMcpClient() {
        StreamableHttpMcpTransport amapTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/fcc6c34909f54b/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("amapMcpClient")
                .transport(amapTransport)
                .build();
    }

    /**
     * 时间
     */
    @Bean
    @Lazy
    public McpClient timeMcpClient() {
        StreamableHttpMcpTransport timeTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/0c208159e4be48/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("timeMcpClient")
                .transport(timeTransport)
                .build();
    }

    /**
     * 天气
     */
    @Bean
    @Lazy
    public McpClient weatherMcpClient() {
        StreamableHttpMcpTransport weatherTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/671622ac838a4a/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("weatherMcpClient")
                .transport(weatherTransport)
                .build();
    }

    /**
     * 飞机
     */
    @Bean
    @Lazy
    public McpClient flightMcpClient() {
        StreamableHttpMcpTransport flightTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/abb08788ac6946/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("flightMcpClient")
                .transport(flightTransport)
                .build();
    }

    /**
     * 酒店
     */
    @Bean
    @Lazy
    public McpClient hotelMcpClient() {
        StreamableHttpMcpTransport hotelTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/f55b087b2bb540/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("hotelMcpClient")
                .transport(hotelTransport)
                .build();
    }

    /**
     * 高铁
     */
    @Bean
    @Lazy
    public McpClient trainMcpClient() {
        StreamableHttpMcpTransport trainTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/3ac0805ba0be42/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("trainMcpClient")
                .transport(trainTransport)
                .build();
    }

    /**
     * 必应搜索
     */
    @Bean
    @Lazy
    public McpClient biyingSearchMcpClient() {
        StreamableHttpMcpTransport trainTransport = StreamableHttpMcpTransport.builder()
                .url("https://mcp.api-inference.modelscope.net/432ea453232742/mcp")
                .build();
        return new DefaultMcpClient.Builder()
                .key("biyingSearchMcpClient")
                .transport(trainTransport)
                .build();
    }


    @Bean
    public McpToolProvider mcpToolProvider(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient weatherMcpClient,
            McpClient flightMcpClient,
            McpClient hotelMcpClient,
            McpClient trainMcpClient,
            McpClient biyingSearchMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, biyingSearchMcpClient, amapMcpClient, weatherMcpClient, flightMcpClient, hotelMcpClient, trainMcpClient)
                .build();
    }

    @Bean("toolsNormal")
    public McpToolProvider toolsNormal(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient weatherMcpClient,
            McpClient flightMcpClient,
            McpClient hotelMcpClient,
            McpClient trainMcpClient,
            McpClient biyingSearchMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, weatherMcpClient, flightMcpClient, hotelMcpClient, trainMcpClient, biyingSearchMcpClient)
                .build();
    }

    @Bean("toolsRequirement")
    public McpToolProvider toolsRequirement(
            McpClient timeMcpClient,
            McpClient weatherMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, weatherMcpClient)
                .build();
    }

    @Bean("toolsAccommodation")
    public McpToolProvider toolsAccommodation(
            McpClient timeMcpClient,
            McpClient hotelMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, hotelMcpClient)
                .build();
    }

    @Bean("toolsFood")
    public McpToolProvider toolsFood(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient weatherMcpClient,
            McpClient biyingSearchMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, weatherMcpClient, biyingSearchMcpClient)
                .build();
    }

    @Bean("toolsDriving")
    public McpToolProvider toolsDriving(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient weatherMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, weatherMcpClient)
                .build();
    }

    @Bean("toolsFlight")
    public McpToolProvider toolsFlight(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient flightMcpClient,
            McpClient weatherMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, flightMcpClient, weatherMcpClient)
                .build();
    }

    @Bean("toolsTrain")
    public McpToolProvider toolsTrain(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient trainMcpClient,
            McpClient weatherMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, trainMcpClient, weatherMcpClient)
                .build();
    }

    @Bean("toolsTransportPlanner")
    public McpToolProvider toolsTransportPlanner(
            McpClient timeMcpClient,
            McpClient amapMcpClient,
            McpClient weatherMcpClient
    ) {
        return McpToolProvider.builder()
                .mcpClients(timeMcpClient, amapMcpClient, weatherMcpClient)
                .build();
    }
}
