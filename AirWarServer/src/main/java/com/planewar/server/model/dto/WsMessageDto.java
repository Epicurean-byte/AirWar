package com.planewar.server.model.dto;

import lombok.Data;

/**
 * WebSocket 通讯消息统一封装体。
 * type  : 消息类型标识，如 "MOVE", "FIRE", "ENEMY_SPAWN", "SCORE_UPDATE", "GAME_OVER" 等
 * roomId: 所属房间 ID
 * userId: 发送方用户 ID（服务端下发时为 0）
 * payload: JSON 字符串，存放具体数据
 */
@Data
public class WsMessageDto {
    private String type;
    private long roomId;
    private long userId;
    private String payload;
}
