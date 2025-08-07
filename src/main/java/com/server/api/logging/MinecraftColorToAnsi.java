package com.server.api.logging;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftColorToAnsi {

    private static final Map<Character, String> ANSI_COLOR_MAP = new HashMap<>();
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_UNDERLINE = "\u001B[4m";
    private static final String ANSI_STRIKETHROUGH = "\u001B[9m";

    static {
        ANSI_COLOR_MAP.put('0', "\u001B[30m"); // black
        ANSI_COLOR_MAP.put('1', "\u001B[34m"); // dark blue
        ANSI_COLOR_MAP.put('2', "\u001B[32m"); // dark green
        ANSI_COLOR_MAP.put('3', "\u001B[36m"); // dark aqua
        ANSI_COLOR_MAP.put('4', "\u001B[31m"); // dark red
        ANSI_COLOR_MAP.put('5', "\u001B[35m"); // dark purple
        ANSI_COLOR_MAP.put('6', "\u001B[33m"); // gold
        ANSI_COLOR_MAP.put('7', "\u001B[37m"); // gray
        ANSI_COLOR_MAP.put('8', "\u001B[90m"); // dark gray
        ANSI_COLOR_MAP.put('9', "\u001B[94m"); // blue
        ANSI_COLOR_MAP.put('a', "\u001B[92m"); // green
        ANSI_COLOR_MAP.put('b', "\u001B[96m"); // aqua
        ANSI_COLOR_MAP.put('c', "\u001B[91m"); // red
        ANSI_COLOR_MAP.put('d', "\u001B[95m"); // light purple
        ANSI_COLOR_MAP.put('e', "\u001B[93m"); // yellow
        ANSI_COLOR_MAP.put('f', "\u001B[97m"); // white
        ANSI_COLOR_MAP.put('r', ANSI_RESET);   // reset
        ANSI_COLOR_MAP.put('l', ANSI_BOLD);    // bold
        ANSI_COLOR_MAP.put('n', ANSI_UNDERLINE); // underline
        ANSI_COLOR_MAP.put('m', ANSI_STRIKETHROUGH); // strikethrough
        // Formatting codes (bold, italic, etc. can be added if desired)
    }

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[§&]([0-9a-fl-nr])", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("[§&]#([A-Fa-f0-9]{6})");

    public static String translate(String input) {
        if (input == null) return null;

        // Convert hex codes (e.g., &#FF00FF)
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(input);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            String ansi = ansiFromHex(hex);
            hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement(ansi));
        }
        hexMatcher.appendTail(hexBuffer);

        // Convert legacy § codes
        Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(hexBuffer.toString());
        StringBuffer finalOutput = new StringBuffer();
        while (legacyMatcher.find()) {
            char code = Character.toLowerCase(legacyMatcher.group(1).charAt(0));
            String ansi = ANSI_COLOR_MAP.getOrDefault(code, "");
            legacyMatcher.appendReplacement(finalOutput, Matcher.quoteReplacement(ansi));
        }
        legacyMatcher.appendTail(finalOutput);

        // Always reset ANSI at end
        return finalOutput + ANSI_RESET;
    }

    private static String ansiFromHex(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b); // 24-bit ANSI foreground
    }
}