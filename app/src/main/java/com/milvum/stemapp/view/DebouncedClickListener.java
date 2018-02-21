package com.milvum.stemapp.view;

import android.view.View;

/**
 * OnClickListener that requires a call to done before it
 */
public abstract class DebouncedClickListener implements View.OnClickListener {
    private boolean idle;

    public DebouncedClickListener() {
        this.idle = true;
    }

    @Override
    public final void onClick(View v) {
        if(!idle) {
            return; // bounce
        }

        idle = false;
        clickAction(v);
    }

    protected abstract void clickAction(View v);

    protected void done() {
        this.idle = true;
    }
}
