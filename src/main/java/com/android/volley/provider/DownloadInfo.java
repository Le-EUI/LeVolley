package provider;

/**
 * Created by dingding on 30/05/16.
 */
public class DownloadInfo{
    private String url;
    private String filePath;
    private long fileSize;
    private long startPos;
    private long endPos;
    private long completeSize;
    private int blockId;
    private int blockCount;
    private int state;

    public DownloadInfo(String url, String filePath, long fileSize, long startPos, long endPos, long completeSize,
                        int blockId, int blockCount, int state){
        this.url = url;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.startPos = startPos;
        this.endPos = endPos;
        this.completeSize = completeSize;
        this.blockId = blockId;
        this.blockCount = blockCount;
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
