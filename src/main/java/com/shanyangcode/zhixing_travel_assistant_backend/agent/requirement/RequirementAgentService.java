package com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RequirementAgentService {

    @SystemMessage(fromResource = "docs/sys_agent/RequirementAgent.txt")
    RequirementInfo chat(@MemoryId String sessionId, @UserMessage String prompt);
}
