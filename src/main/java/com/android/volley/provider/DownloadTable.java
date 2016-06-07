package com.android.volley.provider;

/**
 * Created by dingding on 22/05/16.
 */
public class DownloadTable {

    public interface DownloadInfo extends BaseColumns{

        String TABLE = "download_info";

        /**
         * 下载的url
         */
        String URL = "url";

        /**
         * 下载的文件名
         */
        String FILE_PATH = "file_path";

        /**
         * 下载的文件大小
         */
        String FILE_SIZE = "file_size";

        /**
         * 下载的起始位置
         */
        String START_POS = "start_pos";

        /**
         * 下载的结束
         */
        String END_POS = "end_pos";

        /**
         * 当前下载的完成大小
         */
        String COMPLETE_SIZE = "complete_size";

        /**
         * 下载所在文件块的id
         */
        String BLOCK_ID = "block_id";

        /**
         * 下载时文件划分的总块数
         */
        String BLOCK_COUNT = "block_count";

        /**
         * 下载状态
         */
        String STATE = "state";
        int FAIL = 0;/**失败*/
        int SUCCESS = 1;/**成功*/
        int PAUSE = 2;/**暂停*/
        int INIT = 3;/**默认初始化状态*/
        int LOADING = 4;/**正在下载*/
        int CANCEL = 5;/**取消*/
        int WAITING = 6;/**等待中，已经加入下载队列*/
    }
}
