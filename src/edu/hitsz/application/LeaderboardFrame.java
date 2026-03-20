package edu.hitsz.application;

import edu.hitsz.score.FileScoreDao;
import edu.hitsz.score.ScoreDao;
import edu.hitsz.score.ScoreRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 简易排行榜窗口：展示、删除历史得分。
 */
public class LeaderboardFrame extends JFrame {
    private final ScoreDao scoreDao;
    private final Difficulty difficulty;
    private final JTable table;
    private final DefaultTableModel model;

    public LeaderboardFrame(Difficulty difficulty) {
        super("Aircraft War");
        this.difficulty = difficulty;
        String file = "data/scores_" + difficulty.name().toLowerCase() + ".txt";
        this.scoreDao = new FileScoreDao(file);

        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("难度：" + difficulty + "    排行榜", SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"名次", "玩家名", "得分", "记录时间"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton deleteBtn = new JButton("删除选中的记录");
        deleteBtn.addActionListener(e -> deleteSelected());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(deleteBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        List<ScoreRecord> records = new ArrayList<>(scoreDao.loadRecords());
        records.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed()
                .thenComparing(ScoreRecord::getTimestamp));
        model.setRowCount(0);
        int rank = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        for (ScoreRecord r : records) {
            model.addRow(new Object[]{rank++, r.getPlayerName(), r.getScore(), fmt.format(r.getTimestamp())});
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int ok = JOptionPane.showConfirmDialog(this, "是否删除选中的记录?", "选择一个选项", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        List<ScoreRecord> records = new ArrayList<>(scoreDao.loadRecords());
        records.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed()
                .thenComparing(ScoreRecord::getTimestamp));
        if (row >= 0 && row < records.size()) {
            records.remove(row);
            scoreDao.saveRecords(records);
            refresh();
        }
    }
}

