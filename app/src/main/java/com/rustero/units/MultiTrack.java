
package com.rustero.units;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.rustero.App;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;


public class MultiTrack extends Thread {

    private static final String TAG = "MultiTrack";
    //static volatile private boolean mMuxing = false;
    private volatile boolean mQuit = false;
    private volatile boolean mDone = false;
    private File mFile;
    private MediaMuxer mMuxer;

    public long videoBytes, audioBytes;
    private int mAudioTrack, mVideoTrack;
    private int mReso, mAudioCount, mVideoCount;
    private long mLastPull;

    volatile private int mTrackCount;
    private volatile boolean mQueued = false;
    private LinkedList<Chunk> mQueue = new LinkedList<Chunk>();
    private final Object mQueueLock = new Object();

    private static final int[] POOL_EDGE = {1000, 8000, 32000, 128000};
    private static final int[] POOL_DEPTH = {64, 8, 4, 2};
    private int mPoolWidth = POOL_EDGE.length;
    private ArrayList<LinkedList<Chunk>> mPools;
    private final Object mPoolLock = new Object();




    public static class Chunk {
        private int mPoolLine, mTrackIndex;
        private MediaCodec.BufferInfo mBufInfo;
        private ByteBuffer mBufData;

        public void set(int aSize) {
            mBufInfo =  new MediaCodec.BufferInfo();
            mBufData = ByteBuffer.allocate(aSize);
        }

        public void put(int aTrackIndex, ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
            mTrackIndex = aTrackIndex;
            mBufInfo.set(0, aBufInfo.size, aBufInfo.presentationTimeUs, aBufInfo.flags);

            if (mBufData.capacity() < aBufInfo.size)
                mBufData = ByteBuffer.allocate(aBufInfo.size*2);

            aBufData.rewind();
            mBufData.clear();
            mBufData.put(aBufData);
            mBufData.flip();
        }
    }



    private void finish() {
        Log.d(TAG, "Tracker_finish_11");
        mQuit = true;
        try {
            join();
        } catch (Exception ex) {
            Log.d(TAG, " *** Tracker_finish ex: " + ex.getMessage());
        }
        Log.d(TAG, "Tracker_finish_99");
    }



    @Override
    public void run() {
        Log.d(TAG, "Tracker_run_11");
        try {
            while (true) {
                Thread.sleep(1);
                if (mQuit) {
                    Log.d(TAG, "Tracker_need_quit");
                    break;
                }

                long span = System.currentTimeMillis() - mLastPull;
                if (span > 222) {
                    mLastPull = System.currentTimeMillis();
                    pullQueue();
                }

//                if ((span > 999) && (!mMuxing)) {
//                    mMuxing = true;
//                    mLastPull = System.currentTimeMillis();
//                    pullQueue();
//                    mMuxing = false;
//                }

            }
        } catch (Exception ex) {
            Log.d(TAG, " *** Tracker_run ex: " + ex.getMessage());
        }
        mDone = true;
        Log.d(TAG, "Tracker_run_99");
    }



    private void pullQueue() {
        if (!isStarted()) return;
        //Log.d(TAG, "pullQueue " + "  reso:" + mReso + "  queueSize: " + mQueue.size());
        int audioCount = 0;
        int videoCount = 0;

        while (mQueued) {
            long took = System.currentTimeMillis();
            Chunk chunk = null;
            synchronized (mQueueLock) {
                if (mQueue.size() > 0) {
                    chunk = mQueue.get(0);
                    mQueue.remove(0);
                }
                mQueued = mQueue.size() > 0;
            }

            try {
                mMuxer.writeSampleData(chunk.mTrackIndex, chunk.mBufData, chunk.mBufInfo);
                if (chunk.mTrackIndex == mVideoTrack)
                    videoCount++;
                else
                    audioCount++;
                synchronized (mPoolLock) {
                    LinkedList<Chunk> pool = mPools.get(chunk.mPoolLine);
                    pool.add(chunk);
                }
            } catch (Exception ex) {
                Log.e(TAG, " *** writeVideo " + ex.getMessage());
            }

            took = System.currentTimeMillis() - took;
            if (took > 22) App.logLine("### long writeSampleData " + took);
        }

        //Log.d(TAG, "pullQueue " + "  audioCount:" + audioCount + "  videoCount: " + videoCount);
        //Log.d(TAG, "  posi0: "+mPools.get(0).size() + "  posi1: "+mPools.get(1).size() + "  posi2: "+mPools.get(2).size() + "  posi3: " + mPools.get(3).size());
    }







