package kie.com.soundtube;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 * Created by kieChang on 2017/5/18.
 */
public class CustomViewPager extends ViewPager {

    SlidingUpPanelLayout panel = null;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSlidePanel(SlidingUpPanelLayout panel) {
        this.panel = panel;

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("view pager intercept", MotionEvent.actionToString(ev.getAction()));
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_UP && panel != null) {
            panel.setTouchEnabled(true);
        }
        return super.onTouchEvent(ev);

    }


}
