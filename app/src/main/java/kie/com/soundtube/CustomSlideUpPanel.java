package kie.com.soundtube;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

/**
 * Created by kieChang on 2017/9/30.
 */

public class CustomSlideUpPanel extends SlidingUpPanelLayout {
    boolean isTouchable = false;

    public CustomSlideUpPanel(Context context) {
        super(context);
    }

    public CustomSlideUpPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSlideUpPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

//        int action = ev.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                Log.d("customslideupPanel", "intercept down");
//                break;
//            case MotionEvent.ACTION_UP:
//                Log.d("customslideupPanel", "intercept up");
//                break;
//            default:
//                break;
//        }
        if (getPanelState() == PanelState.DRAGGING) {
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        Log.d("customslideupPanel", "touch");
//        int action = ev.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                Log.d("customslideupPanel", "down");
//                break;
//            case MotionEvent.ACTION_UP:
//                Log.d("customslideupPanel", "up");
//                break;
//            default:
//                break;
//        }
        return super.onTouchEvent(ev);
    }
}
