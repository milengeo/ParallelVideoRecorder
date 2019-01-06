
package com.rustero.units;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.rustero.App;

import java.io.File;
import java.nio.ByteBuffer;






public class TrackWriter {

    protected static final String TAG = "TrackWriter";
    protected File mFile;

    protected volatile int mTrackCount;
    protected int mAudioTrack, mVideoTrack;
    private MediaMuxer mMuxer;



    synchronized public void attach(File aFile) {
        mTrackCount = 0;
        mFile = aFile;
        try {
            mMuxer = new MediaMuxer(mFile.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
        catch (Exception ex) {
            Log.e(TAG, " *** Tracker attach " + ex.getMessage());
        }
    }



    synchronized public void detach() {
            try {
                if (mMuxer != null) {
                    mTrackCount = 0;
                    mMuxer.stop();
                    mMuxer.release();
                    Log.i(TAG, "mMuxer.cease");
                    mMuxer = null;
                }
            } catch (Exception ex) {
                Log.e(TAG, " *** Tracker detach " + ex.getMessage());
            }
    }



    synchronized public void addAudioTrack(MediaFormat aFormat) {
            try {
                if (mMuxer != null) {
                    if (aFormat != null)
                        mAudioTrack = mMuxer.addTrack(aFormat);
                    Log.i(TAG, " * addAudioTrack " + mAudioTrack);
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



    synchronized public void addVideoTrack(MediaFormat aFormat) {
            try {
                if (mMuxer != null) {
                    if (aFormat != null)
                        mVideoTrack = mMuxer.addTrack(aFormat);
                    Log.i(TAG, " * addVideoTrack " + mVideoTrack);
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



    synchronized public boolean isStarted() {
        return (mTrackCount == 2);
    }



    synchronized public void writeAudio(ByteBuffer byteBuf, MediaCodec.BufferInfo bufInfo) {
        if (!isStarted()) return;
            try {
                if (mMuxer != null)
                    mMuxer.writeSampleData(mAudioTrack, byteBuf, bufInfo);
            }
            catch (Exception ex) {
                Log.e(TAG, "writeAudio " + ex.getMessage());
            }
    }



    public void writeVideo(ByteBuffer byteBuf, MediaCodec.BufferInfo bufInfo) {
        long took = System.currentTimeMillis();
        if (!isStarted()) return;

            try {
                if (mMuxer != null)
                    mMuxer.writeSampleData(mVideoTrack, byteBuf, bufInfo);
            }
            catch (Exception ex) {
                Log.e(TAG, "writeVideo " + ex.getMessage());
            }
        took = System.currentTimeMillis() - took;
        if (took > 22) App.logLine("### long writeVideo " + took);
    }


}




