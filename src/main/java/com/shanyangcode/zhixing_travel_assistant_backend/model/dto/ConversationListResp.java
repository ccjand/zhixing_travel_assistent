package com.shanyangcode.zhixing_travel_assistant_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationListResp {

    //会话id
    private String id;

    private String title;
}
