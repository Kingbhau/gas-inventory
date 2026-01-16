package com.gasagency.util;

/**
 * Generic API Response wrapper for all REST endpoints
 * Provides a consistent response format with success status, message, and data
 */
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /**
     * Default constructor
     */
    public ApiResponse() {
    }

    /**
     * Constructor with all fields
     *
     * @param success Whether the operation was successful
     * @param message Message describing the operation result
     * @param data    The actual response data payload
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * Constructor for success response with message and data
     *
     * @param message Message describing the operation result
     * @param data    The actual response data payload
     */
    public ApiResponse(String message, T data) {
        this.success = true;
        this.message = message;
        this.data = data;
    }

    /**
     * Constructor for simple success response with message only
     *
     * @param message Message describing the operation result
     */
    public ApiResponse(String message) {
        this.success = true;
        this.message = message;
        this.data = null;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

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

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
