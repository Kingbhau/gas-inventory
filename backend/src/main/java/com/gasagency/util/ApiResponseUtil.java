package com.gasagency.util;

import org.slf4j.MDC;
import org.springframework.data.domain.Page;

import com.gasagency.dto.response.PagedResponseDTO;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ApiResponseUtil {
    private ApiResponseUtil() {
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(message, data);
        applyMeta(response);
        return response;
    }

    public static <T> ApiResponse<PagedResponseDTO<T>> success(String message, Page<T> page) {
        PagedResponseDTO<T> paged = new PagedResponseDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast());
        ApiResponse<PagedResponseDTO<T>> response = new ApiResponse<>(message, paged);
        applyMeta(response);
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>(message, null);
        applyMeta(response);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        ApiResponse<T> response = new ApiResponse<>(message, null);
        response.setError(new ApiError(code, message));
        applyMeta(response);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, String code, String details) {
        ApiResponse<T> response = new ApiResponse<>(message, null);
        response.setError(new ApiError(code, details));
        applyMeta(response);
        return response;
    }

    private static void applyMeta(ApiResponse<?> response) {
        response.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC).toString());
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            response.setRequestId(requestId);
        }
    }
}

