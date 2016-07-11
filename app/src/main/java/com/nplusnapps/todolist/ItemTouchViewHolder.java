package com.nplusnapps.todolist;

/**
 * This interface provides the callbacks to notify an item has been selected or cleared.
 */
public interface ItemTouchViewHolder {

    /**
     * Called when the item has been selected.
     */
    void onItemSelected();

    /**
     * Called when the item has been cleared.
     */
    void onItemClear();

    /**
     * Sets the provided tag to the stored view.
     *
     * @param tag The tag
     */
    void setTag(int tag);

    /**
     * Gets the tag from the stored view.
     *
     * @return The tag
     */
    int getTag();
}
