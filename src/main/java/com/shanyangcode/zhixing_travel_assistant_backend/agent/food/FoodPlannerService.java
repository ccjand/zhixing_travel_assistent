package com.shanyangcode.zhixing_travel_assistant_backend.agent.food;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface FoodPlannerService {

    @SystemMessage(fromResource = "docs/sys_agent/food.txt")
    String chat(@MemoryId String sessionId, @UserMessage String prompt);
}