package com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation;

import com.alibaba.dashscope.utils.JsonUtils;
import com.shanyangcode.zhixing_travel_assistant_backend.agent.state.TravelWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransportPlannerAgent {

    @Autowired
    private TransportPlannerService transportPlannerService;

    public String execute(String sessionId, TravelWorkflowState state) {

        TransportAssemble transportAssemble = new TransportAssemble();
        BeanUtils.copyProperties(state.requirements(), transportAssemble);
        transportAssemble.setDrivePlan(state.drivingTransportOutput());
        transportAssemble.setFlightPlan(state.flightTransportOutput());
        transportAssemble.setTrainPlan(state.trainTransportOutput());

        String prompt = JsonUtils.toJson(transportAssemble);
        return transportPlannerService.chat(sessionId, prompt);
    }

}
