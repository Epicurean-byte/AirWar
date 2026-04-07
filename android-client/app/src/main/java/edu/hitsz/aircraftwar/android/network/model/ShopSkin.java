package edu.hitsz.aircraftwar.android.network.model;

public final class ShopSkin {
    private final int skinId;
    private final String name;
    private final String description;
    private final long price;
    private final String category;
    private final boolean equippable;
    private final String assetName;
    private final boolean owned;

    public ShopSkin(int skinId, String name, String description, long price, String category,
                    boolean equippable, String assetName, boolean owned) {
        this.skinId = skinId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.equippable = equippable;
        this.assetName = assetName;
        this.owned = owned;
    }

    public int getSkinId() {
        return skinId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEquippable() {
        return equippable;
    }

    public String getAssetName() {
        return assetName;
    }

    public boolean isOwned() {
        return owned;
    }

    public boolean isPlaneSkin() {
        return "plane_skin".equals(category) || equippable;
    }

    public boolean isMemorabilia() {
        return "memorabilia".equals(category) && !equippable;
    }
}
