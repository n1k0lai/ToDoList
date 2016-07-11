package com.nplusnapps.todolist;

import android.support.v7.widget.RecyclerView;

/**
 * This interface provides the callbacks to notify an item has been moved, dropped or dismissed.
 */
public interface ItemTouchAdapter {

    /**
     * Called when the source item has been moved to the target item position.
     *
     * @param sourceHolder The source item view holder
     * @param targetHolder The target item view holder
     */
    void onItemMove(RecyclerView.ViewHolder sourceHolder, RecyclerView.ViewHolder targetHolder);

    /**
     * Called when the item has been dropped.
     *
     * @param viewHolder The item view holder
     */
    void onItemDrop(RecyclerView.ViewHolder viewHolder);

    /**
     * Called when the item has been dismissed.
     *
     * @param viewHolder The item view holder
     */
    void onItemDismiss(RecyclerView.ViewHolder viewHolder);
}
