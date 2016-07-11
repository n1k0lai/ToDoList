package com.nplusnapps.todolist;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * The simple content provider for CRUD operations.
 * There's no auto notification of the data changes in this particular implementation.
 */
public class DataProvider extends ContentProvider {

    public static final Uri CONTENT_URI =
            Uri.parse("content://com.nplusnapps.todolist.dataprovider/tasks");

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PREVIOUS = "previous";
    public static final String COLUMN_NEXT = "next";
    public static final String COLUMN_TASK = "task";
    public static final String ORDER_DEFAULT = COLUMN_ID + " DESC";

    private static final int MATCH_TASKS = 1;
    private static final int MATCH_TASK = 2;

    private ContentResolver mResolver;
    private DatabaseHelper mHelper;
    private SQLiteDatabase mDatabase;

    private static final UriMatcher sMatcher;

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI("com.nplusnapps.todolist.dataprovider", "tasks", MATCH_TASKS);
        sMatcher.addURI("com.nplusnapps.todolist.dataprovider", "tasks/#", MATCH_TASK);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        mResolver = context.getContentResolver();
        mHelper = new DatabaseHelper(context,
                DatabaseHelper.DATABASE_NAME, null,
                DatabaseHelper.DATABASE_VERSION);

        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sMatcher.match(uri)) {
            case MATCH_TASKS:
                return "vnd.android.cursor.dir/vnd.com.nplusnapps.dataprovider.tasks";
            case MATCH_TASK:
                return "vnd.android.cursor.item/vnd.com.nplusnapps.dataprovider.tasks";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        mDatabase = mHelper.getWritableDatabase();

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DatabaseHelper.TABLE_TASKS);

        int match = sMatcher.match(uri);
        switch (match) {
            case MATCH_TASKS:
                break;
            case MATCH_TASK:
                builder.appendWhere(COLUMN_ID + " = " + uri.getPathSegments().get(1));
                break;
            default:
                throw new SQLException("Unable to query " + uri);
        }

        if (TextUtils.isEmpty(sortOrder)) {
            switch (match) {
                case MATCH_TASKS:
                    sortOrder = ORDER_DEFAULT;
                    break;
                default:
                    break;
            }
        }

        Cursor cursor = builder.query(
                mDatabase, projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null) {
            cursor.setNotificationUri(mResolver, uri);
        }

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mDatabase = mHelper.getWritableDatabase();

        switch (sMatcher.match(uri)) {
            case MATCH_TASKS:
                long id = mDatabase.insert(DatabaseHelper.TABLE_TASKS, "nullcolumn", values);
                if (id != -1) {
                    return ContentUris.withAppendedId(CONTENT_URI, id);
                }
            default:
                throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        mDatabase = mHelper.getWritableDatabase();

        switch (sMatcher.match(uri)) {
            case MATCH_TASKS:
                break;
            case MATCH_TASK:
                selection = COLUMN_ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        return mDatabase.delete(DatabaseHelper.TABLE_TASKS, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        mDatabase = mHelper.getWritableDatabase();

        switch (sMatcher.match(uri)) {
            case MATCH_TASKS:
                break;
            case MATCH_TASK:
                selection = COLUMN_ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        return mDatabase.update(DatabaseHelper.TABLE_TASKS, values, selection, selectionArgs);
    }

    /**
     * This helper manages the database create and upgrade operations.
     */
    public static class DatabaseHelper extends SQLiteOpenHelper {
        public static final String DATABASE_NAME = "ToDoList.db";
        public static final int DATABASE_VERSION = 1;

        public static final String TABLE_TASKS = "Tasks";

        private static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_TASKS + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_PREVIOUS + " INTEGER, " + COLUMN_NEXT + " INTEGER, "+ COLUMN_TASK + " TEXT);";
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS '" + TABLE_TASKS + "'";

        public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Warn before upgrading the database.
            Log.w(DataProvider.class.getSimpleName(), "Upgrading database from version " + oldVersion +
                    " to " + newVersion + ". All existing tables will be destroyed.");
            db.execSQL(DROP_TABLE);
            onCreate(db);
        }
    }
}
