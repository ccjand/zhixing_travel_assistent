package com.shanyangcode.zhixing_travel_assistant_backend.config;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.accommodation.AccommodationPlannerService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.food.FoodPlannerService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.RequirementAgentService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.TransportPlannerService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.DrivingTransportSubService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.FlightTransportSubService;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents.TrainTransportSubService;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AgentFactory implements ApplicationContextAware {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    @Autowired
    private StreamingChatModel streamingChatModel;

    private ApplicationContext applicationContext;


    public <T> T getAgent(Class<T> agentClass) {
        return applicationContext.getBean(agentClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public AccommodationPlannerService accommodationPlannerService(@Qualifier("toolsAccommodation") McpToolProvider toolsAccommodation) {
        return AiServices.builder(AccommodationPlannerService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("accommodation:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsAccommodation)
                .build();
    }

    @Bean
    public FoodPlannerService foodPlannerService(@Qualifier("toolsFood") McpToolProvider toolsFood) {
        return AiServices.builder(FoodPlannerService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("food:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(1000)
                                .build()
                )
                .toolProvider(toolsFood)
                .build();
    }


    @Bean
    public TransportPlannerService transportPlannerService(@Qualifier("toolsTransportPlanner") McpToolProvider toolsTransportPlanner) {
        return AiServices.builder(TransportPlannerService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("transport:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsTransportPlanner)
                .build();
    }

    @Bean
    public RequirementAgentService requirementAgentService(@Qualifier("toolsRequirement") McpToolProvider toolsRequirement) {
        return AiServices.builder(RequirementAgentService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("requirement:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsRequirement)
                .build();
    }


    @Bean
    public DrivingTransportSubService drivingTransportSubService(@Qualifier("toolsDriving") McpToolProvider toolsDriving) {
        return AiServices.builder(DrivingTransportSubService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("driving:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsDriving)
                .build();
    }


    @Bean
    public FlightTransportSubService flightTransportSubService(@Qualifier("toolsFlight") McpToolProvider toolsFlight) {
        return AiServices.builder(FlightTransportSubService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("flight:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsFlight)
                .build();
    }


    @Bean
    public TrainTransportSubService trainTransportSubService(@Qualifier("toolsTrain") McpToolProvider toolsTrain) {
        return AiServices.builder(TrainTransportSubService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                //memory 和 会话id 存redis
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id("train:" + memoryId)
                                .chatMemoryStore(chatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .toolProvider(toolsTrain)
                .build();
    }
}
