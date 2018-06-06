package kie.com.soundtube;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 * Created by kieChang on 2017/7/13.
 */

interface OnItemClicked {
    void onClick(View view, int position);

    void onLongClick(View view, int position);
}

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

    OnItemClicked clickListener = null;
    GestureDetector gestureDetector;
    SlidingUpPanelLayout panel = null;
    Context context;
    float prevX = 0;
    float prevY = 0;
    float thresholdX = 15f;
    float thresholdY = 15f;
    long totalX = 0;
    long totalY = 0;


    public void setSlidePanel(SlidingUpPanelLayout panel) {
        this.panel = panel;
    }

    public RecyclerTouchListener(Context context, final RecyclerView rv, final OnItemClicked listener) {
        clickListener = listener;
        this.context = context;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                if (Math.abs(motionEvent.getX() - prevX) <= thresholdX &&
                        Math.abs(motionEvent.getY() - prevY) <= thresholdY && totalX <= thresholdX && totalY <= thresholdY) {
                    View child = rv.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
                    if (child != null) {
                        Rect rect = new Rect();
                        child.findViewById(R.id.video_option).getGlobalVisibleRect(rect);
                        boolean handled = rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                        if (listener != null && !handled)
                            clickListener.onClick(child, rv.getChildAdapterPosition(child));
                    }
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                if (Math.abs(motionEvent.getX() - prevX) <= thresholdX &&
                        Math.abs(motionEvent.getY() - prevY) <= thresholdY && totalX <= thresholdX && totalY <= thresholdY) {
                    View child = rv.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
                    if (child != null) {
                        Rect rect = new Rect();
                        View c = child.findViewById(R.id.video_option);
                        if (c != null) {
                            c.getGlobalVisibleRect(rect);
                        }
                        boolean handled = rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                        if (listener != null && !handled)
                            clickListener.onLongClick(child, rv.getChildAdapterPosition(child));
                    }
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(final RecyclerView rv, MotionEvent e) {

        int action = e.getAction();
        gestureDetector.onTouchEvent(e);
        if (action == MotionEvent.ACTION_DOWN) {
            if (panel != null) {
                panel.setTouchEnabled(false);
            }
            prevX = e.getX();
            prevY = e.getY();
            totalX = 0;
            totalY = 0;
        } else if (action == MotionEvent.ACTION_MOVE) {
            totalX += Math.abs(e.getX() - prevX);
            totalY += Math.abs(e.getY() - prevY);
        } else if (action == MotionEvent.ACTION_UP && panel != null) {
            panel.setTouchEnabled(true);
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
