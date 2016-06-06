/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package toolbox;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import com.android.volley.RequestQueue;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class DownloadSizeRequest extends Request<Long> {
    /**下载文件划分块的大小*/
    private static final long BLOCK_SIZE = 2 * 1024 * 1024;

    private static final int MSG_POST_PROGRESS = 1000;

    private final Listener<Long> mListener;

    private String mSavePath;//保存路径
    private String mFileName;//文件名

    private RequestQueue mRequestQueue;

    /** 块下载完成大小数组, 代表每一个块下载完成的大小　*/
    private long[] mCompleteSize;

    /** 上一次下载的进度 */
    private int mProgress;

    /**
     * 主线程handler, 用来对所有块发送来的进度进行统计
     */
    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_POST_PROGRESS:
                    handlerProgress(msg);
                    break;
            }
        }
    };
    /**
     * Creates a new request with the given method.
     *
     * @param method the request {@link Method} to use
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadSizeRequest(int method, RequestQueue queue, String url, String savePath, String fileName, Listener<Long> listener,
                               ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mRequestQueue = queue;
        mSavePath = savePath;
        mFileName = fileName;
    }

    /**
     * Creates a new GET request.
     *
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadSizeRequest(RequestQueue queue, String url, String savePath, String fileName, Listener<Long> listener, ErrorListener errorListener) {
        this(Method.GET, queue, url, savePath, fileName, listener, errorListener);
    }

    @Override
    public Type getType() {
        return Type.DOWNLOAD_SIZE;
    }

    @Override
    protected void deliverResponse(Long response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<Long> parseNetworkResponse(NetworkResponse response) {
        //获取文件长度
        long parsed = response.httpResponse.getEntity().getContentLength();
        VolleyLog.d("fileSize: " + parsed);
        //根据文件长度计算需要开启的线程数
        int threadNum = (int) (parsed / BLOCK_SIZE) + 1;
        mRequestQueue.threadNum = threadNum;
        if (parsed > 0 && !TextUtils.isEmpty(mSavePath) && !TextUtils.isEmpty(mFileName)){
            creatFile(parsed);
            handlerDownload(parsed);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

    /**
     *　根据文件大小创建文件
     * @param fileSize 文件大小
     */
    private void creatFile(long fileSize){
        VolleyLog.d("savePath: " + mSavePath + ", fileName: " + mFileName);
        File dir = new File(mSavePath);
        if(!dir.exists()) {
            if(dir.mkdirs()) {
                VolleyLog.d("mkdirs success.");
            }
        }
        File file = new File(mSavePath, mFileName);
        RandomAccessFile randomFile= null;
        try {
            randomFile = new RandomAccessFile(file,"rwd");
            randomFile.setLength(fileSize);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                randomFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据文件大小进行分块下载
     * @param size 需要下载的文件大小
     */
    private void handlerDownload(long size){
        VolleyLog.d("download size: " + size);
        if (size > BLOCK_SIZE){
            int blockCount = (int) (size / BLOCK_SIZE) + 1;
            mCompleteSize = new long[blockCount];
            if (blockCount > 1){
                long startPos = 0;
                long endPos = -1;
                for (int i = 0; i < blockCount; i++){
                    startPos = endPos + 1;
                    if (i == blockCount - 1){
                        endPos = size;
                    } else {
                        endPos = startPos + BLOCK_SIZE;
                    }

                    download(size, startPos, endPos, 0, i, blockCount);
                }
            }
        } else {
            //只启动一个线程下载
            mCompleteSize = new long[1];
            download(size, 0, size, 0, 0, 1);
        }
    }

    /**
     * 开始下载文件
     * @param startPos 下载开始位置
     * @param endPos　下载结束位置
     * @param completeSize　下载完成大小
     * @param blockId　下载块的id
     * @param blockCount 下载块的数量
     */
    private void download(long fileSize, long startPos, long endPos, long completeSize, final int blockId, final int blockCount){
        final String filePath = mSavePath + File.separator + mFileName;
        VolleyLog.d("download...filePath: " + filePath + ", startPos: " + startPos + ", endPos: " + endPos
                + ", completeSize: " + completeSize + ", blockId: " + blockId + ", blockCount: " + blockCount);
        toolbox.DownloadRequest request = new toolbox.DownloadRequest(
                getUrl(), filePath, fileSize, startPos, endPos, completeSize, blockId, blockCount,
                new Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //VolleyLog.d("success: " + response);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.e("error:" + filePath + ", blockId: " + blockId + ", blockCount: " + blockCount);
                        error.printStackTrace();
                    }
                });
        request.setLoadingListener(new Response.LoadingListener(){
            @Override
            public void onLoading(Request.Type type, long fileSize, long startPos, long endPos, long completeSize, int blockId, int blockCount) {
                VolleyLog.d("type: " + type + ", startPos: " + startPos + ", endPos: " + endPos
                + ", completeSize: " + completeSize + ", blockId: " + blockId + ", blockCount: " + blockCount);
                postProgressToHandler(fileSize, completeSize, blockId);
            }
        });
        request.attach(mContext);
        request.setTag(getTag());
        if (mRequestQueue != null){
            mRequestQueue.add(request);
        }
    }

    /**
     * 将每个块的进度发送到handler顺序统计进度
     * @param fileSize
     * @param completeSize
     * @param blockId
     */
    private void postProgressToHandler(long fileSize, long completeSize, int blockId){
        Object[] obj = new Object[3];
        obj[0] = fileSize;
        obj[1] = completeSize;
        obj[2] = blockId;
        Message msg = mHandler.obtainMessage(MSG_POST_PROGRESS, obj);
        msg.sendToTarget();
    }

    /**
     * 对块传来的进度进行重新计算
     * @param msg
     */
    private void handlerProgress(Message msg){
        Object[] obj = (Object[]) msg.obj;
        long fileSize = (Long) obj[0];
        long completeSize = (Long) obj[1];
        int blockId = (Integer) obj[2];
        mCompleteSize[blockId] = completeSize;

        long totalCompleteSize = 0;
        for (long complete : mCompleteSize){
            totalCompleteSize += complete;
        }

        int progress = (int) (100 * totalCompleteSize / fileSize);
        if (progress - mProgress >= 1){
            postProgress(getType(), totalCompleteSize, progress);
            mProgress = progress;
        }
    }

    /**
     * 获得文件路径
     * @return
     */
    private String getFilePath(){
        if (mSavePath.endsWith(File.separator)){
            return mSavePath + mFileName;
        }
        return mSavePath + File.separator + mFileName;
    }

    @Override
    public void cancel() {
        super.cancel();
        mRequestQueue.cancelAll(getTag());
    }

    @Override
    public void pause() {
        super.pause();
        mRequestQueue.pauseAll(getTag());
    }

    @Override
    public void resume() {
        super.resume();
        mRequestQueue.resumeAll(getTag());
    }

    @Override
    public Request<?> setTag(Object tag) {
        return super.setTag(getFilePath()+getUrl());
    }
}
