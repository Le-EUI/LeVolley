package main.java.com.android.volley.toolbox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 生成一个线程池
 * 取消线程池中的任务
 * 查询线程池中是否包含某一个任务
 *
 * Created by koplee on 16-5-24.
 */
public class ThreadManager {

    public static final String DEFAULT_SINGLE_POOL_NAME = "DEFAULT_SINGLE_POOL_NAME";

    public static int threadNums;

    private static ThreadPoolProxy mLongPool = null;
    private static Object mLongLock = new Object();

    private static ThreadPoolProxy mShortPool = null;
    private static Object mShortLock = new Object();

    private static ThreadPoolProxy mDownloadPool = null;
    private static Object mDownloadLock = new Object();

    private static Map<String, ThreadPoolProxy> mMap = new HashMap<String, ThreadPoolProxy>();
    private static Object mSingleLock = new Object();

    public ThreadManager(int threadNums){
        this.threadNums = threadNums;
    }

    //获取下载线程
    public static ThreadPoolProxy getDownloadPool(){
        synchronized (mDownloadLock){
            if(mDownloadPool == null){
                mDownloadPool = new ThreadPoolProxy(threadNums, threadNums, 5L);
            }
            return mDownloadPool;
        }
    }

    //获取一个用于执行长耗时任务的线程池，避免和短耗时任务处在同一个队列而阻塞了重要的短耗时任务，通常用来联网操作
    public static ThreadPoolProxy getLongPool(){
        synchronized (mLongLock){
            if(mLongPool == null){
                mLongPool = new ThreadPoolProxy(5, 5, 5L);
            }
            return mLongPool;
        }
    }

    //获取一个用于执行短耗时任务的线程池，避免和短耗时任务处在同一个队列而阻塞了重要的长耗时任务，通常用来执行本地IO/SQL
    public static ThreadPoolProxy getShortPool(){
        synchronized (mLongLock){
            if(mShortLock == null){
                mShortPool = new ThreadPoolProxy(2, 2, 5L);
            }
            return mShortPool;
        }
    }

    //获取一个单线程池，所有任务将会被按照哦加入的顺序执行，面除了同步开销的问题
    public static ThreadPoolProxy getSinglePool(){
        return getSinglePool(DEFAULT_SINGLE_POOL_NAME);
    }

    //获取一个单线程池，所有任务江会被按照加入的顺序执行，免除了同步开销的问题
    public static ThreadPoolProxy getSinglePool(String name){
        synchronized (mSingleLock){
            ThreadPoolProxy singlePool = mMap.get(name);
            if(singlePool == null){
                singlePool = new ThreadPoolProxy(1, 1, 5L);
                mMap.put(name, singlePool);
            }
            return singlePool;
        }
    }

    public static class ThreadPoolProxy{
        private ThreadPoolExecutor mPool;
        private int mCorePoolSize;
        private int mMaximumPoolSize;
        private long mKeepAliveTime;

        private ThreadPoolProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime){
            mCorePoolSize = corePoolSize;
            mMaximumPoolSize = maximumPoolSize;
            mKeepAliveTime = keepAliveTime;
        }

        //执行任务，当线程池处于关闭，将会重新创建新的线程池
        public synchronized void execute(Runnable run){
            if(run == null){
                return;
            }

            /*
            * 参数说明：
            * 当线程池中的线程数目小于mCorePoolSize,直接创建新的线程加入线程池执行任务
            * 当线程池中的线程数目大于mCorePoolSize,将会把任务放入任务队列BlockingQueue中
            * 但是当总线程数大于mMaximumPoolSize时，将会抛出异常，交给RejectedExecutionHandler处理
            * mKeepAliveTime是线程执行完任务后，且队列中没有可以执行的任务，存活的时间，而后参数是时间单位
            * ThreadFactory是每次创建新的线程工厂
            * */
            if(mPool == null || mPool.isShutdown()){
                mPool = new ThreadPoolExecutor(mCorePoolSize, mMaximumPoolSize, mKeepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
            }
            mPool.execute(run);
        }

        //取消线程池中某个还未执行的任务
        public synchronized void cancel(Runnable run){
            if(mPool != null && (!mPool.isShutdown() || mPool.isTerminating())){
                mPool.getQueue().remove(run);
            }
        }

        //取消线程池中某个还未执行的任务
        public synchronized boolean contains(Runnable run){
            if(mPool != null && (!mPool.isShutdown() || mPool.isTerminating())){
                return mPool.getQueue().contains(run);
            }else{
                return false;
            }
        }

        //立刻关闭线程池，并且正在执行的任务也将会被中断
        public void stop(){
            if(mPool != null && (!mPool.isShutdown() || mPool.isTerminating())){
                mPool.shutdownNow();
            }
        }

        //平缓关闭单任务线程池，但是会确保所有已经加入的任务都将会被执行完毕才关闭
        public synchronized void shutdown(){
            if(mPool != null && (!mPool.isShutdown() || mPool.isTerminating())){
                mPool.shutdownNow();
            }
        }

    }
}

