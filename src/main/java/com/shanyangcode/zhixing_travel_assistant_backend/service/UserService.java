package com.shanyangcode.zhixing_travel_assistant_backend.service;

import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.LoginRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.RegisterRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.UserInfo;

public interface UserService {
    void register(RegisterRequest req);

    UserInfo login(LoginRequest req);

    void logout(String userId);
}
