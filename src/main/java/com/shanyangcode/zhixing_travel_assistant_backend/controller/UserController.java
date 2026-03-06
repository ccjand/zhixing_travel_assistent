package com.shanyangcode.zhixing_travel_assistant_backend.controller;

import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.ApiResult;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.LoginRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.RegisterRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.UserInfo;
import com.shanyangcode.zhixing_travel_assistant_backend.service.UserService;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.RequestContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping(value = "/register")
    public ApiResult<?> register(@RequestBody @Valid RegisterRequest req) {
        userService.register(req);
        return ApiResult.success("注册成功");
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserInfo> login(@RequestBody @Valid LoginRequest req) {
        UserInfo userinfo = userService.login(req);
        return ApiResult.success(userinfo);
    }

    @PostMapping(value = "/logout")
    public ApiResult<?> logout() {
        String userId = RequestContext.getUserId();
        userService.logout(userId);
        return ApiResult.success("退出成功");
    }
}
