package com.eam.assetcenter.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(true, message, data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<T>(false, message, null);
    }
}


