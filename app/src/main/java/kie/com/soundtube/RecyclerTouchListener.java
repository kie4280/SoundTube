package kie.com.soundtube;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
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

    SlidingUpPanelLayout panel = null;
    Context context;
    float prevX = 0;
    float prevY = 0;
    float thresholdX = 15f;
    float thresholdY = 15f;


    public void setSlidePanel(SlidingUpPanelLayout panel) {
        this.panel = panel;
    }

    public RecyclerTouchListener(Context context, final OnItemClicked listener) {
        clickListener = listener;
        this.context = context;
    }

    @Override
    public boolean onInterceptTouchEvent(final RecyclerView rv, MotionEvent e) {

        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (panel != null) {
                panel.setTouchEnabled(false);
            }
            prevX = e.getX();
            prevY = e.getY();
        } else if (action == MotionEvent.ACTION_UP) {
            if (panel != null) {
                panel.setTouchEnabled(true);
            }
            if (Math.abs(e.getX() - prevX) <= thresholdX &&
                    Math.abs(e.getY() - prevY) <= thresholdY) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && clickListener != null) {
                    clickListener.onClick(child, rv.getChildAdapterPosition(child));
                }
            }
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
