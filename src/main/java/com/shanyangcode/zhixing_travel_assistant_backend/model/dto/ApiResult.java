package com.shanyangcode.zhixing_travel_assistant_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shanyangcode.zhixing_travel_assistant_backend.enums.CommonError;
import lombok.Data;

/**
 * 基础返回体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResult<T> {

    /**
     * 成功标识true or false
     */
    private Boolean success;

    /**
     * 错误码
     */
    private Integer errCode;

    /**
     * 错误消息
     */
    private String errMsg;

    /**
     * 返回数据对象
     */
    private T data;

    public static <T> ApiResult<T> success() {
        ApiResult<T> result = new ApiResult<T>();
        result.setData(null);
        result.setSuccess(Boolean.TRUE);
        return result;
    }

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<T>();
        result.setData(data);
        result.setSuccess(Boolean.TRUE);
        return result;
    }

    public static <T> ApiResult<T> fail(Integer code, String msg) {
        ApiResult<T> result = new ApiResult<T>();
        result.setSuccess(Boolean.FALSE);
        result.setErrCode(code);
        result.setErrMsg(msg);
        return result;
    }

    public static <T> ApiResult<T> fail(CommonError error) {
        ApiResult<T> result = new ApiResult<T>();
        result.setSuccess(Boolean.FALSE);
        result.setErrCode(error.getCode());
        result.setErrMsg(error.getMessage());
        return result;
    }


    public boolean isSuccess() {
        return this.success;
    }
}
