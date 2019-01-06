
package com.rustero.tools;


import android.hardware.Camera;
import android.util.Log;


import java.util.List;

@SuppressWarnings("deprecation")




public class Caminf {

    private static final String TAG = "CamC";
    public SizeList resolutions = new SizeList();


    public Caminf() {
    }


    public static Camera openFrontCamera() {
        Camera result = null;
        Camera.CameraInfo info = new Camera.CameraInfo();

        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = Camera.open(i);
                break;
            }
        }
        return result;
    }



    public static Camera openBackCamera() {
        Camera result = null;
        Camera.CameraInfo info = new Camera.CameraInfo();

        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                result = Camera.open(i);
                break;
            }
        }
        return result;
    }



    public static void addCameraSizes(Camera aCamera, SizeList aSili) {
        Camera.Parameters params = aCamera.getParameters();
        List<Camera.Size> casis = params.getSupportedVideoSizes();
        for (Camera.Size casi : casis) {
            aSili.addUnique(casi.width, casi.height);
        }
    }



    public static SizeList getCameraSizes(Camera aCamera) {
        SizeList result = new SizeList();
        Camera.Parameters params = aCamera.getParameters();
        List<Camera.Size> casis = params.getSupportedVideoSizes();
        for (Camera.Size casi : casis) {
            Log.i("VideoSize", "Supported Size: " + casi.width + "x" + casi.height);
            result.addSize(new Size2(casi.width, casi.height));
        }
        return result;
    }



    /**
     * Attempts to find a preview size that matches the provided width and outHeight (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     */
    public static void selectPreviewSize(Camera.Parameters aPars, int aWidth, int aHeight) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = aPars.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " + ppsfv.width + "x" + ppsfv.height);
        }

        for (Camera.Size size : aPars.getSupportedVideoSizes()) {
            if (size.width <= aWidth && size.height <= aHeight) {
                aPars.setPreviewSize(aWidth, aHeight);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + aWidth + "x" + aHeight);
        if (ppsfv != null) {
            aPars.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is

    }



    public static void selectPreferredSize(Camera.Parameters aPars) {
        Camera.Size size = aPars.getPreferredPreviewSizeForVideo();
        if (size != null) {
            Log.d(TAG, "Camera preferred preview size for video is " + size.width + "x" + size.height);
            aPars.setPreviewSize(size.width, size.height);
        } else {
            aPars.setPreviewSize(640, 480);
        }

    }



}
