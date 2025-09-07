package nl.serverpapi.standupCommentator.utils;

import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private long lastResponseTime = 0;
    private final long cooldownMillis;

    public CooldownManager(long cooldownSeconds) {
        this.cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSeconds);
    }

    public boolean isOnCooldown() {
        return System.currentTimeMillis() - lastResponseTime < cooldownMillis;
    }

    public void resetCooldown() {
        lastResponseTime = System.currentTimeMillis();
    }
}