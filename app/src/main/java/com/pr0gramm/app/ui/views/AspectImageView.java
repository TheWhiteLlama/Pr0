package com.pr0gramm.app.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.pr0gramm.app.R;

/**
 * A image that measures it height to the same value
 * as its width.
 */
public class AspectImageView extends ImageView {
    private float aspect;

    public AspectImageView(Context context) {
        this(context, null, 0);
    }

    public AspectImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AspectImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // apply attributes
        Resources.Theme theme = context.getTheme();
        TypedArray arr = theme.obtainStyledAttributes(attrs, R.styleable.AspectImageView, 0, 0);
        try {
            aspect = arr.getFloat(R.styleable.AspectImageView_aspect, 1.f);
        } finally {
            arr.recycle();
        }
    }

    /**
     * Sets the aspect of this image. This will not trigger a relayout.
     *
     * @param aspect Sets the aspect of the image.
     */
    public void setAspect(float aspect) {
        if (this.aspect != aspect) {
            this.aspect = aspect;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, (int) (width / aspect));
    }
}