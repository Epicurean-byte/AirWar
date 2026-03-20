package edu.hitsz.score;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于文件系统的得分 DAO，实现读写本地文本文件。
 */
public class FileScoreDao implements ScoreDao {

    private final Path filePath;

    public FileScoreDao(String filePath) {
        this.filePath = Paths.get(filePath);
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize score file: " + e.getMessage());
        }
    }

    @Override
    public List<ScoreRecord> loadRecords() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(filePath).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(ScoreRecord::fromLine)
                    .filter(record -> record != null)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            System.err.println("Failed to read scores: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void saveRecords(List<ScoreRecord> records) {
        List<String> lines = records.stream()
                .map(ScoreRecord::toLine)
                .collect(Collectors.toList());
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            System.err.println("Failed to save scores: " + e.getMessage());
        }
    }
}

