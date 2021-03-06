package com.pr0gramm.app.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class VerticallyUnboundedFrameLayout extends FrameLayout {
    private static final Logger logger = LoggerFactory.getLogger(VerticallyUnboundedFrameLayout.class);

    public VerticallyUnboundedFrameLayout(Context context) {
        super(context);
    }

    public VerticallyUnboundedFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticallyUnboundedFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void measureChildWithMargins(@NonNull View child,
                                           int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {

        super.measureChildWithMargins(child,
                parentWidthMeasureSpec, widthUsed,
                MeasureSpec.UNSPECIFIED, heightUsed);
    }
}
