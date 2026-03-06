package com.shanyangcode.zhixing_travel_assistant_backend.agent.accommodation;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.Requirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class AccommodationPlannerAgent {
    @Autowired
    private AccommodationPlannerService accommodationPlannerService;

    public String execute(String sessionId, Requirements requirements) {
        String special = Optional.ofNullable(requirements.getSpecial()).orElse("无");
        String prompt = """
                我想查询%s附近的酒店或者住宿信息，人数为%s，入住时间为%s。
                如果没有特殊的话，默认入住时间为一天。
                另外，我有一些特殊要求：%s
                """.formatted(requirements.getDestination(), requirements.getPeople(), requirements.getStartDate(), special);
        return accommodationPlannerService.chat(sessionId, prompt);
    }
}
