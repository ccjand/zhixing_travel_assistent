package com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Requirements implements Serializable {
    private String origin;
    private String destination;
    private String startDate;
    private String people;
    private String special;
}