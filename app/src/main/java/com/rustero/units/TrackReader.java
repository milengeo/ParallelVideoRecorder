
package com.rustero.units;


import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.nio.ByteBuffer;






public class TrackReader {

    private static final String TAG = "TrackReader";
    private String mSource;
    private MediaExtractor mExtor;
    private MediaFormat mFormat;
    private String mMime;

    public TrackReader() {
        mExtor = new MediaExtractor();
    }


    public void setDataSource(String aSource) {
        try {
            mExtor.setDataSource(aSource);
            mSource = aSource;
        }
        catch (Exception ex) {}
    }


    public int getTrackCount() {
        return mExtor.getTrackCount();
    }


    public void selectTrack(int aIndex) {
        mExtor.selectTrack(aIndex);
    }


    public MediaFormat getTrackFormat(int aIndex) {
        return mExtor.getTrackFormat(aIndex);
    }


    public void release() {
        mExtor.release();
    }


    public int readSampleData(ByteBuffer aBuffer, int aOffset) {
        return mExtor.readSampleData(aBuffer, aOffset);
    }


    public long getSampleTime() {
        return mExtor.getSampleTime();
    }


    public void advance() {
        mExtor.advance();
    }


}




