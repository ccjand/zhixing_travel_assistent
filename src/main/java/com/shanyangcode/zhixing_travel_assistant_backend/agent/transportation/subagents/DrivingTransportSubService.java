package com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface DrivingTransportSubService {

    /**
     * 自驾agent
     */
    @SystemMessage(fromResource = "docs/sys_agent/Driving.txt")
    String chat(@MemoryId String sessionId, @UserMessage String prompt);
}
