package com.shanyangcode.zhixing_travel_assistant_backend.agent.transportation;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.Requirements;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransportAssemble extends Requirements implements Serializable {
    private String drivePlan;
    private String trainPlan;
    private String flightPlan;
}
