package com.gasagency.service;

import com.gasagency.entity.AlertNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE (Server-Sent Events) Service for real-time alert notifications
 * Manages persistent connections with clients and broadcasts alerts
 */
@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Subscribe user to alert stream
     * Each user gets their own SSE emitter connection
     */
    public SseEmitter subscribe(String userId) {
        logger.info("User {} subscribing to alerts", userId);

        // 30 minute timeout - increased from 5 minutes to allow longer connections
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.put(userId, emitter);

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to alert stream")
                    .build());
        } catch (IOException e) {
            logger.warn("Failed to send connection confirmation to user {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        }

        // Start a keep-alive thread to prevent timeout
        startKeepAlive(userId, emitter);

        // Auto cleanup on completion
        emitter.onCompletion(() -> {
            logger.info("User {} SSE completed", userId);
            emitters.remove(userId);
        });

        // Auto cleanup on timeout
        emitter.onTimeout(() -> {
            logger.info("User {} SSE timeout", userId);
            emitters.remove(userId);
        });

        // Auto cleanup on error
        emitter.onError(throwable -> {
            logger.warn("User {} SSE error: {}", userId, throwable.getMessage());
            emitters.remove(userId);
        });

        return emitter;
    }

    /**
     * Send keep-alive messages to prevent connection timeout
     */
    private void startKeepAlive(String userId, SseEmitter emitter) {
        Thread keepAliveThread = new Thread(() -> {
            try {
                while (emitters.containsKey(userId)) {
                    Thread.sleep(60000); // Send keep-alive every 60 seconds
                    try {
                        emitter.send(SseEmitter.event()
                                .name("keep-alive")
                                .data("Connection active")
                                .build());
                    } catch (IOException e) {
                        logger.debug("Failed to send keep-alive to user {}: {}", userId, e.getMessage());
                        break;
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Keep-alive thread interrupted for user {}", userId);
                Thread.currentThread().interrupt();
            }
        });
        keepAliveThread.setDaemon(true);
        keepAliveThread.setName("SSE-KeepAlive-" + userId);
        keepAliveThread.start();
    }

    /**
     * Broadcast alert to all connected clients in real-time
     */
    public void broadcastAlert(AlertNotification alert) {
        logger.debug("Broadcasting alert: {} to {} connected users", alert.getAlertKey(), emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(alert.getId().toString())
                        .name("alert")
                        .data(alert)
                        .build());
            } catch (IOException e) {
                logger.warn("Failed to send alert to user {}: {}", userId, e.getMessage());
                emitters.remove(userId);
            }
        });
    }

    /**
     * Broadcast alert dismissal to all connected clients
     */
    public void broadcastAlertDismissal(Long alertId) {
        logger.debug("Broadcasting alert dismissal: {} to {} connected users", alertId, emitters.size());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("alert-dismissed")
                        .data(alertId)
                        .build());
            } catch (IOException e) {
                logger.warn("Failed to send dismissal notification to user {}: {}", userId, e.getMessage());
                emitters.remove(userId);
            }
        });
    }

    /**
     * Get count of active connections
     */
    public int getActiveConnections() {
        return emitters.size();
    }
}
