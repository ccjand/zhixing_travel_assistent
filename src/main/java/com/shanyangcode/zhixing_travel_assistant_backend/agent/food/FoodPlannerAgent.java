package com.shanyangcode.zhixing_travel_assistant_backend.agent.food;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FoodPlannerAgent {

    @Autowired
    private FoodPlannerService foodPlannerService;

    public String execute(String sessionId, String destination) {
        String prompt = """
                帮我查找距离%s最近的美食地点，结果不得多余10条
                """.formatted(destination);

        return foodPlannerService.chat(sessionId, prompt);
    }
}
