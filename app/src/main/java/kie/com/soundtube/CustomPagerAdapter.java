package kie.com.soundtube;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by kieChang on 2017/7/14.
 */
public class CustomPagerAdapter extends PagerAdapter {

    ArrayList<Integer> showview = new ArrayList<>(3);
    ArrayList<View> pageviews = new ArrayList<>(3);
    boolean prePrev = false, preNext = false;
    int count = 1;

    public CustomPagerAdapter(ArrayList<View> pageviews) {
        this.pageviews = pageviews;
        showview.add(1);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(pageviews.get(showview.get(position)));
        return pageviews.get(showview.get(position));
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }


    public void changeSate(boolean hasnext, boolean hasprev) {
        showview.clear();
        count = 1;
        if (hasprev) {
            showview.add(0);
            count++;
        }
        showview.add(1);
        if (hasnext) {
            showview.add(2);
            count++;
        }
        if (hasnext != preNext || hasprev != prePrev) {
            notifyDataSetChanged();
        }
        preNext = hasnext;
        prePrev = hasprev;

    }
}
