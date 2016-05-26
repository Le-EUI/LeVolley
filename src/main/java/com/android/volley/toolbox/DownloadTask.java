package main.java.com.android.volley.toolbox;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import java.io.File;

/**
 * Created by koplee on 16-5-24.
 */
public class DownloadTask implements Runnable {
    //下载开始位置
    private static long startPos;
    //下载结束位置
    private static long endPos;
    //下载完成大小
    private static long completeSize;
    //下载块的id
    private static int blockId;
    //下载块的数量
    private static int blockCount;
    //保存路径
    private static String mSavePath;
    //文件名
    private static String mFileName;
    //消息队列
    public static RequestQueue mRequestQueue;


    public DownloadTask(long startPos, long endPos, long completeSize, int blockId,
                        int blockCount, String mSavePath, String mFileName, RequestQueue mRuestQueue){
        this.startPos = startPos;
        this.endPos = endPos;
        this.completeSize = completeSize;
        this.blockId = blockId;
        this.blockCount = blockCount;
        this.mSavePath = mSavePath;
        this.mFileName = mFileName;
        this.mRequestQueue = mRuestQueue;
    }

    /**
     * 开始下载文件
     * @param startPos 下载开始位置
     * @param endPos　下载结束位置
     * @param completeSize　下载完成大小
     * @param blockId　下载块的id
     * @param blockCount 下载块的数量
     */
    private void download(long startPos, long endPos, long completeSize,
                          final int blockId, final int blockCount, String mSavePath,
                          String mFileName, RequestQueue mRequestQueue){
        /*final String filePath = mSavePath + File.separator + mFileName;
        VolleyLog.d("download...filePath: " + filePath + ", startPos: " + startPos + ", endPos: " + endPos
                + ", completeSize: " + completeSize + ", blockId: " + blockId + ", blockCount: " + blockCount);
        toolbox.DownloadRequest request = new toolbox.DownloadRequest(
                getUrl(), filePath, startPos, endPos, completeSize, blockId, blockCount,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //VolleyLog.d("success: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.e("error:" + filePath + ", blockId: " + blockId + ", blockCount: " + blockCount);
                        error.printStackTrace();
                    }
                });
        request.setLoadingListener(new Response.LoadingListener(){
            @Override
            public void onLoading(Type type, long startPos, long endPos, long completeSize, int blockId, int blockCount) {
                VolleyLog.d("type: " + type + ", startPos: " + startPos + ", endPos: " + endPos
                        + ", completeSize: " + completeSize + ", blockId: " + blockId + ", blockCount: " + blockCount);
            }
        });
        if (mRequestQueue != null){
            mRequestQueue.add(request);
        }*/
    }

    @Override
    public void run() {
        download(startPos, endPos, completeSize, blockId, blockCount, mSavePath, mFileName, mRequestQueue);
    }


}
