package com.nplusnapps.todolist;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main activity holding the list of tasks.
 */
public class MainActivity extends AppCompatActivity {

    public static final String ACTION_ADD_TASK = "task_added";
    public static final String ACTION_EDIT_TASK = "task_edited";
    public static final String ACTION_MOVE_TASK = "task_moved";
    public static final String ACTION_DELETE_TASK = "task_deleted";

    public static final String EXTRA_TASK = "task";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_PREVIOUS = "task_previous";
    public static final String EXTRA_TASK_NEXT = "task_next";
    public static final String EXTRA_TASK_TARGET = "task_target";
    public static final String EXTRA_TASK_DIRECTION = "task_direction";

    private static final String EXTRA_VIEW_STATE = "view_state";

    private RecyclerListAdapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private ContentResolver mResolver;
    private ContentObserver mObserver;
    private SparseArray<Parcelable> mSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mResolver = getContentResolver();
        mObserver = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return false;
            }

            @Override
            public void onChange(boolean selfChange) {
                new QuerySortDataTask().execute(DataProvider.CONTENT_URI);
            }
        };

        final View stubView = findViewById(android.R.id.empty);

        mRecyclerAdapter = new RecyclerListAdapter(this, new ArrayList<Map<String, String>>());
        mRecyclerAdapter.setOnTaskChangedListener(new OnTaskChangedListener() {
            @Override
            public void onTaskMoved(int selectedId, int targetId, int moveDirection) {
                Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                intent.setAction(ACTION_MOVE_TASK).
                        putExtra(EXTRA_TASK_ID, selectedId).
                        putExtra(EXTRA_TASK_TARGET, targetId).
                        putExtra(EXTRA_TASK_DIRECTION, moveDirection);

                startService(intent);
            }

            @Override
            public void onTaskEdited(int taskId, String taskText) {
                Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                intent.setAction(ACTION_EDIT_TASK).putExtra(EXTRA_TASK_ID, taskId).putExtra(EXTRA_TASK, taskText);

                startService(intent);
            }

            @Override
            public void onTaskDeleted(final int taskId) {
                Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                intent.setAction(ACTION_DELETE_TASK).putExtra(EXTRA_TASK_ID, taskId);

                startService(intent);

                showSnackbar();
            }

            @Override
            public void onTaskCountChanged(int taskCount) {
                stubView.setVisibility(taskCount == 0 ? View.VISIBLE : View.INVISIBLE);

                setTitleTasks(taskCount);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(layoutManager);

        ItemTouchHelper touchHelper =
                new ItemTouchHelper(new ItemTouchHelperCallback(mRecyclerAdapter));
        touchHelper.attachToRecyclerView(mRecyclerView);

        // Restores the recycler view state if there's any.
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_VIEW_STATE)) {
            mSavedState = savedInstanceState.getSparseParcelableArray(EXTRA_VIEW_STATE);
        }

        new QuerySortDataTask().execute(DataProvider.CONTENT_URI);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mResolver.registerContentObserver(DataProvider.CONTENT_URI, true, mObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mResolver.unregisterContentObserver(mObserver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        SparseArray<Parcelable> recyclerState = new SparseArray<>();

        if (mRecyclerView != null) {
            mRecyclerView.saveHierarchyState(recyclerState);
        }

        outState.putSparseParcelableArray(EXTRA_VIEW_STATE, recyclerState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * The method is invoked when the floating button is clicked.
     *
     * @param view The view clicked
     */
    public void addNewTask(View view) {
        if (mRecyclerView.getChildCount() > 0) {
            mRecyclerView.scrollToPosition(0);
        }

        startService(new Intent(this, BackgroundService.class).setAction(ACTION_ADD_TASK));
    }

    /**
     * Shows the snackbar message when a task is deleted.
     */
    private void showSnackbar() {
        final Snackbar snackbar = Snackbar.make(mRecyclerView, getString(R.string.snackbar_task_deleted), Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.action_dismiss), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    /**
     * Sets the activity's title according to the task count.
     *
     * @param taskCount The task count
     */
    private void setTitleTasks(int taskCount) {
        setTitle(taskCount == 0 ? getString(R.string.app_name) :
                getString(R.string.activity_title_tasks, taskCount));
    }

    /**
     * This task handles the expensive data query and sort operations on a background thread.
     */
    public class QuerySortDataTask extends AsyncTask<Uri, Void, List<Map<String, String>>> {

        @Override
        protected List<Map<String, String>> doInBackground(Uri... params) {
            List<Map<String, String>> tempList = new ArrayList<>();

            // Yes, this is not the most elegant way of sorting the doubly linked cursor data, but it works.
            Cursor cursor = MainActivity.this.getContentResolver().query(params[0], null, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    Map<String, String> task = new HashMap<>();
                    task.put(EXTRA_TASK_ID, cursor.getString(cursor.getColumnIndex(DataProvider.COLUMN_ID)));
                    task.put(EXTRA_TASK, cursor.getString(cursor.getColumnIndex(DataProvider.COLUMN_TASK)));
                    task.put(EXTRA_TASK_PREVIOUS, cursor.getString(cursor.getColumnIndex(DataProvider.COLUMN_PREVIOUS)));
                    task.put(EXTRA_TASK_NEXT, cursor.getString(cursor.getColumnIndex(DataProvider.COLUMN_NEXT)));

                    tempList.add(task);
                }
            } catch (Exception e) {
                Log.e(MainActivity.class.getSimpleName(), e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            List<Map<String, String>> sortedList = new ArrayList<>();

            int previousId;
            for (Map<String, String> task : tempList) {
                if (Integer.valueOf(task.get(EXTRA_TASK_NEXT)) == 0) {
                    tempList.remove(task);
                    sortedList.add(task);

                    previousId = Integer.valueOf(task.get(EXTRA_TASK_PREVIOUS));

                    int currentPos;
                    while (!tempList.isEmpty()) {
                        for (Map<String, String> nextTask : tempList) {
                            currentPos = tempList.indexOf(nextTask);

                            if (Integer.valueOf(nextTask.get(EXTRA_TASK_ID)) == previousId) {
                                tempList.remove(nextTask);
                                sortedList.add(nextTask);

                                previousId = Integer.valueOf(nextTask.get(EXTRA_TASK_PREVIOUS));

                                break;
                            }

                            if (currentPos == tempList.size() - 1) {
                                // Quits the loop if there's no valid previous ID
                                break;
                            }
                        }
                    }

                    break;
                }
            }

            return sortedList;
        }

        @Override
        protected void onPostExecute(List<Map<String, String>> result) {
            if (mRecyclerAdapter != null) {
                mRecyclerAdapter.addItems(result, true);
            }

            if (mRecyclerView != null && mSavedState != null) {
                mRecyclerView.restoreHierarchyState(mSavedState);
                mSavedState = null;
            }
        }
    }
}
