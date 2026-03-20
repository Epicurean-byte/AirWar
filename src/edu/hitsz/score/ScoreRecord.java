package edu.hitsz.score;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ScoreRecord {
    private static final DateTimeFormatter PRIMARY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter LEGACY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String playerName;
    private final int score;
    private final LocalDateTime timestamp;

    public ScoreRecord(String playerName, int score, LocalDateTime timestamp) {
        this.playerName = playerName;
        this.score = score;
        this.timestamp = timestamp;
    }

    public static ScoreRecord fromLine(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        String name = parts[0];
        int value;
        try {
            value = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        LocalDateTime time = parseTimestamp(parts[2]);
        return new ScoreRecord(name, value, time);
    }

    public String toLine() {
        return playerName + "|" + score + "|" + PRIMARY_FORMAT.format(timestamp);
    }

    public static String format(LocalDateTime time) {
        return PRIMARY_FORMAT.format(time);
    }

    private static LocalDateTime parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value, PRIMARY_FORMAT);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value, LEGACY_FORMAT);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
