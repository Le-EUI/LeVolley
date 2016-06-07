package com.android.volley.toolbox;

import android.database.Cursor;
import android.os.SystemClock;
import com.android.volley.*;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import com.android.volley.provider.DownloadInfo;
import com.android.volley.provider.DownloadProviderTracker;
import com.android.volley.provider.DownloadTable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dingding on 23/05/16.
 */
public class DownloadBlockRequest extends Request<String>{
    private static final String TAG = "DownloadBlockRequest";

    /** 暂停超时时间 */
    private static final long PAUSE_TIME_OUT = 2 * 60 * 1000;

    private final Listener<String> mListener;
    private String mFilePath;
    private long mFileSize;
    private long mStartPos;
    private long mEndPos;
    private long mCompeleteSize;
    private int mBlockId;
    private int mBlockCount;

    private DownloadInfo mDownloadInfo;

    /**
     * Creates a new request with the given method.
     *
     * @param method the request {@link Method} to use
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadBlockRequest(int method, String url, String filePath, long fileSize, long startPos, long endPos, long completeSize, int blockId, int blockCount,
                                Listener<String> listener, ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mFilePath = filePath;
        mFileSize = fileSize;
        mStartPos = startPos;
        mEndPos = endPos;
        mCompeleteSize = completeSize;
        mBlockId = blockId;
        mBlockCount = blockCount;
        mDownloadInfo = new DownloadInfo(url, mFilePath, mFileSize, mStartPos, mEndPos, mCompeleteSize,
                mBlockId, mBlockCount, State.WAITING.code);

        checkDownloadInfo();
    }

    /**
     * Creates a new GET request.
     *
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadBlockRequest(String url, String filePath, long fileSize, long startPos, long endPos, long completeSize, int blockId, int blockCount, Listener<String> listener, ErrorListener errorListener) {
        this(Method.GET, url, filePath, fileSize, startPos, endPos, completeSize, blockId, blockCount, listener, errorListener);
    }

    @Override
    public Type getType() {
        return Type.DOWNLOAD;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        File downloadFile = new File(mFilePath);
        long fileLen = 0;
        if (downloadFile.isFile() && downloadFile.exists()) {
            fileLen = downloadFile.length();
        }
        VolleyLog.d("getHeaders...fileLen: " + fileLen + ", mStartPos: " + mStartPos
                + ", mCompeleteSize: " + mCompeleteSize + ", mEndPos: " + mEndPos);
        if (fileLen > 0) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Range", "bytes=" + String.valueOf(mStartPos + mCompeleteSize) + "-" + String.valueOf(mEndPos));
            VolleyLog.d("getHeaders...heads: " + headers.entrySet().toString());
            return headers;
        }
        return super.getHeaders();
    }

    @Override
    protected void deliverResponse(String response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String filename = new String(response.data);
        if (response.data.length > 0) {
            return Response.success(filename, null);
        } else {
            return Response.error(new ParseError(response));
        }
    }

    public boolean isSupportRange(final HttpResponse response) {
        if (response == null) return false;
        Header header = response.getFirstHeader("Accept-Ranges");
        if (header != null) {
            return "bytes".equals(header.getValue());
        }
        header = response.getFirstHeader("Content-Range");
        if (header != null) {
            String value = header.getValue();
            return value != null && value.startsWith("bytes");
        }
        return false;
    }

    @Override
    public byte[] handleRawResponse(HttpResponse httpResponse) throws IOException, ServerError, CanceledError {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        VolleyLog.d("handleRawResponse...statusCode: " + statusCode);
        if (statusCode < 300){
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(mFilePath, "rwd");
                randomAccessFile.seek(mStartPos + mCompeleteSize);
                InputStream inputStream = httpResponse.getEntity().getContent();
                if (inputStream == null){
                    throw new ServerError();
                }
                if (isCanceled()) {
                    throw new CanceledError();
                }
                updateState(State.LOADING.code);
                postProgress();
                byte buffer[] = new byte[4 * 1024];
                int length = 0;
                while((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    if (isCanceled()) {
                        throw new CanceledError();
                    }
                    if (isPaused()){
                        long pausedTime = SystemClock.elapsedRealtime();
                        while (isPaused()){
                            long currentTime = SystemClock.elapsedRealtime();
                            if (currentTime - pausedTime > PAUSE_TIME_OUT){
                                cancel();
                                break;
                            }
                        }
                        if (isPaused()){
                            break;
                        }
                    }
                    randomAccessFile.write(buffer, 0, length);
                    mCompeleteSize += length;
                    postProgress();
                }
                postProgress();
                return mFilePath.getBytes();
            } catch (FileNotFoundException e){
                VolleyLog.e("filepath not exit");
                e.printStackTrace();
            } finally {
                //inputStream.close();
                randomAccessFile.close();
            }
        } else if (statusCode == 416) {
            throw new IOException("may be the file have been download finished.");
        } else {
            throw new IOException();
        }
        return super.handleRawResponse(httpResponse);
    }

    @Override
    public void cancel() {
        if (isCanceled()){
            return;
        }
        updateState(State.CANCEL.code, true);
        super.cancel();
    }

    @Override
    public void pause() {
        if (isPaused()){
            return;
        }
        updateState(State.PAUSE.code, true);
        super.pause();
    }

    @Override
    public void resume() {
        if (!isPaused()){
            return;
        }
        updateState(State.LOADING.code);
        super.resume();
    }

    /**
     * 发送进度
     */
    private void postProgress(){
        postProgress(Type.DOWNLOAD, mFileSize, mStartPos, mEndPos, mCompeleteSize, mBlockId, mBlockCount);
    }