    public void attach(File aFile) {
        mPools = new ArrayList<LinkedList<Chunk>>(POOL_EDGE.length);
        for (int pi=0; pi<mPoolWidth; pi++) {
            LinkedList<Chunk> pool = new LinkedList<Chunk>();
            for (int ci=0; ci<POOL_DEPTH[pi]; ci++) {
                Chunk chunk = new Chunk();
                chunk.mPoolLine = pi;
                chunk.set(POOL_EDGE[pi]*2);
                pool.add(chunk);
            }
            mPools.add(pool);
        }

        mTrackCount = 0;
        mFile = aFile;
        try {
            mMuxer = new MediaMuxer(mFile.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
        catch (Exception ex) {
            Log.e(TAG, " *** Tracker attach " + ex.getMessage());
        }

        start();
        Log.d(TAG, "Tracker_attach_99");
    }



    public void detach() {
        finish();
        try {
            if (mMuxer != null) {
                mTrackCount = 0;
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
                Log.d(TAG, "mMuxer.cease " + mReso);
            }
        } catch (Exception ex) {
            Log.e(TAG, " *** Tracker detach " + ex.getMessage());
        }
        Log.d(TAG, "Tracker_detach_99");
    }





    public void addAudioTrack(MediaFormat aFormat) {
        try {
            if (mMuxer != null) {
                if (aFormat != null)
                    mAudioTrack = mMuxer.addTrack(aFormat);
                Log.d(TAG, " * addAudioTrack " + mAudioTrack);
                mTrackCount++;
                if (mTrackCount == 2) {
                    mMuxer.start();
                }

            }
        }
        catch (Exception ex) {
            Log.e(TAG, " *** Tracker addAudioTrack " + ex.getMessage());
        }
    }



    public void addVideoTrack(MediaFormat aFormat) {
            try {
                if (mMuxer != null) {
                    if (aFormat != null) {
                        mVideoTrack = mMuxer.addTrack(aFormat);
                        mReso = aFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    }
                    Log.d(TAG, " * addVideoTrack " + mVideoTrack + "  reso:" + mReso);
                    mTrackCount++;
                    if (mTrackCount == 2) {
                        mMuxer.start();
                    }
                }
            }
            catch (Exception ex) {
                Log.e(TAG, "addVideoTrack " + ex.getMessage());
            }
    }



    public boolean isStarted() {
        return (mTrackCount == 2) && (mMuxer != null);
    }



    public void writeAudio(ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
        writeSample(mAudioTrack, aBufData, aBufInfo);
        videoBytes += aBufInfo.size;
    }



    public void writeVideo(ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {
        writeSample(mVideoTrack, aBufData, aBufInfo);
        videoBytes += aBufInfo.size;
    }







    private void writeSample(int aTrackIndex, ByteBuffer aBufData, MediaCodec.BufferInfo aBufInfo) {

        Chunk chunk = null;
        int poin = 0;
        long took = System.currentTimeMillis();

        synchronized (mPoolLock) {
            for (int i=mPoolWidth-1; i>0; i--) {
                if (aBufInfo.size >= POOL_EDGE[i]) {
                    poin = i;
                    break;
                }
            }
            //Log.d(TAG, "writeSample_pool_index: " + poin);

            LinkedList<Chunk> pool = mPools.get(poin);
            if (pool.size() == 0) {
                chunk = new Chunk();
                chunk.mPoolLine = poin;
                chunk.set(aBufInfo.size + 16);
                //Log.d(TAG, " *** writeSample_new_chunk: " + aBufInfo.size);
            } else {
                chunk = pool.get(0);
                pool.remove(0);
                if (aBufInfo.size > chunk.mBufData.capacity()) {
                    chunk.mBufData = ByteBuffer.allocate(aBufInfo.size * 2);
                    //Log.d(TAG, "writeSample_resize_chunk: " + aBufInfo.size);
                }
            }
        }

        try {
            chunk.put(aTrackIndex, aBufData, aBufInfo);
            synchronized (mQueueLock) {
                mQueue.add(chunk);
                mQueued = true;
                ///Log.d(TAG, "writeSample_queue_size: " + mQueue.size() + "  poin " + chunk.mPoolLine);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "writeSample " + ex.getMessage());
        }
        took = System.currentTimeMillis() - took;
        if (took > 22) App.logLine("###*** long writeSample " + took);
    }

}




