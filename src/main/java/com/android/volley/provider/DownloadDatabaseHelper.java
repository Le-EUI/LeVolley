package provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by dingding on 22/05/16.
 */
public class DownloadDatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "DownloadDatabaseHelper";

    private static DownloadDatabaseHelper sInstance = null;
    // 数据库名
    static final String DATABASE_NAME = "download.db";
    // 数据库版本
    static final int DATABASE_VERSION  = 1;

    private DownloadDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Return a singleton helper for the backup provider
     */
    /* package */ static synchronized DownloadDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DownloadDatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DownloadDatabaseHelper onCreate()-------->DATABASE_VERSION: " + DATABASE_VERSION);
        createTables(db);
        createIndices(db);
        createDeletionTriggers(db);
        createViews(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ".");

        switch (oldVersion) {
            case 1:
                if (newVersion <= 1) {
                    return;
                }
                db.beginTransaction();
                try {
                    upgradeDatabaseToVersion2(db);
                    db.setTransactionSuccessful();
                } catch (Throwable ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    break;
                } finally {
                    db.endTransaction();
                }
            return;

        }

        Log.e(TAG, "Destroying all old data.");
        dropAll(db);
        onCreate(db);
    }

    /**
     * 创建表
     * @param db
     */
    private void createTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadTable.DownloadInfo.TABLE + " (" +
                DownloadTable.DownloadInfo._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DownloadTable.DownloadInfo.URL + " TEXT," +
                DownloadTable.DownloadInfo.FILE_PATH + " TEXT," +
                DownloadTable.DownloadInfo.FILE_SIZE + " TEXT," +
                DownloadTable.DownloadInfo.START_POS + " TEXT," +
                DownloadTable.DownloadInfo.END_POS + " TEXT," +
                DownloadTable.DownloadInfo.COMPLETE_SIZE + " TEXT," +
                DownloadTable.DownloadInfo.BLOCK_ID + " INTEGER,"  +
                DownloadTable.DownloadInfo.BLOCK_COUNT + " INTEGER,"  +
                DownloadTable.DownloadInfo.STATE + " INTEGER DEFAULT " + DownloadTable.DownloadInfo.INIT +
                ");");
    }

    /**
     * 创建索引
     * @param db
     */
    private void createIndices(SQLiteDatabase db){

    }

    /**
     * 创建触发器
     * .
     * @param db
     */
    private void createDeletionTriggers(SQLiteDatabase db){

    }

    /**
     * 创建视图
     * @param db
     */
    private void createViews(SQLiteDatabase db) {
    }

    /**
     *  升级数据库1
     *
     * */
    private void upgradeDatabaseToVersion2(SQLiteDatabase db) {
    }

    /**
     * 册除所有相关数据
     * @param db
     */
    private void dropAll(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + DownloadTable.DownloadInfo.TABLE);
    }
}
