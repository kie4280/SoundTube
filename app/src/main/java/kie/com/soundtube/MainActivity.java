package kie.com.soundtube;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements SearchFragment.OnFragmentInteractionListener, VideoFragment.OnFragmentInteractionListener {

    public static Handler UiHandler = null;
    public Fragment[] fragments = new Fragment[2];
    NonSwipeViewPager viewPager;
    TabLayout tabLayout;
    VideoFragment videoFragment;
    SearchFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiHandler = new Handler(Looper.getMainLooper());
        FragmentManager fragmentManager = getSupportFragmentManager();
        videoFragment = new VideoFragment();
        searchFragment = new SearchFragment();

        fragments[0] = searchFragment;
        fragments[1] = videoFragment;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frame, videoFragment, "video");
        fragmentTransaction.commit();

        YouTubeExtractor extractor = new YouTubeExtractor("xWzlwGVQ6_Q");
        YouTubeExtractor.YouTubeExtractorListener listener = new YouTubeExtractor.YouTubeExtractorListener() {
            @Override
            public void onSuccess(YouTubeExtractor.YouTubeExtractorResult result) {
                DataHolder dataHolder = new DataHolder();
                dataHolder.videoUri = result.getVideoUri();
                System.out.println("video uri  " + dataHolder.videoUri);
                videoFragment.playVideo(dataHolder);
            }

            @Override
            public void onFailure(Error error) {
                System.out.println(error.getMessage());

            }
        };
        extractor.startExtracting(listener, YouTubeExtractor.YOUTUBE_VIDEO_QUALITY_HD_720);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        System.out.println("activity destroy");
        super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onreturnVideo(DataHolder dataHolder) {
        viewPager.setCurrentItem(1, true);
        videoFragment.playVideo(dataHolder);
    }

    @Override
    public void onBackPressed() {
        System.out.println("activity back");
        super.onBackPressed();
    }
}