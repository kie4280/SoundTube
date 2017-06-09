package kie.com.soundtube;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by kieChang on 2017/5/18.
 */
public class CustomViewPager extends ViewPager {

    private boolean swipable = true;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return swipable && super.onInterceptTouchEvent(ev);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        return swipable && super.onTouchEvent(ev);
    }

    public void setSwipingEnabled(boolean swipingEnabled) {
        this.swipable = swipingEnabled;
    }
}
