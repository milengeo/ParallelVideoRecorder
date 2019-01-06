package com.rustero.units;


import java.nio.ByteBuffer;



public class MediaSample {
    public long time;
    public ByteBuffer data;


    public MediaSample(int aRoom) {
        super();
        data = ByteBuffer.allocate(aRoom);
    }


    public void clear() {
        data.clear();
    }


    public int getSize() {
        return data.limit();
    }


    public void putCode(byte aCode) {
        data.put(aCode);
        data.flip();
    }


    public void putData(MediaSample aSample) {
        data.clear();
        if (data.capacity() < aSample.data.limit())
            data = ByteBuffer.allocate(aSample.data.limit() * 2);
        data.put(aSample.data);
        data.flip();
    }


    public void getData(MediaSample aSample) {
        aSample.data.clear();
        if (aSample.data.capacity() < data.limit())
            aSample.data = ByteBuffer.allocate(data.limit() * 2);
        aSample.data.put(data);
        aSample.data.flip();
    }

}

