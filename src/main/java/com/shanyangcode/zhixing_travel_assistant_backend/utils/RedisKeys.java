package com.shanyangcode.zhixing_travel_assistant_backend.utils;

public class RedisKeys {
    public final static String PREFIX = "shanyangcode:zhixing:";


    public final static String USER_AUTH_TOKEN_PREFIX = PREFIX + "user:auth:";

    //7天
    public final static Integer USER_AUTH_TOKEN_TIMEOUT = 60 * 60 * 24 * 7;

    public final static String RAG_CACHE_PREFIX = PREFIX + "rag:cache:";


    public static String getUserAuthToken(String uid) {
        return USER_AUTH_TOKEN_PREFIX + uid;
    }

    public static String getRagCacheKey(String digest) {
        return RAG_CACHE_PREFIX + digest;
    }

}
