package com.example.elevate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.Holder> {

    public interface OnShopClick { void onClick(ShopItem item); }
    private final OnShopClick listener;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopItemAdapter(OnShopClick l) { this.listener = l; }

    public void submit(List<ShopItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_card, parent, false);
        return new Holder(v);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
        ShopItem it = items.get(pos);
        h.title.setText(it.title);

        if (it.iconRes != 0) { h.img.setImageResource(it.iconRes); h.img.setVisibility(View.VISIBLE); }
        else { h.img.setVisibility(View.INVISIBLE); }

        h.lockedOverlay.setVisibility(it.locked ? View.VISIBLE : View.GONE);
        h.equippedBadge.setVisibility(it.equipped && !it.locked ? View.VISIBLE : View.GONE);

        if (!it.locked && !it.owned) {
            h.priceRow.setVisibility(View.VISIBLE);
            h.price.setText(String.valueOf(it.price));
        } else {
            h.priceRow.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(it));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView img, equippedBadge;
        TextView title, price;
        View priceRow, lockedOverlay;
        Holder(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            title = v.findViewById(R.id.title);
            price = v.findViewById(R.id.price);
            priceRow = v.findViewById(R.id.priceRow);
            lockedOverlay = v.findViewById(R.id.lockedOverlay);
            equippedBadge = v.findViewById(R.id.equippedBadge);
        }
    }
}
