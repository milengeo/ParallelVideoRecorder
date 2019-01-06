
package com.rustero.units;


import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.rustero.App;
import com.rustero.mains.ProfileC;
import com.rustero.tools.Caminf;
import com.rustero.tools.SizeList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("deprecation")




public class MultiEncor implements SurfaceHolder.Callback {



    public interface Events {
        void onLogLine(final String aLine);
        void onAttached();
        void onDetached();
        void onStarted();
        void onStopped();
        void onFlowInfo(int aIndex, final long aSize, final int aRate);
        void onTimeInfo(final long aSofar);
        void onError(final String aMessage);
    }


    private static boolean mUseAudio = true;
    private static final String TAG = "MultiEncor";

    public int askWidth, askHeight;
    public int camWidth, camHeight;
    private int mMaxiZoom, mThisZoom, mZoomStep;
    private List<Integer> mZooms = null;
    private int lastCamWi, lastCamHe, lastSurWi, lastSurHe;
    private long mKickMils, mDuration;

    private Events mEventer;
    private boolean mAskRecording, mNowRecording;

    public SurfaceView surfView;
    public Surface surface;
    public SurfaceHolder surfHolder;
    public int surfWidth, surfHeight;

    private List<RillC> mRills = new ArrayList<RillC>();
    private VideoMotor mVideoMotor;
    private AudioMotor mAudioMotor;
    volatile private boolean mAudioReady;



    public MultiEncor(Events aEventer, SurfaceView aView) {
        App.logLine("MultiEncor");
        mEventer = aEventer;
        surfView = aView;
        SurfaceHolder holder = aView.getHolder();
        holder.addCallback(this);
        loadRills();
    }






    // * SurfaceHolder.Callback


    @Override
    public void surfaceCreated(SurfaceHolder aHolder) {
        Log.d(TAG, "surfaceCreated");
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        App.logLine("surfaceChanged fmt=" + format + " size=" + width + "x" + height);
        surfHolder = holder;
        surfWidth = width;
        surfHeight = height;
        surface = holder.getSurface();

        if (isRunning())
            detachVitor();
        attachVitor();
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        App.logLine("Surface destroyed");
        surface = null;
        detachVitor();
    }





    public ArrayList<FlowC> makeFlowList() {
            ArrayList<FlowC> result = new ArrayList<FlowC>();

            for (int i = 0; i < mRills.size(); i++) {
                RillC rill = mRills.get(i);
                FlowC flow = new FlowC();
                flow.name = rill.outName;
                flow.size = 1;
                result.add(flow);
            }
            return result;
    }



    private void loadRills() {
        if (mAskRecording) {
            throw new RuntimeException("Cannot add mRills while recording!");
        }
            List<ProfileC> profList = App.getProfList();
            for (int i = 0; i < profList.size(); i++) {
                ProfileC prof = profList.get(i);
                if (!prof.chosen) continue;

                RillC rill = new RillC();
                if (App.DEV)
                    rill.outName = prof.name + "_0000.mp4";
                else
                    rill.outName = prof.name + "_" + App.takeNameCount() + ".mp4";
                rill.outFile = new File(App.getOutputFolder(), rill.outName);
                rill.outFile.delete();
                rill.outWidth = prof.width;
                rill.outHeight = prof.height;
                rill.bitrate = prof.bitrate * 1000;
                mRills.add(rill);
            }
    }



    public boolean isRunning() {
        return (mVideoMotor != null);
    }



    public boolean isRecording() {
        return (mAskRecording);
    }



    public void startRecording() {
        App.logLine("startRecording");
        mAskRecording = true;
        mKickMils = System.currentTimeMillis();
    }



    public void stopRecording() {
        App.logLine("stopRecording");
        if (!mAskRecording) return;
        mAskRecording = false;
    }



    private void attachVitor() {
        App.logLine("begin");
        try {
            mVideoMotor = new VideoMotor();
            mVideoMotor.start();
        }
        catch (Exception ex) {};
    }



    private void detachVitor() {

        App.logLine("cease");
        if (mVideoMotor != null) {
            mVideoMotor.finish();
            mVideoMotor = null;
        }
    }



    public float getZoom() {
        if (null == mZooms) return 1.0f;
        float result = 1.0f;
        if (mThisZoom < mZooms.size())
            result = mZooms.get(mThisZoom) / 100.0f;
        return result;
    }



