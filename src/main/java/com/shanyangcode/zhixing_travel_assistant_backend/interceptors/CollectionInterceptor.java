package com.shanyangcode.zhixing_travel_assistant_backend.interceptors;

import com.shanyangcode.zhixing_travel_assistant_backend.enums.CommonError;
import com.shanyangcode.zhixing_travel_assistant_backend.exception.BusinessException;
import com.shanyangcode.zhixing_travel_assistant_backend.model.dto.UserInfo;
import com.shanyangcode.zhixing_travel_assistant_backend.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CollectionInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthUtil authUtil;

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String AUTHORIZATION_SCHEMA = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader(HEADER_AUTHORIZATION);


        if (authorization == null || !authorization.startsWith(AUTHORIZATION_SCHEMA)) {
            throw new BusinessException(CommonError.PARAMS_ERROR.getCode(), "Authorization 不能为空");
        }

        String[] split = authorization.split(" ");
        if (split.length != 2) {
            throw new BusinessException(CommonError.PARAMS_ERROR.getCode(), "Authorization 格式错误");
        }

        String token = split[1];

        //获取对应的uid
        String uid = getValidUid(token);
        if (uid == null) {
            response.setStatus(401);
            return false;
        }

        RequestContext.set(new RequestContextInfo(uid));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //避免内存溢出
        RequestContext.remove();
    }

    public String getValidUid(String token) {
        String uid = authUtil.getUidOrNull(token);
        if (uid == null) {
            return null;
        }

        String key = RedisKeys.getUserAuthToken(uid);
        UserInfo userInfo = RedisUtil.get(key, UserInfo.class);

        //没有登录
        if (userInfo == null) {
            return null;
        }

        //不能用老token来登录
        if (!token.equals(userInfo.getToken())) {
            return null;
        }

        return uid;
    }
}
