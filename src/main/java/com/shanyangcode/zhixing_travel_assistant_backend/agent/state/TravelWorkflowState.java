package com.shanyangcode.zhixing_travel_assistant_backend.agent.state;

import com.shanyangcode.zhixing_travel_assistant_backend.agent.requirement.Requirements;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.HashMap;
import java.util.Map;

public class TravelWorkflowState extends AgentState {
    public static final String KEY_ORIGIN = "origin"; //出发地
    public static final String KEY_DESTINATION = "destination"; //目的地
    public static final String KEY_START_DATE = "startDate"; //出发日期
    public static final String KEY_PEOPLE = "people"; //人数
    public static final String KEY_SPECIAL = "special"; //特殊要求


    /**
     * 形如：
     * requirements: {
     * origin: "北京",
     * destination: "上海",
     * start_date: "2023-01-01 11:20",
     * people: "2",
     * special: "无"
     * }
     */
    public static final Map<String, Channel<?>> scheme = new HashMap<>();

    static {
        scheme.put("sessionId", Channels.base(() -> ""));
        scheme.put("userText", Channels.base(() -> ""));
        scheme.put("requirements", Channels.base(Requirements::new));
        scheme.put("nextStep", Channels.base(() -> "askMissing"));
        scheme.put("drivingTransportOutput", Channels.base(() -> ""));
        scheme.put("flightTransportOutput", Channels.base(() -> ""));
        scheme.put("trainTransportOutput", Channels.base(() -> ""));
        scheme.put("accommodationOutput", Channels.base(() -> ""));
        scheme.put("transportOutput", Channels.base(() -> ""));
        scheme.put("foodOutput", Channels.base(() -> ""));
        scheme.put("output", Channels.base(() -> ""));
        scheme.put("missing", Channels.base(() -> true));
        scheme.put("firstMissing", Channels.base(() -> ""));
    }


    public TravelWorkflowState(Map<String, Object> data) {
        super(data);
    }


    public String drivingTransportOutput() {
        return this.<String>value("drivingTransportOutput").orElse("");
    }


    public String flightTransportOutput() {
        return this.<String>value("flightTransportOutput").orElse("");
    }


    public String trainTransportOutput() {
        return this.<String>value("trainTransportOutput").orElse("");
    }

    public String transportOutput() {
        return this.<String>value("transportOutput").orElse("");
    }

    public boolean missing() {
        return this.<Boolean>value("missing").orElse(true);
    }

    public String firstMissing() {
        return this.<String>value("firstMissing").orElse("");
    }

    public String output() {
        return this.<String>value("output").orElse("");
    }

    public String sessionId() {
        return this.<String>value("sessionId").orElse("");
    }

    public String userText() {
        return this.<String>value("userText").orElse("");
    }

    public Requirements requirements() {
        return this.<Requirements>value("requirements").orElse(new Requirements());
    }

    public String nextStep() {
        return this.<String>value("nextStep").orElse("collect");
    }

    public String accommodationOutput() {
        return this.<String>value("accommodationOutput").orElse("");
    }

    public String foodOutput() {
        return this.<String>value("foodOutput").orElse("");
    }

}
