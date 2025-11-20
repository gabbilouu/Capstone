package com.example.elevate;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple spacing decoration for the shop grid.
 * The constructor takes spacing in *pixels*.
 * If you want 8dp, pass a converted value:
 *   int space = (int) (8 * view.getResources().getDisplayMetrics().density);
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacePx;

    public SpacesItemDecoration(int spacePx) {
        this.spacePx = spacePx;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {

        int position = parent.getChildAdapterPosition(view);
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        int spanCount = 1;
        if (lm instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) lm).getSpanCount();
        }

        outRect.left = spacePx / 2;
        outRect.right = spacePx / 2;
        outRect.bottom = spacePx;

        // Add top margin only for first row
        if (position < spanCount) {
            outRect.top = spacePx;
        } else {
            outRect.top = spacePx / 2;
        }
    }
}
