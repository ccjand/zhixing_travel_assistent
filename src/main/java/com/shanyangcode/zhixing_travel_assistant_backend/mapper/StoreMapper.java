package com.shanyangcode.zhixing_travel_assistant_backend.mapper;

import org.apache.ibatis.annotations.Param;

public interface StoreMapper {

    int upsert(@Param("namespace") String[] namespace,
               @Param("key") String key,
               @Param("jsonValue") String jsonValue);
}
