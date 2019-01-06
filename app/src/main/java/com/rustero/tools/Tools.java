package com.rustero.tools;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;

import com.rustero.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;

import android.util.TypedValue;


public class Tools {

    static final String TAG = "Tools";


    static public void delay(int aMills) {
        try {
            Thread.sleep(aMills);
        } catch (Exception ex) {}
        Log.d(TAG, "delay_99");
    }


    static public boolean folderExists(String aPath) {
        File folder = new File(aPath);
        boolean result = folder.isDirectory();
        return result;
    }


    static public String getFileExt(String aName) {
        String ext = "";
        int p = aName.lastIndexOf('.');
        if (p > 0)
            ext = aName.substring(p + 1);
        return ext;
    }


    static public int getIntAttr(Context aContext, int aId) {
        TypedValue typedValue = new TypedValue();
        int[] attrIds= new int[] { android.R.attr.actionBarSize };
        int attrIndex = 0;
        TypedArray ta = aContext.obtainStyledAttributes(typedValue.data, attrIds);
        int result = ta.getDimensionPixelSize(attrIndex, -1);
        ta.recycle();
        return result;
    }


    static public boolean isBlank(String aValue) {
        if (aValue != null && !aValue.isEmpty())
            return false;
        else
            return true;
    }


    static public String addSlash(String aValue) {
        String result = aValue;
        if (result.length() > 0)
          if (result.charAt(result.length()-1) != '/')
              result += '/';
        return result;
    }


    static public String upperDir(String aValue) {
        String result = "";
        int p = aValue.lastIndexOf('/');
        if (p > 0)
            result = aValue.substring(0, p);
        if (result.length() == 0)
            result = "/";
        return result;
    }


    public static String formatFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "bytes", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }



    public static String formatBitrate(long aByteRate) {
        if (aByteRate <= 0) return "0 kbps";
        String result = new DecimalFormat("#,##0.#").format(aByteRate*8/1000) + "kbps";
        return result;
    }


    public static String formatDuration(long aDura) {
        if (aDura <= 0) return "0:00:00";
        String result = String.format("%d:%02d:%02d", aDura / 3600, (aDura % 3600) / 60, (aDura % 60));
        return result;
    }

    public static void writePrivateFile(Context aContext, String aName, String aText) {
        try {
            FileOutputStream fos = aContext.openFileOutput(aName, Context.MODE_PRIVATE);
            fos.write(aText.getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(App.LOG_TAG, e.getMessage(), e);
        }
    }



    public static String readPrivateFile(Context aContext, String aName) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = aContext.openFileInput(aName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            Log.e(App.LOG_TAG, e.getMessage(), e);
        }
        return sb.toString();
    }



    public static int parseInt(String aStr) {
        int result = 0;
        try {
            result = Integer.parseInt(aStr);
        } catch (NumberFormatException e) {
            result = 0;
        }
        return result;
    }



    public static int getAboveInt(List<String> aList, int aValue) {
        int result = -1;
        for (int i=0; i<aList.size(); i++) {
            int val = parseInt(aList.get(i));
            if (val >= aValue) {
                result = i;
                break;
            }
        }

        if (-1 == result) {
            if (aList.size() > 0)
                result = aList.size()-1;
        }

        return result;
    }


}
