package com.rustero.units;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.nio.ByteBuffer;


public class RillC {
    public static final int FRAME_RATE = 30;
    private Surface inputSurface;
    public egl.WindowSurface mEncoderSurface;

    public static int detachCount;
    public MultiTrack tracker;
    public MediaCodec codec;
    public MediaFormat videoFormat;
    public int outWidth, outHeight;
    public String outMime;
    public File outFile;
    public String outName;
    public int bitrate = 1000000;
    public MediaCodec.BufferInfo bufferInfo;
    public ByteBuffer bufferData;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    public int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    public int intraTerm = 2;
    private int lastCamWi, lastCamHe, lastSurWi, lastSurHe;
    public final float[] matrix = new float[16];



    public RillC() {
        super();
        bufferInfo = new MediaCodec.BufferInfo();
        Matrix.setIdentityM(matrix, 0);
    }



    public void attach(egl.Core aEglCore) {
        try {
            // Set some properties.  Failing to specify some of these can cause the MediaCodec configure() call to throw an unhelpful exception.
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, outWidth, outHeight);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, intraTerm);

            codec = MediaCodec.createEncoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = codec.createInputSurface();
            mEncoderSurface = new egl.WindowSurface(aEglCore, inputSurface, true);
            codec.start();

            tracker = new MultiTrack();
            tracker.attach(outFile);
        } catch (Exception ex) {
            Log.d("MediaRill", "MediaRill-attach: " + ex.getMessage()); }
    }


    public void detach() {
        try {
            if (codec != null) {
                codec.stop();
                codec.release();
                codec = null;
            }
        } catch (Exception ex) {
            Log.d("MediaRill", "detach-codec: " + ex.getMessage());
        }

        try {
            if (tracker != null) {
                tracker.detach();
                tracker = null;
            }
        } catch (Exception ex) {
            Log.d("MediaRill", "detach-codec: " + ex.getMessage());
        }
    }



    public long getTotalBytes() {
        long result = tracker.videoBytes + tracker.audioBytes;
        return result;
    }


    public void setVideoFormat() {
        videoFormat = codec.getOutputFormat();
        outMime = videoFormat.getString(MediaFormat.KEY_MIME);
        outWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
        outHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
        tracker.addVideoTrack(videoFormat);
    }




//    public void writeVideo() {
//        videoBytes += bufferInfo.size;
//        tracker.writeVideo(bufferData, bufferInfo);
//    }




    public void releaseBuffer(int aBufferIndex) {
        codec.releaseOutputBuffer(aBufferIndex, false);
        bufferInfo.size = 0;
    }



    public void cropAspect(int aCamWi, int aCamHe) {
        if (0==aCamWi) return;
        if (0==aCamHe) return;
        if (0==outWidth) return;
        if (0==outHeight) return;

        if ( (lastCamWi == aCamWi)
            && (lastCamHe == aCamHe)
            && (lastSurWi == outWidth)
            && (lastSurHe == outHeight) )
            return;

        lastCamWi = aCamWi;
        lastCamHe = aCamHe;
        lastSurWi = outWidth;
        lastSurHe = outHeight;

        float wico = 1f;
        float heco = 1f;

        float wiRatio = (float) lastSurWi / lastCamWi;
        float heRatio = (float) lastSurHe / lastCamHe;

        if (heRatio < wiRatio) {
            // stretch by surHeight
            heco = wiRatio / heRatio;
        } else {
            // stretch by surfWidth
            wico = heRatio / wiRatio;
        }

        Matrix.setIdentityM(matrix, 0);
        Matrix.scaleM(matrix, 0, wico, heco, 1f);
    }






}


