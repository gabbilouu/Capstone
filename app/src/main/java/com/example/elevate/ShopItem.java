package com.example.elevate;

public class ShopItem {
    public final String id;
    public final String title;
    public final int iconRes;
    public final int price;
    public final boolean owned;
    public final boolean locked;
    public final boolean equipped; // NEW
    public final ShopFragment.Category category;

    public ShopItem(String id, String title, int iconRes, int price,
                    boolean owned, boolean locked, boolean equipped,
                    ShopFragment.Category category) {
        this.id = id; this.title = title; this.iconRes = iconRes; this.price = price;
        this.owned = owned; this.locked = locked; this.equipped = equipped; this.category = category;
    }

    public ShopItem copy(boolean owned, boolean locked, boolean equipped) {
        return new ShopItem(id, title, iconRes, price, owned, locked, equipped, category);
    }
}
