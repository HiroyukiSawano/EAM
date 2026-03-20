package com.eam.assetcenter.common.exception;

/**
 * 业务异常，表示可预期的领域校验失败。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}





