package com.shanyangcode.zhixing_travel_assistant_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.zhixing_travel_assistant_backend.cache.UserCache;
import com.shanyangcode.zhixing_travel_assistant_backend.enums.CommonError;
import com.shanyangcode.zhixing_travel_assistant_backend.exception.BusinessException;
import com.shanyangcode.zhixing_travel_assistant_backend.mapper.UserMapper;
import com.shanyangcode.zhixing_travel_assistant_backend.model.User;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.LoginRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.RegisterRequest;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.UserInfo;
import com.shanyangcode.zhixing_travel_assistant_backend.service.UserService;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.AuthUtil;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.BCryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @Author ccj
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private UserCache userCache;

    @Override
    public void register(RegisterRequest req) {
        //查找是否已经注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, req.getEmail());
        User one = this.getOne(queryWrapper);
        if (one != null) {
            throw new BusinessException(CommonError.USER_ALREADY_EXISTS.getCode(), "用户已存在");
        }

        //注册
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(req.getEmail())
                .username(req.getUsername())
                .passwordHash(BCryptUtil.encode(req.getPassword()))//密码加密
                .build();

        this.save(user);
    }


    @Override
    public UserInfo login(LoginRequest req) {
        //查找是否已经注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, req.getEmail());
        User one = this.getOne(queryWrapper);
        if (one == null) {
            throw new BusinessException(CommonError.USER_NOT_EXISTS.getCode(), "用户不存在");
        }

        if (!BCryptUtil.check(req.getPassword(), one.getPasswordHash())) {
            throw new BusinessException(CommonError.LOGIN_ERROR.getCode(), "密码错误");
        }

        //生成登录凭证
        String token = authUtil.createToken(one.getId());
        //缓存
        UserInfo userInfo = new UserInfo(one.getId(), one.getUsername(), one.getEmail(), one.getPreferences(), token);
        userCache.saveUserInfo(userInfo);

        return userInfo;
    }

    @Override
    public void logout(String userId) {
        userCache.removeUserInfo(userId);
    }
}
