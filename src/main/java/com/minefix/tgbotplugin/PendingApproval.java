package com.minefix.tgbotplugin;

import java.util.HashSet;
import java.util.UUID;

public class PendingApproval {
    private static final HashSet<UUID> frozenPlayers = new HashSet<>();

    public static void add(UUID uuid) { frozenPlayers.add(uuid); }
    public static void remove(UUID uuid) { frozenPlayers.remove(uuid); }
    public static boolean contains(UUID uuid) { return frozenPlayers.contains(uuid); }
    public static void clear() { frozenPlayers.clear(); }
}
