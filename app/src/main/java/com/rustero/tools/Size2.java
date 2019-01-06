package com.rustero.tools;




public class Size2 implements Comparable<Size2> {
    public int width, height;

    @Override
    public int compareTo(Size2 o) {
        int result = 0;
        if (o.height < height)
            result = 1;
        else if (o.height > height)
            result = -1;
        else if (o.width < width)
            result = 1;
        else if (o.width > width)
            result = -1;
        return result;
    }


    public Size2(int aWidth, int aHeight) {
        width = aWidth;
        height = aHeight;
    }


    public String getResolutio() {
        String result = width + "x" + height;
        return result;
    }

}
