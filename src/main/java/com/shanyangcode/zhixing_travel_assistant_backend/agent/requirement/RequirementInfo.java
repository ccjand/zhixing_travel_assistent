package com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.state.TravelWorkflowState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementInfo implements Serializable {
    private String sessionId;
    private String userText;
    private Boolean missing;
    /**
     * firstMissing的值对应：
     * @see TravelWorkflowState#KEY_ORIGIN
     * @see TravelWorkflowState#KEY_DESTINATION
     * @see TravelWorkflowState#KEY_PEOPLE
     * @see TravelWorkflowState#KEY_SPECIAL
     * @see TravelWorkflowState#KEY_START_DATE
     */
    private String firstMissing;
    private Requirements requirements; //已收集到的需求
}


