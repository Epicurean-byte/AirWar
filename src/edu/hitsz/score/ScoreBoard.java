package edu.hitsz.score;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreBoard {
    private static final int DEFAULT_MAX_RECORDS = 10;
    private final ScoreDao scoreDao;
    private final int maxRecords;

    public ScoreBoard(ScoreDao scoreDao) {
        this(scoreDao, DEFAULT_MAX_RECORDS);
    }

    public ScoreBoard(ScoreDao scoreDao, int maxRecords) {
        this.scoreDao = scoreDao;
        this.maxRecords = maxRecords;
    }

    public synchronized void recordScore(String playerName, int score) {
        List<ScoreRecord> records = new ArrayList<>(scoreDao.loadRecords());
        records.add(new ScoreRecord(playerName, score, LocalDateTime.now()));
        records.sort(Comparator.comparingInt(ScoreRecord::getScore)
                .reversed()
                .thenComparing(ScoreRecord::getTimestamp));
        if (records.size() > maxRecords) {
            records = new ArrayList<>(records.subList(0, maxRecords));
        }
        scoreDao.saveRecords(records);
    }

    public synchronized void printLeaderboard() {
        List<ScoreRecord> records = scoreDao.loadRecords();
        if (records.isEmpty()) {
            System.out.println("Scoreboard is empty.");
            return;
        }
        System.out.println("===== Scoreboard =====");
        System.out.printf("%-4s %-10s %-10s %-20s%n", "Rank", "Player", "Score", "Time");
        int rank = 1;
        for (ScoreRecord record : records) {
            System.out.printf("%-4d %-10s %-10d %-20s%n",
                    rank++, record.getPlayerName(), record.getScore(),
                    ScoreRecord.format(record.getTimestamp()));
        }
        System.out.println("======================");
    }
}
