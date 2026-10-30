package com.kamismp.kamicore.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Small helper for sending colored messages.
 * Messages use MiniMessage tags like <red>, <gold>, <b> etc.
 */
public final class Msg {

    private static String prefix = "";

    private Msg() {
    }

    public static void setPrefix(String newPrefix) {
        prefix = newPrefix == null ? "" : newPrefix;
    }

    /** Chat message with the KamiSMP prefix in front. */
    public static void send(CommandSender to, String message) {
        to.sendRichMessage(prefix + message);
    }

    /** Chat message without the prefix. */
    public static void raw(CommandSender to, String message) {
        to.sendRichMessage(message);
    }

    /** Text shown just above the hotbar. */
    public static void actionBar(Player to, String message) {
        to.sendActionBar(MiniMessage.miniMessage().deserialize(message));
    }

    /** Shared "only players can use this" message. */
    public static void playersOnly(CommandSender to) {
        send(to, "<red>Only players can use this command.");
    }
}
