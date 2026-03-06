package com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation.subagents;

import com.alibaba.dashscope.utils.JsonUtils;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.Requirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class DrivingTransportSubAgent {

    @Autowired
    private DrivingTransportSubService drivingTransportSubService;

    public String execute(String sessionId, Requirements requirements) {
        String prompt = JsonUtils.toJson(requirements);
        return drivingTransportSubService.chat(sessionId, prompt);
    }
}
