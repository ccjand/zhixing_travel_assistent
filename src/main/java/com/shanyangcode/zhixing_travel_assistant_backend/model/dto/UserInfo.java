package com.shanyangcode.zhixing_travel_assistant_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String id;

    private String username;

    private String email;

    private String preferences;

    private String token;
}