    public void applyZoom() {
        try {
            mVideoMotor.mParams.setZoom(mThisZoom);
            mVideoMotor.mCamera.setParameters(mVideoMotor.mParams);
            Log.d(TAG, "applyZoom: " + mThisZoom);
        } catch (Exception ex) {
            App.logLine(" *** applyZoom error");
        }
    }



    public void incZoom() {
        mThisZoom += mZoomStep;
        if (mThisZoom > mMaxiZoom)
            mThisZoom = mMaxiZoom;
        applyZoom();
    }



    public void decZoom() {
        mThisZoom -= mZoomStep;
        if (mThisZoom < 0)
            mThisZoom = 0;
        applyZoom();
    }





    private class VideoMotor extends Thread {
        private volatile boolean mQuit = false;
        private volatile boolean mDone = false;
        private volatile boolean mImageReady = false;

        private long mLastTack;
        private egl.Core mEglCore;
        private SurfaceTexture mCameraTexture;  // receives the output from the camera preview
        private egl.FullFrameRect mFullFrameBlit;
        private final float[] mCameraMatrix = new float[16];
        private final float[] mDisplayMatrix = new float[16];
        private int mTextureId;

        private egl.WindowSurface mDisplaySurface;
        private Camera mCamera;
        private Camera.Parameters mParams;
        private long mSofar = 0;


        // Creates a texture object suitable for use with this program. On exit, the texture will be bound.
        public int createTextureObject() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            egl.checkGlError("glGenTextures");

            int texId = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
            egl.checkGlError("glBindTexture " + texId);

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            egl.checkGlError("glTexParameter");

            return texId;
        }



        public VideoMotor() {
            Matrix.setIdentityM(mDisplayMatrix, 0);
        }



        /**
         * Opens a camera, and attempts to establish preview mode at the specified surfWidth and outHeight.
         * Sets mCameraPreviewFps to the expected frame rate (which might actually be variable).
         */
        private void attachCamera(int aWidth, int aHeight) {
            if (mCamera != null) {
                App.logLine(" ***** camera already initialized");
                throw new RuntimeException("camera already initialized");
            }

            if (App.gNowFront)
                mCamera = Caminf.openFrontCamera();
            else
                mCamera = Caminf.openBackCamera();
            if (mCamera == null) {
                App.logLine(" ***** Unable to open camera");
                throw new RuntimeException("Unable to open camera");
            }
            SizeList sili = Caminf.getCameraSizes(mCamera);

            mParams = mCamera.getParameters();
            App.logLine("Camera.Parameters_1: " + mParams);

            mMaxiZoom = 1;
            if (mParams.isZoomSupported()) {
                mMaxiZoom = mParams.getMaxZoom();
                mZooms = mParams.getZoomRatios();
            }
            mZoomStep = 1;
            App.logLine("MaxiZoom: " + mMaxiZoom);

            mParams.setRecordingHint(true);  // Give the camera a hint that we're recording video.  This can have a big impact on frame rate.

            Caminf.selectPreviewSize(mParams, aWidth, aHeight);
            boolean previewFailed = false;
            try {
                mCamera.setParameters(mParams);
            } catch (Exception ex) {
                App.logLine(" * Error configuring desired preview size");
                previewFailed = true;
            }

            if (previewFailed) {
                previewFailed = false;
                Caminf.selectPreferredSize(mParams);
                try {
                    mCamera.setParameters(mParams);
                } catch (Exception ex) {
                    App.logLine(" *** Error configuring preferred size");
                    previewFailed = true;
                }
            }

            if (previewFailed) {
                previewFailed = false;
                Caminf.selectPreviewSize(mParams, 640, 480);
                try {
                    mCamera.setParameters(mParams);
                } catch (Exception ex) {
                    App.logLine(" ***** Error configuring preview VGA size");
                    previewFailed = true;
                }
            }

            if (previewFailed) {
                App.logLine(" ***** Error configuring camera");
                throw new RuntimeException("Error configuring camera!");
            }

            Camera.Size cameraPreviewSize = mParams.getPreviewSize();

            //List<int[]> fpsRange mParams.getPreviewFpsRange();
            int fps = mParams.getPreviewFrameRate();
            String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height + " @" + (fps / 1.0f) + "fps";
            //String previewFacts = mParams.flatten();
            App.logLine(" * Camera config: " + previewFacts);

            camWidth = cameraPreviewSize.width;
            camHeight = cameraPreviewSize.height;

            try {
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException ioe) {
                App.logLine(" ***** setPreviewTexture Error!");
                throw new RuntimeException(ioe);
            }

            mCamera.startPreview();
        }



