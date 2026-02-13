package com.gasagency.security;

import com.gasagency.service.CustomUserDetailsService;
import com.gasagency.util.JwtUtil;
import com.gasagency.util.LoggerUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            final String authorizationHeader = request.getHeader("Authorization");
            String username = null;
            String jwt = null;

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                username = jwtUtil.extractUsername(jwt);
                logger.debug("JWT_TOKEN_FOUND | method=header | username={}", username);
            } else if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("jwt_token".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        username = jwtUtil.extractUsername(jwt);
                        logger.debug("JWT_TOKEN_FOUND | method=cookie | username={}", username);
                    }
                }
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsernameForJwt(username);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    LoggerUtil.logAudit("AUTHENTICATION_SUCCESS", "JWT_VALIDATION",
                            "username", username, "ip", request.getRemoteAddr());
                    logger.info("AUTHENTICATION_SUCCESS | username={} | ip={}", username, request.getRemoteAddr());
                } else {
                    LoggerUtil.logAudit("AUTHENTICATION_FAILED", "JWT_VALIDATION",
                            "username", username, "reason", "token_invalid", "ip", request.getRemoteAddr());
                    logger.warn("AUTHENTICATION_FAILED | username={} | reason=token_invalid | ip={}",
                            username, request.getRemoteAddr());
                }
            } else if (username == null) {
                logger.debug("NO_JWT_TOKEN_FOUND | uri={} | method={}",
                        request.getRequestURI(), request.getMethod());
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("JWT_FILTER_ERROR | exception={} | message={}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            LoggerUtil.logException(logger, "JWT Filter processing failed", e,
                    "uri", request.getRequestURI(), "method", request.getMethod());
            throw new ServletException("JWT Filter error", e);
        }
    }
}

