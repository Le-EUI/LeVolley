package toolbox;

import com.android.volley.*;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dingding on 23/05/16.
 */
public class DownloadRequest extends Request<String>{
    private final Listener<String> mListener;
    private String mFilePath;
    private long mStartPos;
    private long mEndPos;
    private long mCompeleteSize;
    private int mBlockId;
    private int mBlockCount;

    /**
     * Creates a new request with the given method.
     *
     * @param method the request {@link Method} to use
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadRequest(int method, String url, Listener<String> listener,
                               ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mType = Type.DOWNLOAD;
    }

    /**
     * Creates a new GET request.
     *
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadRequest(String url, String savePath, long startPos, long endPos, long completeSize, int blockId, int blockCount, Listener<String> listener, ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
        mFilePath = savePath;
        mStartPos = startPos;
        mEndPos = endPos;
        mCompeleteSize = completeSize;
        mBlockId = blockId;
        mBlockCount = blockCount;
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
                postProgress();
                byte buffer[] = new byte[4 * 1024];
                int length = 0;
                while((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    randomAccessFile.write(buffer, 0, length);
                    mCompeleteSize += length;
                    if (isCanceled()) {
                        throw new CanceledError();
                    }
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

    /**
     * 发送进度
     */
    private void postProgress(){
        postProgress(Type.DOWNLOAD, mStartPos, mEndPos, mCompeleteSize, mBlockId, mBlockCount);
    }
}
