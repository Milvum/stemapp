package com.milvum.stemapp.adapters;

import android.view.View;

/**
 * .
 */

public interface ListItemClickedListener<T> {
    void onListItemClicked(View view, T item, int position);
}
