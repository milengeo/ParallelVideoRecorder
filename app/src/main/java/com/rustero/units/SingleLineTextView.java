package com.rustero.units;

import android.content.Context;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Forked from: http://androidxref.com/4.1.1/xref/frameworks/base/core/java/com/android/internal/widget/DialogTitle.java
 * Created by LeoLink on 2014-06-30.
 */
public class SingleLineTextView extends TextView {

    public SingleLineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
    }

    public SingleLineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
    }

    public SingleLineTextView(Context context) {
        super(context);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Layout layout = getLayout();
        if (layout != null) {
            final int lineCount = layout.getLineCount();
            if (lineCount > 0) {
                final int ellipsisCount = layout.getEllipsisCount(lineCount - 1);
                if (ellipsisCount > 0) {

                    final float textSize = getTextSize();

                    // textSize is already expressed in pixels
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize - 1));

                    // recursion
                    measure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }
    }
}