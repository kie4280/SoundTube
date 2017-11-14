package kie.com.soundtube;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by kieChang on 2017/7/14.
 */
public class CustomPagerAdapter1 extends PagerAdapter {

    ArrayList<Integer> showview = new ArrayList<>(3);
    ArrayList<View> pageviews = new ArrayList<>(3);
    boolean prePrev = false, preNext = false;
    Handler ui;


    public CustomPagerAdapter1(ArrayList<View> pageviews) {
        this.pageviews = pageviews;
        ui = new Handler(Looper.getMainLooper());
        showview.add(1);

    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = pageviews.get(showview.get(position));
//        View view = pageviews.get(position);
        Log.d("pageradapter", "initiate" + Integer.toString(position));
        container.addView(view);
        return view;
    }

    @Override
    public int getItemPosition(Object object) {

        int index = pageviews.indexOf(object);
//        Log.d("pageradapter", "viewindex" + Integer.toString(index));
        if (showview.contains(index)) {
            return showview.indexOf(index);
        } else {
            return POSITION_NONE;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        Log.d("pageradapter", "destroyitem");
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
//        Log.d("pagecount", Integer.toString(showview.size()));
        return showview.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }


    public void changeSate(final boolean hasnext, final boolean hasprev) {

        ui.post(new Runnable() {
            @Override
            public void run() {
                showview.clear();
                if (hasprev) {
                    showview.add(0);
                }
                showview.add(1);
                if (hasnext) {
                    showview.add(2);
                }
                if (hasnext != preNext || hasprev != prePrev) {
                    Log.d("pageradapter", "datachanged");
                    notifyDataSetChanged();
                }
                preNext = hasnext;
                prePrev = hasprev;
            }
        });


    }

}