        private void detachCamera() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                App.logLine("releaseCamera -- done");
            }
        }




        private void attachEgl() {
            mEglCore = new egl.Core(null, egl.Core.FLAG_RECORDABLE);
            mDisplaySurface = new egl.WindowSurface(mEglCore, surface, false);
            mDisplaySurface.makeCurrent();

            mFullFrameBlit = new egl.FullFrameRect(new egl.Texture2dProgram(egl.Texture2dProgram.ProgramType.TEXTURE_EXT));

            mTextureId = createTextureObject();
            mCameraTexture = new SurfaceTexture(mTextureId);
            mCameraTexture.setOnFrameAvailableListener(new SurfaceTextureListener());
        }




        private void detachEgl() {
            if (mCameraTexture != null) {
                mCameraTexture.release();
                mCameraTexture = null;
            }
            if (mDisplaySurface != null) {
                mDisplaySurface.release();
                mDisplaySurface = null;
            }
            if (mFullFrameBlit != null) {
                mFullFrameBlit.release(false);
                mFullFrameBlit = null;
            }
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }
        }




        private void attachCore() {
            attachEgl();
            attachCamera(askWidth, askHeight);
            try {
                mEventer.onAttached();
            } catch (Exception ex) {
                App.logLine(" ***** attachCore" + ex.getMessage());
            }

        }



        private void detachCore() {
            detachCamera();
            detachEgl();
            try {
                mEventer.onDetached();
            } catch (Exception ex) {
                App.logLine(" ***** detachCore" + ex.getMessage());
            }
        }


        private void attachRills() {
            mAudioReady = false;
            for (RillC rill : mRills) {
                rill.attach(mEglCore);
            }

            if (mUseAudio) {
                mAudioMotor = new AudioMotor();
                for (RillC rill : mRills)
                    mAudioMotor.mTrackers.add(rill.tracker);
                mAudioMotor.start();
            } else {
                for (RillC rill : mRills)
                    rill.tracker.addAudioTrack(null);
                mAudioReady = true;
            }
        }


        private void detachRills2() {
            if (mAudioMotor != null) {
                mAudioMotor.finish();
                mAudioMotor = null;
            }

            for (RillC rill : mRills) {
                rill.detach();
            }
        }



        private void pushVideo() {
            if (!mAudioReady) return;
                for (RillC rill : mRills)
                    pushEncoder(rill);
        }



        private void pullVideo() {
                for (RillC rill : mRills)
                    pullEncoder(rill);
        }



        private void takeFlows() {
                for (int i = 0; i < mRills.size(); i++) {
                    RillC rill = mRills.get(i);
                    long bytes = rill.getTotalBytes();
                    int rate = (int) (bytes / mSofar);
                    mEventer.onFlowInfo(i, bytes, rate);
                }
        }





        @Override
        public void run() {
            App.logLine("core starting");

            try {
                attachCore();
                clearSurface();

                while (!mQuit) {
                    Thread.sleep(1);

                    if (!mNowRecording) {
                        if (mAskRecording) {
                            // turn on
                            mNowRecording = true;
                            attachRills();
                        }
                    }

                    if (mNowRecording) {
                        if (!mAskRecording) {
                            // turn off
                            mNowRecording = false;
                            detachRills2();
                            mNowRecording = false;
                        }
                    }

                    if (mImageReady) {
                        mImageReady = false;
                        drawFrame();
                        if (mNowRecording) {
                            pushVideo();
                        }
                    }

                    if (mNowRecording) {
                        pullVideo();
                    }

                    if (System.currentTimeMillis() - mLastTack > 999) {
                        mLastTack = System.currentTimeMillis();
                        if (mNowRecording) {
                            mSofar = (System.currentTimeMillis() - mKickMils) / 1000 + 1;
                            takeFlows();
                        }
                        mEventer.onTimeInfo(mSofar);
                    }

                }

                detachCore();
                mDone = true;

            } catch (Exception ex) {
                App.logLine(" ***** MultiEncor_run ex: " + ex.getMessage());
            }

            App.logLine("core quiting");
        }


        private void finish() {
            App.logLine("VideoMotor finish 11");
            mQuit = true;

            try {
                join();
            } catch (InterruptedException ie) {
            }

            App.logLine("VideoMotor finish 99");
        }






        private void pushEncoder(RillC aRill) {
            try {
                aRill.cropAspect(camWidth, camHeight);
                aRill.mEncoderSurface.makeCurrent();
                GLES20.glViewport(0, 0, aRill.outWidth, aRill.outHeight);
                mFullFrameBlit.drawFrame(mTextureId, mCameraMatrix, aRill.matrix);
                aRill.mEncoderSurface.setPresentationTime(mCameraTexture.getTimestamp());
                aRill.mEncoderSurface.swapBuffers();
            } catch (Exception ex) {
                App.logLine(" ***** MultiEncor_pushEncoder ex: " + ex.getMessage());
            }

        }



        // Drains all pending output from the encoder
        private void pullEncoder(RillC aRill) {
            ByteBuffer[] outputBuffers = aRill.codec.getOutputBuffers();
            try {
                int bufIdx = aRill.codec.dequeueOutputBuffer(aRill.bufferInfo, 1000);
                if (bufIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    return;  // no output available yet

                } else if (bufIdx == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = aRill.codec.getOutputBuffers();  // not expected for an encoder

                } else if (bufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    aRill.setVideoFormat();
                    Log.d(TAG, "video output format changed: " + aRill.videoFormat);

                } else if (bufIdx < 0) {
                    Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + bufIdx);

                } else {

                    aRill.bufferData = outputBuffers[bufIdx];
                    if (aRill.bufferData != null) {
                        aRill.bufferData.position(aRill.bufferInfo.offset);
                        aRill.bufferData.limit(aRill.bufferInfo.offset + aRill.bufferInfo.size);


                        //if ((aRill.bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) Log.d(TAG, "MediaCodec.BUFFER_FLAG_KEY_FRAME");
                        if ((aRill.bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // format data
                            Log.d(TAG, "MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        } else {
                            // sample data
                            aRill.tracker.writeVideo(aRill.bufferData, aRill.bufferInfo);
                            //Log.d(TAG, "aRill.writeVideo: " + aRill.bufferInfo.size);
                        }

                        aRill.releaseBuffer(bufIdx);

                        if ((aRill.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "reached end of stream unexpectedly");
                        }
                    }
                }
            }
            catch (Exception ex) {
                App.logLine(" ***** MultiEncor_pullEncoder: " + ex.getMessage());
            };
        }



        private void fillWindowAspect() {
            if (0== camWidth) return;
            if (0== camHeight) return;
            if (0==surfWidth) return;
            if (0==surfHeight) return;

            if ( (lastCamWi == camWidth)
                && (lastCamHe == camHeight)
                && (lastSurWi == surfWidth)
                && (lastSurHe == surfHeight) )
                    return;

            lastCamWi = camWidth;
            lastCamHe = camHeight;
            lastSurWi = surfWidth;
            lastSurHe = surfHeight;

            float wico = 1f;
            float heco = 1f;

            float wiRatio = (float) lastSurWi / lastCamWi;
            float heRatio = (float) lastSurHe / lastCamHe;

            if (heRatio < wiRatio) {
                // stretch by outHeight
                heco = wiRatio / heRatio;
            } else {
                // stretch by surfWidth
                wico = heRatio / wiRatio;
            }

            Matrix.setIdentityM(mDisplayMatrix, 0);
            Matrix.scaleM(mDisplayMatrix, 0, wico, heco, 1f);
        }



        private void cropWindowAspect() {
            if (0==camWidth) return;
            if (0==camHeight) return;
            if (0==surfWidth) return;
            if (0==surfHeight) return;

            if ( (lastCamWi == camWidth)
                && (lastCamHe == camHeight)
                && (lastSurWi == surfWidth)
                && (lastSurHe == surfHeight) )
                return;

            lastCamWi = camWidth;
            lastCamHe = camHeight;
            lastSurWi = surfWidth;
            lastSurHe = surfHeight;

            int drawWidth, drawHeight;
            float aspectRatio = (float) lastCamHe / lastCamWi;

            if (lastSurHe > (int) (lastSurWi * aspectRatio)) {
                // limited by narrow surfWidth; restrict outHeight
                drawWidth = lastSurWi;
                drawHeight = (int) (lastSurWi * aspectRatio);
            } else {
                // limited by short outHeight; restrict surfWidth
                drawWidth = (int) (lastSurHe / aspectRatio);
                drawHeight = lastSurHe;
            }

            float xs = (float) drawWidth / lastSurWi;
            float ys = (float) drawHeight / lastSurHe;

            Matrix.setIdentityM(mDisplayMatrix, 0);
            Matrix.scaleM(mDisplayMatrix, 0, xs, ys, 1f);
        }



        private void drawFrame() {
            if (null == surface) return;
            //cropWindowAspect();
            fillWindowAspect();

            try {
                // latch the next frame from the camera
                mCameraTexture.updateTexImage();
                mCameraTexture.getTransformMatrix(mCameraMatrix);

                // send it to the display
                mDisplaySurface.makeCurrent();
                GLES20.glViewport(0, 0, surfWidth, surfHeight);
                GLES20.glClearColor(0, 0, 0, 0);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                mFullFrameBlit.drawFrame(mTextureId, mCameraMatrix, mDisplayMatrix);
                ///drawExtra(mOuterFrames, mScene.drawWidth, mScene.drawHeight);
                mDisplaySurface.swapBuffers();
            } catch (Exception ex) {
                App.logLine(" ***** MultiEncor_drawFrame ex: " + ex.getMessage());
            }
        }



        private void clearSurface() {
            if (null == surface) return;
            try {
                // send it to the display
                mDisplaySurface.makeCurrent();
                GLES20.glViewport(0, 0, surfWidth, surfHeight);
                GLES20.glClearColor(0, 0, 0, 1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                mDisplaySurface.swapBuffers();
            } catch (Exception ex) {
                Log.d(TAG, " *** MultiEncor_drawFrame ex: " + ex.getMessage());
            }
        }



        private void drawExtra(int frameNum, int width, int height) {
            // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
            int val = frameNum % 3;
            switch (val) {
                case 0:  GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);   break;
                case 1:  GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);   break;
                case 2:  GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);   break;
            }
            int xpos = (int) (width * ((frameNum % 100) / 100.0f));
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            GLES20.glScissor(xpos, 0, width / 32, height / 32);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        }



        private class SurfaceTextureListener implements SurfaceTexture.OnFrameAvailableListener {
            @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mImageReady = true;
            }
        }


    }










    private class AudioMotor extends Thread {

        private volatile boolean mQuit2 = false;
        private volatile boolean mDone = false;

        public static final String TAG = "AudioRecorder";
        public static final boolean VERBOSE = false;


        // audio format settings
        public static final String MIME_TYPE_AUDIO = "audio/mp4a-latm";
        public static final int SAMPLE_RATE = 44100;
        public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
        public static final int BYTES_PER_FRAME = 1024; // AAC
        public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
        public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;

        private MediaCodec mCodec;
        private MediaFormat mMediaFormat, mCodecFormat;

        private AudioRecord mAudioRecorder;
        private MediaCodec.BufferInfo mChunkInfo;
        private ByteBuffer mChunkData;
        //private List<TrackWriter> mTrackWriters = new ArrayList<TrackWriter>();
        private List<MultiTrack> mTrackers = new ArrayList<MultiTrack>();




        AudioMotor() {
            mChunkInfo = new MediaCodec.BufferInfo();
        }



        private void attach() {
            // prepare encoder
            try {
                mMediaFormat = new MediaFormat();
                mMediaFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE_AUDIO);
                mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                mMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
                mMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
                mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1024);
                mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

                mCodec = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
                mCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mCodec.start();
            } catch (Exception ex) {
                Log.e(TAG, "ex: " + ex.getMessage());
            }

            // prepare recorder
            try {
                int iMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                int bufferSize = BYTES_PER_FRAME * 10;

                // Ensure buffer is adequately sized for the AudioRecord object to initialize
                if (bufferSize < iMinBufferSize)
                    bufferSize = ((iMinBufferSize / BYTES_PER_FRAME) + 1) * BYTES_PER_FRAME * 2;

                mAudioRecorder = new AudioRecord(
                    AUDIO_SOURCE,   // source
                    SAMPLE_RATE,    // sample rate, hz
                    CHANNEL_CONFIG, // channels
                    AUDIO_FORMAT,   // audio format
                    bufferSize);   // buffer size (bytes)

                mAudioRecorder.startRecording();
            } catch (Exception ex) {
                Log.e(TAG, "ex: " + ex.getMessage());
            }
        }



        private void detach() {
            if (mCodec != null) {
                mCodec.stop();
                mCodec.release();
                mCodec = null;
            }

            if (mAudioRecorder != null) {
                mAudioRecorder.stop();
                mAudioRecorder.release();
                mAudioRecorder = null;
            }
        }





        @Override
        public void run() {
            Log.d(TAG, "AudioMotor_run_11");
            try {
                attach();
                while (true) {
                    if (mQuit2) {
                        Log.d(TAG, "autor_need_quit");
                        break;
                    }
                    Thread.sleep(1);
                    pushFrame();
                    pullChunk();
                }
                detach();
            } catch (Exception ex) {
                Log.d(TAG, " *** AudioMotor_run ex: " + ex.getMessage());
            }
            mDone = true;
            Log.d(TAG, "AudioMotor_run_99");
        }



        private void finish() {
            Log.d(TAG, "autor_finish_11");
            mQuit2 = true;
            while (true) {
                if (mDone) {
                    Log.d(TAG, "autor_quit_done");
                    break;
                }
                try {
                    Thread.sleep(22);
                } catch (InterruptedException ie) {
                }
            }
            Log.d(TAG, "autor_finish_99");
        }






        public void pushFrame() {
            try {
                ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                int inputBufferIndex = mCodec.dequeueInputBuffer(1000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    int inputLength = mAudioRecorder.read(inputBuffer, BYTES_PER_FRAME);
                    long audioStamp = System.nanoTime() / 1000;
                    if (inputLength != BYTES_PER_FRAME) {
                        Log.d(TAG, String.format("weird audio frame: %d", inputLength));
                    }
                    mCodec.queueInputBuffer(inputBufferIndex, 0, inputLength, audioStamp, 0);
                }
            } catch (Exception ex) {
                Log.e(TAG, " ***** pushFrame: " + ex.getMessage());
            }
        }



        private void pullChunk() {
            if (null == mCodec) return;
            //Log.d(TAG, "AudioMotor_pullChunk");

                ByteBuffer[] bufferArray = mCodec.getOutputBuffers();
                while (true) {
                    try {
                        int bufferIndex = mCodec.dequeueOutputBuffer(mChunkInfo, 0);
                        //Log.d(TAG, " *aenc 11: " + bufferIndex);

                        if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            // not expected for an encoder
                            Log.d(TAG, " *ae INFO_OUTPUT_BUFFERS_CHANGED");
                            bufferArray = mCodec.getOutputBuffers();
                            break;
                        } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                            mCodecFormat = mCodec.getOutputFormat();
                            Log.d(TAG, "audio output format changed: " + mCodecFormat);
                            writeAudioFormat();

                        } else if (bufferIndex < 0) {
                            Log.w(TAG + "_encoder", " *** unexpected result from encoder.dequeueOutputBuffer: " + bufferIndex);
                        } else {

                            mChunkData = bufferArray[bufferIndex];
                            if (mChunkData != null) {
                                if ((mChunkInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    // format data
                                    Log.d(TAG, "AudioCodec.BUFFER_FLAG_CODEC_CONFIG");
                                    mChunkData = null;
                                } else {
                                    //Log.d(TAG, "audio " + mChunkInfo.size + " bytes to muxer, ts=" + mChunkInfo.presentationTimeUs);
                                    mChunkData.position(mChunkInfo.offset);
                                    mChunkData.limit(mChunkInfo.offset + mChunkInfo.size);

                                    writeAudioChunk();

                                    mCodec.releaseOutputBuffer(bufferIndex, false);

                                    if ((mChunkInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        //Log.d(TAG, "audio encoder finished");
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, " ***** pullChunk: " + ex.getMessage());
                    }
                }
        }



        private void writeAudioFormat() {
            for (MultiTrack tracker : mTrackers)
                tracker.addAudioTrack(mCodecFormat);
            mAudioReady = true;
        }


        private void writeAudioChunk() {
            for (MultiTrack tracker : mTrackers)
                tracker.writeAudio(mChunkData, mChunkInfo);
        }

    }




    public static class FlowC {
        public String name;
        public long size;
        public int rate;
    }



}

