package com.gasagency.controller;

import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CsrfController {

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<String>> getCsrfToken(CsrfToken csrfToken) {
        // Touch token to ensure CookieCsrfTokenRepository writes the cookie
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return ResponseEntity.ok(ApiResponseUtil.success("CSRF token initialized"));
    }
}
