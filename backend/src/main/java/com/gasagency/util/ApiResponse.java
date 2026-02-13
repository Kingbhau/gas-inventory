package com.gasagency.util;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API Response wrapper for all REST endpoints
 * Provides a consistent response format with message, data, error, timestamp, and requestId
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private T data;
    private ApiError error;
    private String timestamp;
    private String requestId;

    /**
     * Default constructor
     */
    public ApiResponse() {
    }

    /**
     * Constructor for success response with message and data
     *
     * @param message Message describing the operation result
     * @param data    The actual response data payload
     */
    public ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    /**
     * Constructor for simple success response with message only
     *
     * @param message Message describing the operation result
     */
    public ApiResponse(String message) {
        this.message = message;
        this.data = null;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "message='" + message + '\'' +
                ", data=" + data +
                ", error=" + error +
                ", timestamp='" + timestamp + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}

