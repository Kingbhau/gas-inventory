package com.gasagency.util;

/**
 * Standard error payload for API responses.
 */
public class ApiError {
    private String code;
    private String details;

    public ApiError() {
    }

    public ApiError(String code, String details) {
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

