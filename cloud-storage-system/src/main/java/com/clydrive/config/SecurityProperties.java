package com.clydrive.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SecurityProperties {

    @Value("${security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.lock-time-minutes}")
    private int lockTimeMinutes;

    @Value("${security.password-history-limit}")
    private int passwordHistoryLimit;
}
