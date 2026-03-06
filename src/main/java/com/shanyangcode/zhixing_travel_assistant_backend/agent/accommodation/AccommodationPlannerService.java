package com.shanyangcode.zhixing_travel_assistant_backend.agent.accommodation;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AccommodationPlannerService {

    @SystemMessage(fromResource = "docs/sys_agent/AccommodationAgent.txt")
    String chat(@MemoryId String sessionId, @UserMessage String prompt);
}
