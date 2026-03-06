package com.shanyangcode.zhixing_travel_assistant_backend.utils;

public class RequestContext {

    private final static ThreadLocal<RequestContextInfo> threadLocal = new ThreadLocal<>();

    public static void set(RequestContextInfo requestHolderInfo) {
        threadLocal.set(requestHolderInfo);
    }


    public static RequestContextInfo get() {
        return threadLocal.get();
    }

    public static String getUserId() {
        return threadLocal.get().getUserId();
    }

    public static void remove() {
        threadLocal.remove();
    }

}
