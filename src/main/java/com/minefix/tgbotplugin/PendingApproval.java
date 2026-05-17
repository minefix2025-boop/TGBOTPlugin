package minefix.tgbotplugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PendingApproval {
    private static final Set<UUID> awaiting2FA = new HashSet<>();

    public static void add(UUID uuid) {
        awaiting2FA.add(uuid);
    }

    public static void remove(UUID uuid) {
        awaiting2FA.remove(uuid);
    }

    public static boolean isAwaiting(UUID uuid) {
        return awaiting2FA.contains(uuid);
    }
}
