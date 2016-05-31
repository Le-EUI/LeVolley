package provider;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by dingding on 22/05/16.
 */
public class DownloadProvider extends ContentProvider{
    private static final String TAG = "DownloadProvider";

    /*Default sort order*/
    public static final String DEFAULT_SORT_ORDER = "_id asc";

    /*Call Method*/
    public static final String METHOD_GET_ITEM_COUNT = "METHOD_GET_ITEM_COUNT";
    public static final String KEY_ITEM_COUNT = "KEY_ITEM_COUNT";

    /*Authority*/
    public static final String AUTHORITY = "le.volley.providers.download";

    /*Match Code*/
    public static final int ITEM = 1;
    public static final int ITEM_ID = 2;
    public static final int ITEM_POS = 3;

    /*MIME*/
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.le.volley.download";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.le.volley.download";

    /*Content URI*/
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/item");
    public static final Uri CONTENT_POS_URI = Uri.parse("content://" + AUTHORITY + "/pos");


    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "item", ITEM);
        uriMatcher.addURI(AUTHORITY, "item/#", ITEM_ID);
        uriMatcher.addURI(AUTHORITY, "pos/#", ITEM_POS);
    }

    private static final HashMap<String, String> mDownloadProjectionMap;
    static {
        mDownloadProjectionMap = new HashMap<String, String>();
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo._ID, DownloadTable.DownloadInfo._ID);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.URL, DownloadTable.DownloadInfo.URL);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.FILE_PATH, DownloadTable.DownloadInfo.FILE_PATH);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.FILE_SIZE, DownloadTable.DownloadInfo.FILE_SIZE);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.START_POS, DownloadTable.DownloadInfo.START_POS);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.END_POS, DownloadTable.DownloadInfo.END_POS);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.COMPLETE_SIZE, DownloadTable.DownloadInfo.COMPLETE_SIZE);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.BLOCK_ID, DownloadTable.DownloadInfo.BLOCK_ID);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.BLOCK_COUNT, DownloadTable.DownloadInfo.BLOCK_COUNT);
        mDownloadProjectionMap.put(DownloadTable.DownloadInfo.STATE, DownloadTable.DownloadInfo.STATE);
    }

    private DownloadDatabaseHelper dbHelper = null;
    private ContentResolver resolver = null;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        resolver = context.getContentResolver();
        dbHelper = DownloadDatabaseHelper.getInstance(context);
        Log.i(TAG, "Download Provider Create");

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.i(TAG, "DownloadProvider.query: " + uri);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        String limit = null;

        switch (uriMatcher.match(uri)) {
            case ITEM: {
                sqlBuilder.setTables(DownloadTable.DownloadInfo.TABLE);
                sqlBuilder.setProjectionMap(mDownloadProjectionMap);
                break;
            }
            case ITEM_ID: {
                String id = uri.getPathSegments().get(1);
                sqlBuilder.setTables(DownloadTable.DownloadInfo.TABLE);
                sqlBuilder.setProjectionMap(mDownloadProjectionMap);
                sqlBuilder.appendWhere(DownloadTable.DownloadInfo._ID + "=" + id);
                break;
            }
            case ITEM_POS: {
                String pos = uri.getPathSegments().get(1);
                sqlBuilder.setTables(DownloadTable.DownloadInfo.TABLE);
                sqlBuilder.setProjectionMap(mDownloadProjectionMap);
                limit = pos + ", 1";
                break;
            }
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }

        Cursor cursor = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, TextUtils.isEmpty(sortOrder) ? DEFAULT_SORT_ORDER : sortOrder, limit);
        cursor.setNotificationUri(resolver, uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ITEM:
                return CONTENT_TYPE;
            case ITEM_ID:
            case ITEM_POS:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if(uriMatcher.match(uri) != ITEM) {
            throw new IllegalArgumentException("Error Uri: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long id = db.insert(DownloadTable.DownloadInfo.TABLE, DownloadTable.DownloadInfo._ID, values);
        if(id < 0) {
            throw new SQLiteException("Unable to insert " + values + " for " + uri);
        }

        Uri newUri = ContentUris.withAppendedId(uri, id);
        resolver.notifyChange(newUri, null);

        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        switch(uriMatcher.match(uri)) {
            case ITEM: {
                count = db.delete(DownloadTable.DownloadInfo.TABLE, selection, selectionArgs);
                break;
            }
            case ITEM_ID: {
                String id = uri.getPathSegments().get(1);
                count = db.delete(DownloadTable.DownloadInfo.TABLE, DownloadTable.DownloadInfo._ID + "=" + id
                        + (!TextUtils.isEmpty(selection) ? " and (" + selection + ')' : ""), selectionArgs);
                break;
            }
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }

        resolver.notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        switch(uriMatcher.match(uri)) {
            case ITEM: {
                count = db.update(DownloadTable.DownloadInfo.TABLE, values, selection, selectionArgs);
                break;
            }
            case ITEM_ID: {
                String id = uri.getPathSegments().get(1);
                count = db.update(DownloadTable.DownloadInfo.TABLE, values, DownloadTable.DownloadInfo._ID + "=" + id
                        + (!TextUtils.isEmpty(selection) ? " and (" + selection + ')' : ""), selectionArgs);
                break;
            }
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }

        resolver.notifyChange(uri, null);

        return count;
    }

    @Override
    public Bundle call(String method, String request, Bundle args) {
        Log.i(TAG, "DownloadProvider.call: " + method);

        if(method.equals(METHOD_GET_ITEM_COUNT)) {
            return getItemCount();
        }

        throw new IllegalArgumentException("Error method call: " + method);
    }

    private Bundle getItemCount() {
        Log.i(TAG, "DownloadProvider.getItemCount");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from " + DownloadTable.DownloadInfo.TABLE, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ITEM_COUNT, count);

        cursor.close();
        db.close();

        return bundle;
    }
}
