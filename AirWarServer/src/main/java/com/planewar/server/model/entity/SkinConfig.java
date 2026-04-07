package com.planewar.server.model.entity;
public class SkinConfig {
    private int skinId;
    private String name;
    private String description;
    private long price;
    /** 商品分类：plane_skin / memorabilia */
    private String category;
    /** 是否可装备 */
    private boolean equippable;
    /** 客户端贴图资源名 */
    private String assetName;

    public SkinConfig() {
    }

    public SkinConfig(int skinId, String name, String description, long price, String category,
                      boolean equippable, String assetName) {
        this.skinId = skinId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.equippable = equippable;
        this.assetName = assetName;
    }

    public int getSkinId() {
        return skinId;
    }

    public void setSkinId(int skinId) {
        this.skinId = skinId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEquippable() {
        return equippable;
    }

    public void setEquippable(boolean equippable) {
        this.equippable = equippable;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}
