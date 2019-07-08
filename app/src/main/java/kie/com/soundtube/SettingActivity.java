package kie.com.soundtube;

import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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


    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
