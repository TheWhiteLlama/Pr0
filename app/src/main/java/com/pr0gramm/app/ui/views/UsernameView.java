package com.pr0gramm.app.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.pr0gramm.app.api.pr0gramm.Info;

/**
 */
public class UsernameView extends TextView {
    public UsernameView(Context context) {
        super(context);
        init();
    }

    public UsernameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UsernameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode())
            setUsername("Mopsalarm", 2);
    }

    public void setMark(int mark) {
        if (mark < 0 || mark >= Info.MarkDrawables.size())
            mark = 4;

        // get the drawable for that mark
        setCompoundDrawablesWithIntrinsicBounds(0, 0, Info.MarkDrawables.get(mark), 0);
    }

    public void setUsername(String name, int mark) {
        setText(name + " ");
        setMark(mark);
    }
}
