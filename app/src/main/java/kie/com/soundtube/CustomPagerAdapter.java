package kie.com.soundtube;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by kieChang on 2017/7/14.
 */
public class CustomPagerAdapter extends FragmentPagerAdapter {

    ArrayList<Fragment> fragments;


    public CustomPagerAdapter(FragmentManager fm, @NonNull ArrayList<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }


    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }


    @Override
    public int getCount() {
        return fragments.size();
    }
}
