package com.shanyangcode.zhixing_travel_assistant_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "会话不能为空")
    String sessionId;

    @JsonAlias({"message"})
    @NotBlank(message = "发送内容不能为空")
    String prompt;
}
