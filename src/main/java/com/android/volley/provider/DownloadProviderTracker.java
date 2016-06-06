package provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.android.volley.VolleyLog;

/**
 * Created by dingding on 22/05/16.
 */
public class DownloadProviderTracker {
    private static final String TAG = "DownloadProviderTracker";

    /** download uri */
    private static final Uri DOWNLOAD_CONTENT_URI = DownloadProvider.CONTENT_URI;

    private static final String DEFAULT_SORT_ORDER = DownloadProvider.DEFAULT_SORT_ORDER;

    /** download where */
    private static final String WHERE = DownloadTable.DownloadInfo.URL + " = ? AND "
                                + DownloadTable.DownloadInfo.FILE_PATH + " = ? AND "
                                + DownloadTable.DownloadInfo.BLOCK_ID + " = ?";

    /** get selectionArgs */
    private static String[] selectionArgs(DownloadInfo info){
        return new String[]{info.getUrl(), info.getFilePath(), String.valueOf(info.getBlockId())};
    }

    /**
     * insert a download info
     * @param context
     * @param info {@link DownloadInfo}
     * @return
     */
    public static long insertDownloadInfo(Context context, DownloadInfo info){
        VolleyLog.d(TAG, "insertDownloadInfo... start");
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = createContentValues(info);
        Uri uri = resolver.insert(DOWNLOAD_CONTENT_URI, values);
        String itemId = uri.getPathSegments().get(1);
        VolleyLog.d(TAG, "insertDownloadInfo... end--> new item id : " + itemId);
        return Integer.valueOf(itemId).longValue();
    }

    /**
     * update a {@link DownloadInfo}
     * @param context
     * @param info
     * @param values
     * @return
     */
    public static boolean updateDownloadInfo(Context context, DownloadInfo info, ContentValues values){
        ContentResolver resolver = context.getContentResolver();
        String[] selectionArgs = selectionArgs(info);
        int count = resolver.update(DOWNLOAD_CONTENT_URI, values, WHERE, selectionArgs);
        return count > 0;
    }

    /**
     * update download progress
     * @param context
     * @param info
     * @param completeSize
     * @return
     */
    public static boolean updateDownloadProgress(Context context, DownloadInfo info, long completeSize){
        ContentValues values = new ContentValues();
        values.put(DownloadTable.DownloadInfo.COMPLETE_SIZE, String.valueOf(completeSize));
        return updateDownloadInfo(context, info, values);
    }

    /**
     * update download state
     * @param context
     * @param info
     * @param state
     * @return
     */
    public static boolean updateDownloadState(Context context, DownloadInfo info, int state){
        ContentValues values = new ContentValues();
        values.put(DownloadTable.DownloadInfo.STATE, state);
        return updateDownloadInfo(context, info, values);
    }

    /**
     * update download state and progress
     * @param context
     * @param info
     * @param completeSize
     * @param state
     * @return
     */
    public static boolean updateDownloadStateAndPrgress(Context context, DownloadInfo info, long completeSize, int state){
        ContentValues values = new ContentValues();
        values.put(DownloadTable.DownloadInfo.COMPLETE_SIZE, String.valueOf(completeSize));
        values.put(DownloadTable.DownloadInfo.STATE, state);
        return updateDownloadInfo(context, info, values);
    }

    /**
     * query download info
     * @param context
     * @param info
     * @return
     */
    public static Cursor queryDownloadInfo(Context context, DownloadInfo info){
        VolleyLog.d(TAG, "queryDownloadInfo... start");
        ContentResolver resolver = context.getContentResolver();
        String selection = WHERE;
        String[] selectionArgs = selectionArgs(info);
        Cursor cursor = resolver.query(DOWNLOAD_CONTENT_URI, null, selection, selectionArgs, DEFAULT_SORT_ORDER);
        VolleyLog.d(TAG, "queryDownloadInfo... end");
        return cursor;
    }

    /**
     * is this download info exist
     * @param context
     * @param info
     * @return
     */
    public static boolean isDownloadInfoExist(Context context, DownloadInfo info){
        Cursor cursor = null;
        try {
            cursor = queryDownloadInfo(context, info);
            if (cursor != null && cursor.getCount() != 0){
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return false;
    }

    /**
     * create ContentValues
     * @param info {@link DownloadInfo}
     * @return
     */
    private static ContentValues createContentValues(DownloadInfo info){
        ContentValues values = new ContentValues();
        values.put(DownloadTable.DownloadInfo.URL, info.getUrl());
        values.put(DownloadTable.DownloadInfo.FILE_PATH, info.getFilePath());
        values.put(DownloadTable.DownloadInfo.FILE_SIZE, String.valueOf(info.getFileSize()));
        values.put(DownloadTable.DownloadInfo.START_POS, String.valueOf(info.getStartPos()));
        values.put(DownloadTable.DownloadInfo.END_POS, String.valueOf(info.getEndPos()));
        values.put(DownloadTable.DownloadInfo.COMPLETE_SIZE, String.valueOf(info.getCompleteSize()));
        values.put(DownloadTable.DownloadInfo.BLOCK_ID, info.getBlockId());
        values.put(DownloadTable.DownloadInfo.BLOCK_COUNT, info.getBlockCount());
        values.put(DownloadTable.DownloadInfo.STATE, info.getState());
        return values;
    }
}
