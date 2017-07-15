package kie.com.soundtube;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by kieChang on 2017/7/14.
 */
public class CustomPagerAdapter extends PagerAdapter {

    ArrayList<View> arrayList = new ArrayList<>();
    int count = 3;

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(arrayList.get(position));
        return arrayList.get(position);
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

    public void addView(View view) {
        arrayList.add(view);
    }

    public void removeView(View view) {
        arrayList.remove(view);
    }
}
