package com.example.photomaster.util;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by ravi on 23/10/17.
 */

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == state.getItemCount() - 1) {
            outRect.left = space;
            outRect.right = 0;
        }else{
            outRect.right = space;
            outRect.left = 0;
        }
    }
}