package com.fablab.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limpeza periódica de tokens expirados da blacklist.
 */
@Component
public class TokenBlacklistCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistCleanupTask.class);

    private final TokenBlacklistService blacklistService;

    public TokenBlacklistCleanupTask(TokenBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    /** Remove tokens expirados diariamente às 03h. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        long removed = blacklistService.cleanupExpired();
        if (removed > 0) {
            log.info("Removidos {} tokens expirados da blacklist", removed);
        }
    }
}