package com.odysee.app.adapter;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.model.Comment;

public class CommentItemDecoration extends RecyclerView.ItemDecoration {
    final private int amount;

    public CommentItemDecoration(int amount) {
        this.amount = amount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        CommentListAdapter adapter =  (CommentListAdapter) parent.getAdapter();

        if (adapter != null) {
            Comment comment = adapter.items.get(position);

            if (comment.getParentId()!= null) {
                outRect.left = amount;
            }
        }
    }
}
