package com.shanyangcode.zhixing_travel_assistant_backend.cache;

import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.UserInfo;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.RedisKeys;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.RedisUtil;
import org.springframework.stereotype.Component;

@Component
public class UserCache {

    public void saveUserInfo(UserInfo userInfo) {
        String key = RedisKeys.getUserAuthToken(userInfo.getId());
        RedisUtil.set(key, userInfo, RedisKeys.USER_AUTH_TOKEN_TIMEOUT);
    }

    public void removeUserInfo(String userId) {
        String key = RedisKeys.getUserAuthToken(userId);
        RedisUtil.del(key);
    }
}
