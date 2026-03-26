package com.planewar.server.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkinConfig {
    private int skinId;
    private String name;
    private String description;
    private long price;
    /** 客户端贴图资源名 */
    private String assetName;
}
