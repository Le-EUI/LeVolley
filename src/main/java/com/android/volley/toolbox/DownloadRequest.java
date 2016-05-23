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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by dingding on 23/05/16.
 */
public class DownloadRequest extends Request<String>{
    private final Listener<String> mListener;
    private String mFilePath;
    private long mStartPos;
    private long mEndPos;
    private long mCompeleteSize;
    RandomAccessFile randomAccessFile = null;

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
    public DownloadRequest(String url, String savePath, long startPos, long endPos, long completeSize, Listener<String> listener, ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
        mFilePath = savePath;
        mStartPos = startPos;
        mEndPos = endPos;
        mCompeleteSize = completeSize;
    }

    @Override
    protected void deliverResponse(String response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        return null;
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
        if (statusCode < 300){
            try {
                randomAccessFile = new RandomAccessFile(mFilePath, "rwd");
                randomAccessFile.seek(mStartPos + mEndPos);
                InputStream inputStream = httpResponse.getEntity().getContent();
                if (inputStream == null){
                    throw new ServerError();
                }
                if (isCanceled()) {
                    throw new CanceledError();
                }
                byte buffer[] = new byte[4 * 1024];
                int length = 0;
                while((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    randomAccessFile.write(buffer, 0, length);
                    mCompeleteSize += length;
                    if (isCanceled()) {
                        throw new CanceledError();
                    }
                }
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
}
