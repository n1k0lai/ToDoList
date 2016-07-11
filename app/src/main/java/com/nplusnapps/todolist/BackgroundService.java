package com.nplusnapps.todolist;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

/**
 * The service manages all CRUD operations on the database.
 */
public class BackgroundService extends IntentService {

    private ContentResolver mResolver;

    public BackgroundService() {
        super(BackgroundService.class.getSimpleName());

        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mResolver = getContentResolver();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case MainActivity.ACTION_ADD_TASK:
                    addTask();

                    break;
                case MainActivity.ACTION_EDIT_TASK:
                    editTask(intent.getStringExtra(MainActivity.EXTRA_TASK),
                            intent.getIntExtra(MainActivity.EXTRA_TASK_ID, 0));

                    break;
                case MainActivity.ACTION_DELETE_TASK:
                    deleteTask(intent.getIntExtra(MainActivity.EXTRA_TASK_ID, 0));

                    break;
                case MainActivity.ACTION_MOVE_TASK:
                    moveTask(intent.getIntExtra(MainActivity.EXTRA_TASK_ID, 0),
                            intent.getIntExtra(MainActivity.EXTRA_TASK_TARGET, 0),
                            intent.getIntExtra(MainActivity.EXTRA_TASK_DIRECTION, 0));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Adds a new task.
     */
    private void addTask() {
        int previousId = 0, addedId = 0;

        Cursor cursor = mResolver.query(
                DataProvider.CONTENT_URI, null, DataProvider.COLUMN_NEXT + " = " + 0, null, null);
        try {
            if (cursor.moveToLast()) {
                previousId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_ID));
            }
        } catch (Exception e) {
            logException(e);
        } finally {
            closeCursor(cursor);
        }

        ContentValues values = new ContentValues();
        values.put(DataProvider.COLUMN_PREVIOUS, previousId);
        values.put(DataProvider.COLUMN_NEXT, 0);

        String lastSegment = mResolver.insert(
                DataProvider.CONTENT_URI, values).getLastPathSegment();
        if (lastSegment != null) {
            addedId = Integer.parseInt(lastSegment);
        }

        if (addedId != 0) {
            updateTaskNextId(addedId, previousId);
        }

