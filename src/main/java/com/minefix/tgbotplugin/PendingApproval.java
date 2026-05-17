package com.example.telegramconsole;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PendingApproval {
    private static final Set<UUID> awaiting2FA = new HashSet<>();

    public static synchronized void add(UUID uuid) {
        awaiting2FA.add(uuid);
    }

    public static synchronized void remove(UUID uuid) {
        awaiting2FA.remove(uuid);
    }

    public static synchronized boolean isAwaiting(UUID uuid) {
        return awaiting2FA.contains(uuid);
    }
}
