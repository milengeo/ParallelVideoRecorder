package com.rustero.tools;


import java.util.ArrayList;
import java.util.Collections;


public class SizeList {
    private ArrayList<Size2> mList = new ArrayList<Size2>();

    
    public void clear() {
        mList.clear();
    }


    public int length() {
        return mList.size();
    }


    public void sort() {
        Collections.sort(mList);
    }



    public Size2 get(int aIndex) {
        Size2 result = null;
        if (aIndex < mList.size())
            result = mList.get(aIndex);
        return result;
    }



    public boolean hasSize(Size2 aSize) {
        boolean result = false;
        for (Size2 size : mList) {
            if (size.width == aSize.width && size.height == aSize.height ) {
                result = true;
                break;
            }
        }
        return result;
    }



    public void addSize(Size2 aSize) {
        mList.add(aSize);
    }



    public void addUnique(int aWidth, int aHeight) {
        Size2 size = new Size2(aWidth, aHeight);
        boolean hasit = hasSize(size);
        if (hasit) return;
        addSize(size);
    }



    public int findByHeight(int aHeight) {
        int result = -1;
        for (int i=0; i<mList.size(); i++) {
            Size2 size = mList.get(i);
            if (size.height >= aHeight) {
                result = i;
                break;
            }
        }
        return result;
    }


    public int findSize(int aWidth, int aHeight) {
        int result = -1;
        for (int i=0; i<mList.size(); i++) {
            Size2 size = mList.get(i);
            if ((size.width == aWidth) && (size.height == aHeight)) {
                result = i;
                break;
            }
        }
        return result;
    }





    public Size2 getAboveHeight(int aHeight) {
        Size2 result = null;
        for (int i=0; i<mList.size(); i++) {
            Size2 size = mList.get(i);
            if (size.height >= aHeight) {
                result = size;
                break;
            }
        }
        if (null == result) {
            if (mList.size() > 0)
                result = mList.get(mList.size()-1);
        }
        return result;
    }



    public Size2 getBelowHeight(int aHeight) {
        Size2 result = null;
        for (int i=mList.size()-1; i>=0; i--) {
            Size2 size = mList.get(i);
            if (size.height <= aHeight) {
                result = size;
                break;
            }
        }
        if (null == result) {
            if (mList.size() > 0)
                result = mList.get(0);
        }
        return result;
    }



    public Size2 getLargest() {
        if (mList.size() > 0)
            return mList.get(mList.size()-1);
        else
            return null;
    }


}
