package com.kamismp.kamicore.util;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Helpers for tab-completion.
 */
public final class Suggest {

    private Suggest() {
    }

    /** Which argument the player is typing right now (0 = first). */
    public static int index(String[] args) {
        return Math.max(0, args.length - 1);
    }

    /** The text of the argument the player is typing right now. */
    public static String current(String[] args) {
        return args.length == 0 ? "" : args[args.length - 1];
    }

    /** Keeps only the options that start with what the player already typed. */
    public static List<String> filter(Collection<String> options, String typed) {
        String lower = typed.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }
}
