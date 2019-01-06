
package com.rustero.units;



import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;



public class egl {

    private static final String TAG = "_egl";
    private static final int SIZEOF_FLOAT = 4;

    /** Identity matrix for general use.  Don't modify or life will get weird. */
    public static final float[] IDENTITY_MATRIX;
    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }





    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }



    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }



    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }



    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }



    /**
     * Creates a texture from raw data.
     *
     * @param data Image data, in a "direct" ByteBuffer.
     * @param width Texture surfWidth, in pixels (not bytes).
     * @param height Texture outHeight, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        GLES20.glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        checkGlError("glGenTextures");

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR);
        checkGlError("loadImageTexture");

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, /*level*/ 0, format,
            width, height, /*border*/ 0, format, GLES20.GL_UNSIGNED_BYTE, data);
        checkGlError("loadImageTexture");

        return textureHandle;
    }



    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }



    /**
     * Writes GL version info to the log.
     */
    public static void logVersionInfo() {
        Log.i(TAG, "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i(TAG, "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i(TAG, "version : " + GLES20.glGetString(GLES20.GL_VERSION));

        if (false) {
            int[] values = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
            int majorVersion = values[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
            int minorVersion = values[0];
            if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
                Log.i(TAG, "iversion: " + majorVersion + "." + minorVersion);
            }
        }
    }











/**
 * Core EGL state (display, context, config).
 * <p>
 * The EGLContext must only be attached to one thread at a time.  This class is not thread-safe.
 */

public static final class Core {


    /**
     * Constructor flag: surface must be recordable.  This discourages EGL from using a
     * pixel format that cannot be converted efficiently to something usable by the video
     * encoder.
     */
    public static final int FLAG_RECORDABLE = 0x01;

    /**
     * Constructor flag: ask for GLES3, fall back to GLES2 if not available.  Without this
     * flag, GLES2 is used.
     */
    public static final int FLAG_TRY_GLES3 = 0x02;

    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    private int mGlVersion = -1;


    /**
     * Prepares EGL display and context.
     * <p>
     * Equivalent to EglCore(null, 0).
     */
    public Core() {
        this(null, 0);
    }



    /**
     * Prepares EGL display and context.
     * <p>
     * @param sharedContext The context to share, or null if sharing is not desired.
     * @param flags Configuration bit flags, e.g. FLAG_RECORDABLE.
     */

    public Core(EGLContext sharedContext, int flags) {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        if (sharedContext == null) {
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Try to get a GLES3 context, if requested.
        if ((flags & FLAG_TRY_GLES3) != 0) {
            //Log.d(TAG, "Trying GLES 3");
            EGLConfig config = getConfig(flags, 3);
            if (config != null) {
                int[] attrib3_list = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL14.EGL_NONE
                };
                EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                        attrib3_list, 0);

                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    //Log.d(TAG, "Got GLES 3 config");
                    mEGLConfig = config;
                    mEGLContext = context;
                    mGlVersion = 3;
                }
            }
        }
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {  // GLES 2 only, or GLES 3 attempt failed
            //Log.d(TAG, "Trying GLES 2");
            EGLConfig config = getConfig(flags, 2);
            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            int[] attrib2_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                    attrib2_list, 0);
            checkEglError("eglCreateContext");
            mEGLConfig = config;
            mEGLContext = context;
            mGlVersion = 2;
        }

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }



    /**
     * Finds a suitable EGLConfig.
     *
     * @param flags Bit flags from constructor.
     * @param version Must be 2 or 3.
     */
    private EGLConfig getConfig(int flags, int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }



    /**
     * Discards all resources held by this class, notably the EGL context.  This must be
     * called from the thread where the context was created.
     * <p>
     * On completion, no context will be current.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                // We're limited here -- finalizers don't run on the thread that holds
                // the EGL state, so if a surface or context is still current on another
                // thread we can't fully release it here.  Exceptions thrown from here
                // are quietly discarded.  Complain in the log file.
                Log.w(TAG, "WARNING: EglCore was not explicitly released -- state may be leaked");
                release();
            }
        } finally {
            super.finalize();
        }
    }



    /**
     * Destroys the specified surface.  Note the EGLSurface won't actually be destroyed if it's
     * still current in a context.
     */
    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }



    /**
     * Creates an EGL surface associated with a Surface.
     * If this is destined for MediaCodec, the EGLConfig should have the "recordable" attribute.
     */
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + surface);
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {EGL14.EGL_NONE};
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }


    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    public EGLSurface createOffscreenSurface(int width, int height) {
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig,
                surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }


    /**
     * Makes our EGL context current, using the supplied surface for both "draw" and "read".
     */
    public void makeCurrent(EGLSurface eglSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }


    /**
     * Makes our EGL context current, using the supplied "draw" and "read" surfaces.
     */
    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            Log.d(TAG, "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent(draw,read) failed");
        }
    }


    /**
     * Makes no context current.
     */
    public void makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(EGLSurface eglSurface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nsecs);
    }

    /**
     * Returns true if our context and the specified surface are current.
     */
    public boolean isCurrent(EGLSurface eglSurface) {
        return mEGLContext.equals(EGL14.eglGetCurrentContext()) &&
            eglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW));
    }

    /**
     * Performs a simple surface query.
     */
    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEGLDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    /**
     * Queries a string value.
     */
    public String queryString(int what) {
        return EGL14.eglQueryString(mEGLDisplay, what);
    }

    /**
     * Returns the GLES version this context is configured for (currently 2 or 3).
     */
    public int getGlVersion() {
        return mGlVersion;
    }

    /**
     * Writes the current display, context, and surface to the log.
     */
    public static void logCurrent(String msg) {
        EGLDisplay display;
        EGLContext context;
        EGLSurface surface;

        display = EGL14.eglGetCurrentDisplay();
        context = EGL14.eglGetCurrentContext();
        surface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        Log.i(TAG, "Current EGL (" + msg + "): display=" + display + ", context=" + context +
            ", surface=" + surface);
    }

    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}






    /**
     * Common base class for EGL surfaces.
     * There can be multiple surfaces associated with a single context.
     */
    public static class EglSurfaceBase {


        // EglCore object we're associated with.  It may be associated with multiple surfaces.
        protected Core mEglCore;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        private int mWidth = -1;
        private int mHeight = -1;


        protected EglSurfaceBase(Core eglCore) {
            mEglCore = eglCore;
        }


        /**
         * Creates a window surface.
         * @param surface May be a Surface or SurfaceTexture.
         */
        public void createWindowSurface(Object surface) {
            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                throw new IllegalStateException("surface already created");
            }
            mEGLSurface = mEglCore.createWindowSurface(surface);

            // Don't cache surfWidth/outHeight here, because the size of the underlying surface can change
            // out from under us (see e.g. HardwareScalerActivity).
            //mWidth = mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
            //mHeight = mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
        }


        /**
         * Creates an off-screen surface.
         */
        public void createOffscreenSurface(int width, int height) {
            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                throw new IllegalStateException("surface already created");
            }
            mEGLSurface = mEglCore.createOffscreenSurface(width, height);
            mWidth = width;
            mHeight = height;
        }


        /**
         * Returns the surface's surfWidth, in pixels.
         * If this is called on a window surface, and the underlying surface is in the process
         * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
         * callback).  The size should match after the next buffer swap.
         */
        public int getWidth() {
            if (mWidth < 0) {
                return mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
            } else {
                return mWidth;
            }
        }


        /**
         * Returns the surface's outHeight, in pixels.
         */
        public int getHeight() {
            if (mHeight < 0) {
                return mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
            } else {
                return mHeight;
            }
        }

        /**
         * Release the EGL surface.
         */
        public void releaseEglSurface() {
            mEglCore.releaseSurface(mEGLSurface);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
            mWidth = mHeight = -1;
        }

        /**
         * Makes our EGL context and surface current.
         */
        public void makeCurrent() {
            mEglCore.makeCurrent(mEGLSurface);
        }

        /**
         * Makes our EGL context and surface current for drawing, using the supplied surface
         * for reading.
         */
        public void makeCurrentReadFrom(EglSurfaceBase readSurface) {
            mEglCore.makeCurrent(mEGLSurface, readSurface.mEGLSurface);
        }

        /**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         *
         * @return false on failure
         */
        public boolean swapBuffers() {
            boolean result = mEglCore.swapBuffers(mEGLSurface);
            if (!result) {
                Log.d(TAG, "WARNING: swapBuffers() failed");
            }
            return result;
        }

        /**
         * Sends the presentation time stamp to EGL.
         *
         * @param nsecs Timestamp, in nanoseconds.
         */
        public void setPresentationTime(long nsecs) {
            mEglCore.setPresentationTime(mEGLSurface, nsecs);
        }


        /**
         * Saves the EGL surface to a file.
         * Expects that this object's EGL surface is current.
         */
        public void saveFrame(File file) throws IOException {
            if (!mEglCore.isCurrent(mEGLSurface)) {
                throw new RuntimeException("Expected EGL context/surface is not current");
            }

            // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
            // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
            // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
            // Bitmap "copy pixels" method wants the same format GL provides.
            //
            // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
            // here often.
            //
            // Making this even more interesting is the upside-down nature of GL, which means
            // our output will look upside down relative to what appears on screen if the
            // typical GL conventions are used.

            String filename = file.toString();

            int width = getWidth();
            int height = getHeight();
            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
            checkGlError("glReadPixels");
            buf.rewind();

            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(filename));
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
            } finally {
                if (bos != null) bos.close();
            }
            Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + filename + "'");
        }
    }





    /**
     * Off-screen EGL surface (pbuffer).
     * <p>
     * It's good practice to explicitly release() the surface, preferably from a "finally" block.
     */

    public class OffscreenSurface extends EglSurfaceBase {
        /**
         * Creates an off-screen surface with the specified surfWidth and outHeight.
         */
        public OffscreenSurface(Core eglCore, int width, int height) {
            super(eglCore);
            createOffscreenSurface(width, height);
        }

        /**
         * Releases any resources associated with the surface.
         */
        public void release() {
            releaseEglSurface();
        }
    }




    /**
     * Recordable EGL window surface.
     * <p>
     * It's good practice to explicitly release() the surface, preferably from a "finally" block.
     */

    public static class WindowSurface extends EglSurfaceBase {
        private Surface mSurface;
        private boolean mReleaseSurface;

        /**
         * Associates an EGL surface with the native window surface.
         * <p>
         * Set releaseSurface to true if you want the Surface to be released when release() is
         * called.  This is convenient, but can interfere with framework classes that expect to
         * manage the Surface themselves (e.g. if you release a SurfaceView's Surface, the
         * surfaceDestroyed() callback won't fire).
         */
        public WindowSurface(Core eglCore, Surface surface, boolean releaseSurface) {
            super(eglCore);
            createWindowSurface(surface);
            mSurface = surface;
            mReleaseSurface = releaseSurface;
        }

        /**
         * Associates an EGL surface with the SurfaceTexture.
         */
        public WindowSurface(Core eglCore, SurfaceTexture surfaceTexture) {
            super(eglCore);
            createWindowSurface(surfaceTexture);
        }

        /**
         * Releases any resources associated with the EGL surface (and, if configured to do so,
         * with the Surface as well).
         * <p>
         * Does not require that the surface's EGL context be current.
         */
        public void release() {
            releaseEglSurface();
            if (mSurface != null) {
                if (mReleaseSurface) {
                    mSurface.release();
                }
                mSurface = null;
            }
        }

        /**
         * Recreate the EGLSurface, using the new EglBase.  The caller should have already
         * freed the old EGLSurface with releaseEglSurface().
         * <p>
         * This is useful when we want to update the EGLSurface associated with a Surface.
         * For example, if we want to share with a different EGLContext, which can only
         * be done by tearing down and recreating the context.  (That's handled by the caller;
         * this just creates a new EGLSurface for the Surface we were handed earlier.)
         * <p>
         * If the previous EGLSurface isn't fully destroyed, e.g. it's still current on a
         * context somewhere, the create call will fail with complaints from the Surface
         * about already being connected.
         */
        public void recreate(Core newEglCore) {
            if (mSurface == null) {
                throw new RuntimeException("not yet implemented for SurfaceTexture");
            }
            mEglCore = newEglCore;          // switch to new context
            createWindowSurface(mSurface);  // create new surface
        }
    }







    /**
     * Base class for stuff we like to draw.
     */

    public static class Drawable2d {
        private static final int SIZEOF_FLOAT = 4;

        /**
         * Simple equilateral triangle (1.0 per side).  Centered on (0,0).
         */
        private static final float TRIANGLE_COORDS[] = {
            0.0f,  0.577350269f,   // 0 top
            -0.5f, -0.288675135f,   // 1 bottom left
            0.5f, -0.288675135f    // 2 bottom right
        };
        private static final float TRIANGLE_TEX_COORDS[] = {
            0.5f, 0.0f,     // 0 top center
            0.0f, 1.0f,     // 1 bottom left
            1.0f, 1.0f,     // 2 bottom right
        };
        private static final FloatBuffer TRIANGLE_BUF =
            createFloatBuffer(TRIANGLE_COORDS);
        private static final FloatBuffer TRIANGLE_TEX_BUF =
            createFloatBuffer(TRIANGLE_TEX_COORDS);

        /**
         * Simple square, specified as a triangle strip.  The square is centered on (0,0) and has
         * a size of 1x1.
         * <p>
         * Triangles are 0-1-2 and 2-1-3 (counter-rotate180 winding).
         */
        private static final float RECTANGLE_COORDS[] = {
            -0.5f, -0.5f,   // 0 bottom left
            0.5f, -0.5f,   // 1 bottom right
            -0.5f,  0.5f,   // 2 top left
            0.5f,  0.5f,   // 3 top right
        };
        private static final float RECTANGLE_TEX_COORDS[] = {
            0.0f, 1.0f,     // 0 bottom left
            1.0f, 1.0f,     // 1 bottom right
            0.0f, 0.0f,     // 2 top left
            1.0f, 0.0f      // 3 top right
        };
        private static final FloatBuffer RECTANGLE_BUF =
            createFloatBuffer(RECTANGLE_COORDS);
        private static final FloatBuffer RECTANGLE_TEX_BUF =
            createFloatBuffer(RECTANGLE_TEX_COORDS);

        /**
         * A "full" square, extending from -1 to +1 in both dimensions.  When the model/surfView/projection
         * matrix is identity, this will exactly cover the viewport.
         * <p>
         * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
         * right with external textures from SurfaceTexture.)
         */
        private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
        };
        private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
        };
        private static final FloatBuffer FULL_RECTANGLE_BUF =
            createFloatBuffer(FULL_RECTANGLE_COORDS);
        private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);


        private FloatBuffer mVertexArray;
        private FloatBuffer mTexCoordArray;
        private int mVertexCount;
        private int mCoordsPerVertex;
        private int mVertexStride;
        private int mTexCoordStride;
        private Prefab mPrefab;

        /**
         * Enum values for constructor.
         */
        public enum Prefab {
            TRIANGLE, RECTANGLE, FULL_RECTANGLE
        }

        /**
         * Prepares a drawable from a "pre-fabricated" shape definition.
         * <p>
         * Does no EGL/GL operations, so this can be done at any time.
         */
        public Drawable2d(Prefab shape) {
            switch (shape) {
                case TRIANGLE:
                    mVertexArray = TRIANGLE_BUF;
                    mTexCoordArray = TRIANGLE_TEX_BUF;
                    mCoordsPerVertex = 2;
                    mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
                    mVertexCount = TRIANGLE_COORDS.length / mCoordsPerVertex;
                    break;
                case RECTANGLE:
                    mVertexArray = RECTANGLE_BUF;
                    mTexCoordArray = RECTANGLE_TEX_BUF;
                    mCoordsPerVertex = 2;
                    mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
                    mVertexCount = RECTANGLE_COORDS.length / mCoordsPerVertex;
                    break;
                case FULL_RECTANGLE:
                    mVertexArray = FULL_RECTANGLE_BUF;
                    mTexCoordArray = FULL_RECTANGLE_TEX_BUF;
                    mCoordsPerVertex = 2;
                    mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
                    mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
                    break;
                default:
                    throw new RuntimeException("Unknown shape " + shape);
            }
            mTexCoordStride = 2 * SIZEOF_FLOAT;
            mPrefab = shape;
        }

        /**
         * Returns the array of vertices.
         * <p>
         * To avoid allocations, this returns internal state.  The caller must not modify it.
         */
        public FloatBuffer getVertexArray() {
            return mVertexArray;
        }

        /**
         * Returns the array of texture coordinates.
         * <p>
         * To avoid allocations, this returns internal state.  The caller must not modify it.
         */
        public FloatBuffer getTexCoordArray() {
            return mTexCoordArray;
        }

        /**
         * Returns the number of vertices stored in the vertex array.
         */
        public int getVertexCount() {
            return mVertexCount;
        }

        /**
         * Returns the surfWidth, in bytes, of the data for each vertex.
         */
        public int getVertexStride() {
            return mVertexStride;
        }

        /**
         * Returns the surfWidth, in bytes, of the data for each texture coordinate.
         */
        public int getTexCoordStride() {
            return mTexCoordStride;
        }

        /**
         * Returns the number of position coordinates per vertex.  This will be 2 or 3.
         */
        public int getCoordsPerVertex() {
            return mCoordsPerVertex;
        }

        @Override
        public String toString() {
            if (mPrefab != null) {
                return "[Drawable2d: " + mPrefab + "]";
            } else {
                return "[Drawable2d: ...]";
            }
        }
    }






    /**
     * GL program and supporting functions for flat-shaded rendering.
     */

    public static class FlatShadedProgram {

        private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                "attribute vec4 aPosition;" +
                "void main() {" +
                "    gl_Position = uMVPMatrix * aPosition;" +
                "}";

        private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                "uniform vec4 uColor;" +
                "void main() {" +
                "    gl_FragColor = uColor;" +
                "}";

        // Handles to the GL program and various components of it.
        private int mProgramHandle = -1;
        private int muColorLoc = -1;
        private int muMVPMatrixLoc = -1;
        private int maPositionLoc = -1;


        /**
         * Prepares the program in the current EGL context.
         */
        public FlatShadedProgram() {
            mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgramHandle == 0) {
                throw new RuntimeException("Unable to create program");
            }
            Log.d(TAG, "Created program " + mProgramHandle);

            // get locations of attributes and uniforms

            maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
            checkLocation(maPositionLoc, "aPosition");
            muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            checkLocation(muMVPMatrixLoc, "uMVPMatrix");
            muColorLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColor");
            checkLocation(muColorLoc, "uColor");
        }

        /**
         * Releases the program.
         */
        public void release() {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = -1;
        }

        /**
         * Issues the draw call.  Does the full setup on every call.
         *
         * @param mvpMatrix The 4x4 projection matrix.
         * @param color A 4-element color vector.
         * @param vertexBuffer Buffer with vertex data.
         * @param firstVertex Index of first vertex to use in vertexBuffer.
         * @param vertexCount Number of vertices in vertexBuffer.
         * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
         * @param vertexStride Width, in bytes, of the data for each vertex (often vertexCount *
         *        sizeof(float)).
         */
        public void draw(float[] mvpMatrix, float[] color, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride) {
            checkGlError("draw start");

            // Select the program.
            GLES20.glUseProgram(mProgramHandle);
            checkGlError("glUseProgram");

            // Copy the model / surfView / projection matrix over.
            GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Copy the color vector in.
            GLES20.glUniform4fv(muColorLoc, 1, color, 0);
            checkGlError("glUniform4fv ");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(maPositionLoc);
            checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer");

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            checkGlError("glDrawArrays");

            // Done -- disable vertex array and program.
            GLES20.glDisableVertexAttribArray(maPositionLoc);
            GLES20.glUseProgram(0);
        }
    }






    /**
     * This class essentially represents a viewport-sized sprite that will be rendered with
     * a texture, usually from an external source like the camera or video decoder.
     */

    public static class FullFrameRect {
        private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
        private Texture2dProgram mProgram;


        /**
         * Prepares the object.
         *
         * @param program The program to use.  FullFrameRect takes ownership, and will release
         *     the program when no longer needed.
         */
        public FullFrameRect(Texture2dProgram program) {
            mProgram = program;
        }


        /**
         * Releases resources.
         * <p>
         * This must be called with the appropriate EGL context current (i.e. the one that was
         * current when the constructor was called).  If we're about to destroy the EGL context,
         * there's no value in having the caller make it current just to do this cleanup, so you
         * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
         */
        public void release(boolean doEglCleanup) {
            if (mProgram != null) {
                if (doEglCleanup) {
                    mProgram.release();
                }
                mProgram = null;
            }
        }



        /**
         * Returns the program currently in use.
         */
        public Texture2dProgram getProgram() {
            return mProgram;
        }



        /**
         * Changes the program.  The previous program will be released.
         * <p>
         * The appropriate EGL context must be current.
         */
        public void changeProgram(Texture2dProgram program) {
            mProgram.release();
            mProgram = program;
        }



        /**
         * Creates a texture object suitable for use with drawFrame().
         */
        public int createTextureObject() {
            return mProgram.createTextureObject();
        }



        /**
         * Draws a viewport-filling rect, texturing it with the specified texture object.
         */
        public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix) {
            // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
            mProgram.draw(mvpMatrix, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride());
        }


    }








    /**
     * Code for generating images useful for testing textures.
     */

    public static class GeneratedTexture {
        //private static final String TAG = GlUtil.TAG;

        public enum Image { COARSE, FINE };

        // Basic colors, in little-endian RGBA.
        private static final int BLACK = 0x00000000;
        private static final int RED = 0x000000ff;
        private static final int GREEN = 0x0000ff00;
        private static final int BLUE = 0x00ff0000;
        private static final int MAGENTA = RED | BLUE;
        private static final int YELLOW = RED | GREEN;
        private static final int CYAN = GREEN | BLUE;
        private static final int WHITE = RED | GREEN | BLUE;
        private static final int OPAQUE = (int) 0xff000000L;
        private static final int HALF = (int) 0x80000000L;
        private static final int LOW = (int) 0x40000000L;
        private static final int TRANSP = 0;

        private static final int GRID[] = new int[] {    // must be 16 elements
            OPAQUE|RED,     OPAQUE|YELLOW,  OPAQUE|GREEN,   OPAQUE|MAGENTA,
            OPAQUE|WHITE,   LOW|RED,        LOW|GREEN,      OPAQUE|YELLOW,
            OPAQUE|MAGENTA, TRANSP|GREEN,   HALF|RED,       OPAQUE|BLACK,
            OPAQUE|CYAN,    OPAQUE|MAGENTA, OPAQUE|CYAN,    OPAQUE|BLUE,
        };

        private static final int TEX_SIZE = 64;         // must be power of 2
        private static final int FORMAT = GLES20.GL_RGBA;
        private static final int BYTES_PER_PIXEL = 4;   // RGBA

        // Generate test image data.  This must come after the other values are initialized.
        private static final ByteBuffer sCoarseImageData = generateCoarseData();
        private static final ByteBuffer sFineImageData = generateFineData();


        /**
         * Creates a test texture in the current GL context.
         * <p>
         * This follows image conventions, so the pixel data at offset zero is intended to appear
         * in the top-left corner.  Color values for non-opaque alpha will be pre-multiplied.
         *
         * @return Handle to texture.
         */
        public static int createTestTexture(Image which) {
            ByteBuffer buf;
            switch (which) {
                case COARSE:
                    buf = sCoarseImageData;
                    break;
                case FINE:
                    buf = sFineImageData;
                    break;
                default:
                    throw new RuntimeException("unknown image");
            }
            return createImageTexture(buf, TEX_SIZE, TEX_SIZE, FORMAT);
        }


        /**
         * Generates a "coarse" test image.  We want to create a 4x4 block pattern with obvious color
         * values in the corners, so that we can confirm orientation and coverage.  We also
         * leave a couple of alpha holes to check that channel.  Single pixels are set in two of
         * the corners to make it easy to see if we're cutting the texture off at the edge.
         * <p>
         * Like most image formats, the pixel data begins with the top-left corner, which is
         * upside-down relative to OpenGL conventions.  The texture coordinates should be flipped
         * vertically.  Using an asymmetric patterns lets us check that we're doing that right.
         * <p>
         * Colors use pre-multiplied alpha (so set glBlendFunc appropriately).
         *
         * @return A direct ByteBuffer with the 8888 RGBA data.
         */
        private static ByteBuffer generateCoarseData() {
            byte[] buf = new byte[TEX_SIZE * TEX_SIZE * BYTES_PER_PIXEL];
            final int scale = TEX_SIZE / 4;        // convert 64x64 --> 4x4

            for (int i = 0; i < buf.length; i += BYTES_PER_PIXEL) {
                int texRow = (i / BYTES_PER_PIXEL) / TEX_SIZE;
                int texCol = (i / BYTES_PER_PIXEL) % TEX_SIZE;

                int gridRow = texRow / scale;  // 0-3
                int gridCol = texCol / scale;  // 0-3
                int gridIndex = (gridRow * 4) + gridCol;  // 0-15

                int color = GRID[gridIndex];

                // override the pixels in two corners to check coverage
                if (i == 0) {
                    color = OPAQUE | WHITE;
                } else if (i == buf.length - BYTES_PER_PIXEL) {
                    color = OPAQUE | WHITE;
                }

                // extract RGBA; use "int" instead of "byte" to get unsigned values
                int red = color & 0xff;
                int green = (color >> 8) & 0xff;
                int blue = (color >> 16) & 0xff;
                int alpha = (color >> 24) & 0xff;

                // pre-multiply colors and store in buffer
                float alphaM = alpha / 255.0f;
                buf[i] = (byte) (red * alphaM);
                buf[i+1] = (byte) (green * alphaM);
                buf[i+2] = (byte) (blue * alphaM);
                buf[i+3] = (byte) alpha;
            }

            ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
            byteBuf.put(buf);
            byteBuf.position(0);
            return byteBuf;
        }


        /**
         * Generates a fine-grained test image.
         *
         * @return A direct ByteBuffer with the 8888 RGBA data.
         */
        private static ByteBuffer generateFineData() {
            byte[] buf = new byte[TEX_SIZE * TEX_SIZE * BYTES_PER_PIXEL];

            // top/left: single-pixel red/blue
            checkerPattern(buf, 0, 0, TEX_SIZE / 2, TEX_SIZE / 2,
                OPAQUE|RED, OPAQUE|BLUE, 0x01);
            // bottom/right: two-pixel red/green
            checkerPattern(buf, TEX_SIZE / 2, TEX_SIZE / 2, TEX_SIZE, TEX_SIZE,
                OPAQUE|RED, OPAQUE|GREEN, 0x02);
            // bottom/left: four-pixel blue/green
            checkerPattern(buf, 0, TEX_SIZE / 2, TEX_SIZE / 2, TEX_SIZE,
                OPAQUE|BLUE, OPAQUE|GREEN, 0x04);
            // top/right: eight-pixel black/white
            checkerPattern(buf, TEX_SIZE / 2, 0, TEX_SIZE, TEX_SIZE / 2,
                OPAQUE|WHITE, OPAQUE|BLACK, 0x08);

            ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
            byteBuf.put(buf);
            byteBuf.position(0);
            return byteBuf;
        }

        private static void checkerPattern(byte[] buf, int left, int top, int right, int bottom,
                                           int color1, int color2, int bit) {
            for (int row = top; row < bottom; row++) {
                int rowOffset = row * TEX_SIZE * BYTES_PER_PIXEL;
                for (int col = left; col < right; col++) {
                    int offset = rowOffset + col * BYTES_PER_PIXEL;
                    int color;
                    if (((row & bit) ^ (col & bit)) == 0) {
                        color = color1;
                    } else {
                        color = color2;
                    }

                    // extract RGBA; use "int" instead of "byte" to get unsigned values
                    int red = color & 0xff;
                    int green = (color >> 8) & 0xff;
                    int blue = (color >> 16) & 0xff;
                    int alpha = (color >> 24) & 0xff;

                    // pre-multiply colors and store in buffer
                    float alphaM = alpha / 255.0f;
                    buf[offset] = (byte) (red * alphaM);
                    buf[offset+1] = (byte) (green * alphaM);
                    buf[offset+2] = (byte) (blue * alphaM);
                    buf[offset+3] = (byte) alpha;
                }
            }
        }
    }






    /**
     * Base class for a 2d object.  Includes position, scale, rotation, and flat-shaded color.
     */

    public static class Sprite2d {

        private Drawable2d mDrawable;
        private float mColor[];
        private int mTextureId;
        private float mAngle;
        private float mScaleX, mScaleY;
        private float mPosX, mPosY;

        private float[] mModelViewMatrix;
        private boolean mMatrixReady;

        private float[] mScratchMatrix = new float[16];

        public Sprite2d(Drawable2d drawable) {
            mDrawable = drawable;
            mColor = new float[4];
            mColor[3] = 1.0f;
            mTextureId = -1;

            mModelViewMatrix = new float[16];
            mMatrixReady = false;
        }

        /**
         * Re-computes mModelViewMatrix, based on the current values for rotation, scale, and
         * translation.
         */
        private void recomputeMatrix() {
            float[] modelView = mModelViewMatrix;

            Matrix.setIdentityM(modelView, 0);
            Matrix.translateM(modelView, 0, mPosX, mPosY, 0.0f);
            if (mAngle != 0.0f) {
                Matrix.rotateM(modelView, 0, mAngle, 0.0f, 0.0f, 1.0f);
            }
            Matrix.scaleM(modelView, 0, mScaleX, mScaleY, 1.0f);
            mMatrixReady = true;
        }

        /**
         * Returns the sprite scale along the X axis.
         */
        public float getScaleX() {
            return mScaleX;
        }

        /**
         * Returns the sprite scale along the Y axis.
         */
        public float getScaleY() {
            return mScaleY;
        }

        /**
         * Sets the sprite scale (size).
         */
        public void setScale(float scaleX, float scaleY) {
            mScaleX = scaleX;
            mScaleY = scaleY;
            mMatrixReady = false;
        }

        /**
         * Gets the sprite rotation angle, in degrees.
         */
        public float getRotation() {
            return mAngle;
        }

        /**
         * Sets the sprite rotation angle, in degrees.  Sprite will rotate counter-rotate180.
         */
        public void setRotation(float angle) {
            // Normalize.  We're not expecting it to be way off, so just iterate.
            while (angle >= 360.0f) {
                angle -= 360.0f;
            }
            while (angle <= -360.0f) {
                angle += 360.0f;
            }
            mAngle = angle;
            mMatrixReady = false;
        }

        /**
         * Returns the position on the X axis.
         */
        public float getPositionX() {
            return mPosX;
        }

        /**
         * Returns the position on the Y axis.
         */
        public float getPositionY() {
            return mPosY;
        }

        /**
         * Sets the sprite position.
         */
        public void setPosition(float posX, float posY) {
            mPosX = posX;
            mPosY = posY;
            mMatrixReady = false;
        }

        /**
         * Returns the model-surfView matrix.
         * <p>
         * To avoid allocations, this returns internal state.  The caller must not modify it.
         */
        public float[] getModelViewMatrix() {
            if (!mMatrixReady) {
                recomputeMatrix();
            }
            return mModelViewMatrix;
        }

        /**
         * Sets color to use for flat-shaded rendering.  Has no effect on textured rendering.
         */
        public void setColor(float red, float green, float blue) {
            mColor[0] = red;
            mColor[1] = green;
            mColor[2] = blue;
        }

        /**
         * Sets texture to use for textured rendering.  Has no effect on flat-shaded rendering.
         */
        public void setTexture(int textureId) {
            mTextureId = textureId;
        }

        /**
         * Returns the color.
         * <p>
         * To avoid allocations, this returns internal state.  The caller must not modify it.
         */
        public float[] getColor() {
            return mColor;
        }

        /**
         * Draws the rectangle with the supplied program and projection matrix.
         */
        public void draw(FlatShadedProgram program, float[] projectionMatrix) {
            // Compute model/surfView/projection matrix.
            Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);

            program.draw(mScratchMatrix, mColor, mDrawable.getVertexArray(), 0,
                mDrawable.getVertexCount(), mDrawable.getCoordsPerVertex(),
                mDrawable.getVertexStride());
        }

        /**
         * Draws the rectangle with the supplied program and projection matrix.
         */
        public void draw(Texture2dProgram program, float[] projectionMatrix) {
            // Compute model/surfView/projection matrix.
            Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);

            program.draw(mScratchMatrix, mDrawable.getVertexArray(), 0,
                mDrawable.getVertexCount(), mDrawable.getCoordsPerVertex(),
                mDrawable.getVertexStride(), IDENTITY_MATRIX, mDrawable.getTexCoordArray(),
                mTextureId, mDrawable.getTexCoordStride());
        }

        @Override
        public String toString() {
            return "[Sprite2d pos=" + mPosX + "," + mPosY +
                " scale=" + mScaleX + "," + mScaleY + " angle=" + mAngle +
                " color={" + mColor[0] + "," + mColor[1] + "," + mColor[2] +
                "} drawable=" + mDrawable + "]";
        }
    }






    /**
     * GL program and supporting functions for textured 2D shapes.
     */

    public static class Texture2dProgram {

        public enum ProgramType {
            TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_FILT
        }

        // Simple vertex shader, used for all programs.
        private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uTexMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                "}\n";

        // Simple fragment shader for use with "normal" 2D textures.
        private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        // Simple fragment shader for use with external 2D textures (e.g. what we get from
        // SurfaceTexture).
        private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        // Fragment shader that converts color to black & white with a simple transformation.
        private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
                "}\n";

        // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
        // the lower-right half will have the filter applied, and a thin red line will be drawn
        // at the border.
        //
        // This is not optimized for performance.  Some things that might make this faster:
        // - Remove the conditionals.  They're used to present a half & half surfView with a red
        //   stripe across the middle, but that's only useful for a demo.
        // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
        // - Bake the filter kernel into the shader, instead of passing it through a uniform
        //   array.  That, combined with loop unrolling, should reduce memory accesses.
        public static final int KERNEL_SIZE = 9;
        private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
                "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
                "precision highp float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "uniform float uKernel[KERNEL_SIZE];\n" +
                "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
                "uniform float uColorAdjust;\n" +
                "void main() {\n" +
                "    int i = 0;\n" +
                "    vec4 sum = vec4(0.0);\n" +
                "    if (vTextureCoord.x < vTextureCoord.y - 0.005) {\n" +
                "        for (i = 0; i < KERNEL_SIZE; i++) {\n" +
                "            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
                "            sum += texc * uKernel[i];\n" +
                "        }\n" +
                "    sum += uColorAdjust;\n" +
                "    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {\n" +
                "        sum = texture2D(sTexture, vTextureCoord);\n" +
                "    } else {\n" +
                "        sum.r = 1.0;\n" +
                "    }\n" +
                "    gl_FragColor = sum;\n" +
                "}\n";

        private ProgramType mProgramType;

        // Handles to the GL program and various components of it.
        private int mProgramHandle;
        private int muMVPMatrixLoc;
        private int muTexMatrixLoc;
        private int muKernelLoc;
        private int muTexOffsetLoc;
        private int muColorAdjustLoc;
        private int maPositionLoc;
        private int maTextureCoordLoc;

        private int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

        private float[] mKernel = new float[KERNEL_SIZE];
        private float[] mTexOffset;
        private float mColorAdjust;


        /**
         * Prepares the program in the current EGL context.
         */
        public Texture2dProgram(ProgramType programType) {
            mProgramType = programType;

            switch (programType) {
                case TEXTURE_EXT:
                    mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
                    break;
                case TEXTURE_EXT_BW:
                    mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
                    break;
                case TEXTURE_EXT_FILT:
                    mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
                    break;
                default:
                    throw new RuntimeException("Unhandled type " + programType);
            }
            if (mProgramHandle == 0) {
                throw new RuntimeException("Unable to create program");
            }
            Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

            // get locations of attributes and uniforms

            maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
            checkLocation(maPositionLoc, "aPosition");
            maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
            checkLocation(maTextureCoordLoc, "aTextureCoord");
            muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            checkLocation(muMVPMatrixLoc, "uMVPMatrix");
            muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
            checkLocation(muTexMatrixLoc, "uTexMatrix");
            muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
            if (muKernelLoc < 0) {
                // no kernel in this one
                muKernelLoc = -1;
                muTexOffsetLoc = -1;
                muColorAdjustLoc = -1;
            } else {
                // has kernel, must also have tex offset and color adj
                muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
                checkLocation(muTexOffsetLoc, "uTexOffset");
                muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
                checkLocation(muColorAdjustLoc, "uColorAdjust");

                // initialize default values
                setKernel(new float[] {0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f}, 0f);
                setTexSize(256, 256);
            }
        }

        /**
         * Releases the program.
         * <p>
         * The appropriate EGL context must be current (i.e. the one that was used to create
         * the program).
         */
        public void release() {
            Log.d(TAG, "deleting program " + mProgramHandle);
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = -1;
        }

        /**
         * Returns the program type.
         */
        public ProgramType getProgramType() {
            return mProgramType;
        }



        /**
         * Creates a texture object suitable for use with this program.
         * <p>
         * On exit, the texture will be bound.
         */
        public int createTextureObject() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            checkGlError("glGenTextures");

            int texId = textures[0];
            GLES20.glBindTexture(mTextureTarget, texId);
            checkGlError("glBindTexture " + texId);

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameter");

            return texId;
        }



        /**
         * Configures the convolution filter values.
         *
         * @param values Normalized filter values; must be KERNEL_SIZE elements.
         */
        public void setKernel(float[] values, float colorAdj) {
            if (values.length != KERNEL_SIZE) {
                throw new IllegalArgumentException("Kernel size is " + values.length +
                    " vs. " + KERNEL_SIZE);
            }
            System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
            mColorAdjust = colorAdj;
            //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
        }

        /**
         * Sets the size of the texture.  This is used to find adjacent texels when filtering.
         */
        public void setTexSize(int width, int height) {
            float rw = 1.0f / width;
            float rh = 1.0f / height;

            // Don't need to create a new array here, but it's syntactically convenient.
            mTexOffset = new float[] {
                -rw, -rh,   0f, -rh,    rw, -rh,
                -rw, 0f,    0f, 0f,     rw, 0f,
                -rw, rh,    0f, rh,     rw, rh
            };
            //Log.d(TAG, "filt size: " + surfWidth + "x" + outHeight + ": " + Arrays.toString(mTexOffset));
        }






        /**
         * Issues the draw call.  Does the full setup on every call.
         *
         * @param mvpMatrix The 4x4 projection matrix.
         * @param vertexBuffer Buffer with vertex position data.
         * @param firstVertex Index of first vertex to use in vertexBuffer.
         * @param vertexCount Number of vertices in vertexBuffer.
         * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
         * @param vertexStride Width, in bytes, of the position data for each vertex (often
         *        vertexCount * sizeof(float)).
         * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
         *        for use with SurfaceTexture.)
         * @param texBuffer Buffer with vertex texture data.
         * @param texStride Width, in bytes, of the texture data for each vertex.
         */

        public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                         int vertexCount, int coordsPerVertex, int vertexStride,
                         float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
            checkGlError("draw start");

            // Select the program.
            GLES20.glUseProgram(mProgramHandle);
            checkGlError("glUseProgram");

            // Set the texture.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(mTextureTarget, textureId);

            // Copy the model / surfView / projection matrix over.
            GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(maPositionLoc);
            checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
            checkGlError("glEnableVertexAttribArray");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
            checkGlError("glVertexAttribPointer");

            // Populate the convolution kernel, if present.
            if (muKernelLoc >= 0) {
                GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
                GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
                GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
            }

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            checkGlError("glDrawArrays");

            // Done -- disable vertex array, texture, and program.
            GLES20.glDisableVertexAttribArray(maPositionLoc);
            GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
            GLES20.glBindTexture(mTextureTarget, 0);
            GLES20.glUseProgram(0);
        }
    }







}
