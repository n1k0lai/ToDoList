package com.nplusnapps.todolist;

/**
 * This interface provides the callbacks to notify a task has been changed.
 */
public interface OnTaskChangedListener {

    /**
     * Called when the selected task is moved to another position.
     *
     * @param selectedId The selected task ID
     * @param targetId The target task ID
     * @param moveDirection The direction in which to move the task
     *                      <code>ItemTouchHelper.UP</code> or <code>ItemTouchHelper.DOWN</code>
     */
    void onTaskMoved(int selectedId, int targetId, int moveDirection);

    /**
     * Called when the task has been edited.
     *
     * @param editedId The edited task ID
     * @param taskText The new task
     */
    void onTaskEdited(int editedId, String taskText);

    /**
     * Called when the task has been deleted.
     *
     * @param deletedId The deleted task ID
     */
    void onTaskDeleted(int deletedId);

    /**
     * Called when the task count has been changed.
     *
     * @param taskCount The new task count
     */
    void onTaskCountChanged(int taskCount);
}