    /**
     * 检测该DownloadInfo
     */
    private void checkDownloadInfo(){
        if (!isSupportBreakpoint()){
            VolleyLog.e(TAG, "not support breakpoint");
            return;
        }
        /**检测文件存不存在*/

        /**检测数据库存不存在该DownloadInfo*/
        if (isDownloadInfoExist()){
            updateState(State.WAITING.code);
        } else {
            DownloadProviderTracker.insertDownloadInfo(mContext, mDownloadInfo);
        }
    }

    /**
     * 该downloadInfo是否存在
     * 注：如果存在的话重新赋值mCompleteSize
     * @return
     */
    private boolean isDownloadInfoExist(){
        Cursor cursor = null;
        try {
            cursor = DownloadProviderTracker.queryDownloadInfo(mContext, mDownloadInfo);
            if (cursor != null && cursor.moveToFirst()){
                String complete = cursor.getString(cursor.getColumnIndex(DownloadTable.DownloadInfo.COMPLETE_SIZE));
                mCompeleteSize = Long.valueOf(complete);
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
     * 更新下载状态
     * @param state
     */
    private void updateState(int state){
        updateState(state, false);
    }

    /**
     *  更新下载状态
     * @param state
     * @param updateProgress　是否更新进度
     */
    private void updateState(int state, boolean updateProgress){
        if (!isSupportBreakpoint()){
            return;
        }
        if (updateProgress){
            DownloadProviderTracker.updateDownloadStateAndPrgress(mContext, mDownloadInfo, mCompeleteSize, state);
        } else {
            DownloadProviderTracker.updateDownloadState(mContext, mDownloadInfo, state);
        }
    }

    /**
     * 更新下载进度
     */
    private void updateProgress(){
        if (!isSupportBreakpoint()){
            return;
        }
        DownloadProviderTracker.updateDownloadProgress(mContext, mDownloadInfo, mCompeleteSize);
    }
    /**
     * download state
     */
    public enum State{
        INIT(DownloadTable.DownloadInfo.INIT),
        WAITING(DownloadTable.DownloadInfo.WAITING),
        LOADING(DownloadTable.DownloadInfo.LOADING),
        CANCEL(DownloadTable.DownloadInfo.CANCEL),
        PAUSE(DownloadTable.DownloadInfo.PAUSE),
        SUCCESS(DownloadTable.DownloadInfo.SUCCESS),
        FAIL(DownloadTable.DownloadInfo.FAIL);

        public int code;

        State(int code){
            this.code = code;
        }
    }
}
