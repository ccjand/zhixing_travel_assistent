package com.shanyangcode.zhixing_travel_assistant_backend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author ccj
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException {

    protected Integer errorCode;
    protected String errorMsg;


    public BusinessException(Integer errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

}
