package kie.com.soundtube;

import android.Manifest;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;

public class SettingActivity extends AppCompatActivity implements SettingFragment.OnFragmentInteractionListener {

    Toolbar settingToolbar;
    LinearLayout linearfragment;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        fragmentManager = getFragmentManager();
        settingToolbar = (Toolbar) findViewById(R.id.settingToolbar);
        linearfragment = (LinearLayout) findViewById(R.id.linearSetting);
        setSupportActionBar(settingToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(null);
        SettingFragment settingFragment = new SettingFragment();

        fragmentManager.beginTransaction()
                .add(R.id.linearSetting, settingFragment, "SettingFragment")
                .commit();

    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}