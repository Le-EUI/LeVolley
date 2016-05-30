package provider;

/**
 * Created by dingding on 30/05/16.
 */
public class DownloadInfo implements Comparable<DownloadInfo>{
    private String url;
    private String file_name;
    private long file_size;
    private long start_pos;
    private long end_pos;
    private long complete_size;
    private int block_id;
    private int block_count;
    private int state;

    @Override
    public int compareTo(DownloadInfo another) {
        return 0    ;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public long getStart_pos() {
        return start_pos;
    }

    public void setStart_pos(long start_pos) {
        this.start_pos = start_pos;
    }

    public long getEnd_pos() {
        return end_pos;
    }

    public void setEnd_pos(long end_pos) {
        this.end_pos = end_pos;
    }

    public long getComplete_size() {
        return complete_size;
    }

    public void setComplete_size(long complete_size) {
        this.complete_size = complete_size;
    }

    public int getBlock_id() {
        return block_id;
    }

    public void setBlock_id(int block_id) {
        this.block_id = block_id;
    }

    public int getBlock_count() {
        return block_count;
    }

    public void setBlock_count(int block_count) {
        this.block_count = block_count;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
