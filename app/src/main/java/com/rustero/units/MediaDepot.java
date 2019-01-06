package com.rustero.units;


import android.util.Log;

import java.util.LinkedList;



public class MediaDepot {

    private static final String TAG = "MediaApi.Depot";
    public volatile boolean mPosted;
    //public boolean mActing;

    private int length;
    private final Object mLock = new Object();
    private LinkedList<MediaSample> stock, queue;

    public static final int STATE_IDLE = 0;
    public static final int STATE_PLAY = 1;
    public int state = STATE_IDLE;
    public RillC mediaInfo;


    public void reset(int aLength, int aRoom, RillC aMediaInfo) {
        mediaInfo = aMediaInfo;
        length = aLength;
        queue = new LinkedList<MediaSample>();
        stock = new LinkedList<MediaSample>();
        for (int i=0; i<aLength; i++) {
            stock.add(new MediaSample(aRoom));
        }
        clear();
    }



    public void clear() {
        mPosted = false;
        //mActing = false;
        while (queue.size() > 0) {
            MediaSample sam = queue.get(0);
            queue.remove(0);
            stock.add(sam);
        }
    }



    public MediaSample peekSample() {
        MediaSample result = null;
        long took = System.currentTimeMillis();
        synchronized (mLock) {
            if (!stock.isEmpty()) {
                result = stock.get(0);
                stock.remove(0);
            }
        }
        took = System.currentTimeMillis() - took;
        if (took > 11) Log.i(TAG, "####### Depot.peekSample " + took);
        return result;
    }



    public boolean postSample(MediaSample aSample) {
        boolean result = false;
        long took = System.currentTimeMillis();
        synchronized (mLock) {
            queue.add(aSample);
//            if (!mActing)
//                mActing = (queue.size() == length);
            //mPosted = mActing;
            mPosted = true;
            result = true;
        }
        took = System.currentTimeMillis() - took;
        if (took > 11) Log.i(TAG, "####### Depot.postSample " + took);

        return result;
    }




    public MediaSample pullSample() {
        MediaSample result = null;
        long took = System.currentTimeMillis();
        synchronized (mLock) {
            if (!queue.isEmpty()) {
                result = queue.get(0);
                queue.remove(0);
                //mPosted = (mActing && (queue.size() > 0));
                mPosted = (queue.size() > 0);
            }
        }
        took = System.currentTimeMillis() - took;
        if (took > 11) Log.i(TAG, "####### Depot.pullSample " + took);
        return result;
    }



    public void giveSample(MediaSample aSample) {
        synchronized (mLock) {
            stock.add(aSample);
        }
    }



//        private static void fillBytes(ByteBuffer aBuffer, int aFrom, int aSize) {
//            aBuffer.clear();
//            for (int i=0; i<aSize; i++) {
//                byte b = (byte) (aFrom + i);
//                aBuffer.put(b);
//            }
//            aBuffer.flip();
//        }


}