        notifyChange();
    }

    /**
     * Edits the existing task with the provided ID.
     *
     * @param task The edited task
     * @param id The task ID
     */
    private void editTask(String task, int id) {
        if (id != 0) {
            ContentValues values = new ContentValues();

            if (task != null) {
                values.put(DataProvider.COLUMN_TASK, task);
            } else {
                values.putNull(DataProvider.COLUMN_TASK);
            }

            mResolver.update(
                    DataProvider.CONTENT_URI, values, DataProvider.COLUMN_ID + " = " + id, null);
        }
    }

    /**
     * Deletes the task with the provided ID.
     *
     * @param id The task ID
     */
    private void deleteTask(int id) {
        if (id != 0) {
            int previousId = 0, nextId = 0;

            Cursor cursor = mResolver.query(
                    DataProvider.CONTENT_URI, null, DataProvider.COLUMN_ID + " = " + id, null, null);
            try {
                if (cursor.moveToFirst()) {
                    previousId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_PREVIOUS));
                    nextId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_NEXT));
                }
            } catch (Exception e) {
                logException(e);
            } finally {
                closeCursor(cursor);
            }

            int rowsDeleted = mResolver.delete(
                    DataProvider.CONTENT_URI, DataProvider.COLUMN_ID + " = " + id, null);

            if (rowsDeleted > 0) {
                updateTaskNextId(nextId, previousId);
                updateTaskPreviousId(previousId, nextId);
            }

            notifyChange();
        }
    }

    /**
     * Moves the task to the new logical position in the linked data set.
     *
     * @param selectedId The selected task ID
     * @param targetId The target task ID
     * @param moveDirection The direction in which to move the task
     *                      <code>ItemTouchHelper.UP</code> or <code>ItemTouchHelper.DOWN</code>
     */
    private void moveTask(int selectedId, int targetId, int moveDirection) {
        if (selectedId != 0) {
            int previousId = 0, nextId = 0, targetPreviousId = 0, targetNextId = 0;

            Cursor cursor = mResolver.query(
                    DataProvider.CONTENT_URI, null, DataProvider.COLUMN_ID +
                            " IN (" + selectedId + ", " + targetId + ")", null, null);
            try {
                while (cursor.moveToNext()) {
                    int currentId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_ID));

                    if (currentId == selectedId) {
                        previousId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_PREVIOUS));
                        nextId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_NEXT));
                    } else if (currentId == targetId) {
                        targetPreviousId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_PREVIOUS));
                        targetNextId = cursor.getInt(cursor.getColumnIndex(DataProvider.COLUMN_NEXT));
                    }
                }
            } catch (Exception e) {
                logException(e);
            } finally {
                closeCursor(cursor);
            }

            ContentValues values = new ContentValues();

            switch (moveDirection) {
                case ItemTouchHelper.UP:
                    values.put(DataProvider.COLUMN_PREVIOUS, targetId);
                    values.put(DataProvider.COLUMN_NEXT, targetNextId);
                    break;
                case ItemTouchHelper.DOWN:
                    values.put(DataProvider.COLUMN_NEXT, targetId);
                    values.put(DataProvider.COLUMN_PREVIOUS, targetPreviousId);
                    break;
                default:
                    break;
            }

            int rowsUpdated = mResolver.update(
                    DataProvider.CONTENT_URI, values, DataProvider.COLUMN_ID + " = " + selectedId, null);

            if (rowsUpdated > 0) {
                values.clear();

                switch (moveDirection) {
                    case ItemTouchHelper.UP:
                        if (targetPreviousId == selectedId) {
                            values.put(DataProvider.COLUMN_PREVIOUS, previousId);
                        }
                        values.put(DataProvider.COLUMN_NEXT, selectedId);

                        mResolver.update(DataProvider.CONTENT_URI,
                                values, DataProvider.COLUMN_ID + " = " + targetId, null);

                        updateTaskPreviousId(selectedId, targetNextId);

                        break;
                    case ItemTouchHelper.DOWN:
                        if (targetNextId == selectedId) {
                            values.put(DataProvider.COLUMN_NEXT, nextId);
                        }
                        values.put(DataProvider.COLUMN_PREVIOUS, selectedId);

                        mResolver.update(DataProvider.CONTENT_URI,
                                values, DataProvider.COLUMN_ID + " = " + targetId, null);

                        updateTaskNextId(selectedId, targetPreviousId);

                        break;
                    default:
                        break;
                }

                updateTaskNextId(nextId, previousId);
                updateTaskPreviousId(previousId, nextId);
            }

            notifyChange();
        }
    }

    /**
     * Updates the previous ID column value of the task with the provided ID.
     *
     * @param previousId The previous ID value
     * @param taskId The task ID
     */
    private void updateTaskPreviousId(int previousId, int taskId) {
        if (taskId != 0) {
            ContentValues values = new ContentValues();
            values.put(DataProvider.COLUMN_PREVIOUS, previousId);

            mResolver.update(DataProvider.CONTENT_URI,
                    values, DataProvider.COLUMN_ID + " = " + taskId, null);
        }
    }

    /**
     * Updates the next ID column value of the task with the provided ID.
     *
     * @param nextId The next ID value
     * @param taskId The task ID
     */
    private void updateTaskNextId(int nextId, int taskId) {
        if (taskId != 0) {
            ContentValues values = new ContentValues();
            values.put(DataProvider.COLUMN_NEXT, nextId);

            mResolver.update(DataProvider.CONTENT_URI,
                    values, DataProvider.COLUMN_ID + " = " + taskId, null);
        }
    }

    /**
     * Notifies the observer the data has been changed.
     */
    private void notifyChange() {
        mResolver.notifyChange(DataProvider.CONTENT_URI, null);
    }

    /**
     * Safely closes the provided cursor.
     *
     * @param cursor The cursor to close
     */
    private void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Logs the provided exception.
     *
     * @param e The exception
     */
    private void logException(Exception e) {
        Log.e(BackgroundService.class.getSimpleName(), e.getMessage(), e);
    }
}
