package edu.hitsz.score;

import java.util.List;

/**
 * 数据访问对象接口，用于持久化得分记录。
 */
public interface ScoreDao {

    /** 读取当前的所有得分记录 */
    List<ScoreRecord> loadRecords();

    /** 覆盖保存得分记录 */
    void saveRecords(List<ScoreRecord> records);
}

