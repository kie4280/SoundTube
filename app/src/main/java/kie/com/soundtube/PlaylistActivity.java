package kie.com.soundtube;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;


public class PlaylistActivity extends AppCompatActivity implements PlaylistFragment.OnFragmentInteractionListener {

    FragmentManager fragmentManager;
    PlaylistFragment playlistFragment;
    public DrawerLayout drawerLayout;
    public RelativeLayout mainRelativeLayout;
    Toolbar playlistToolbar;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_layout);
        context = getApplicationContext();
        playlistToolbar = (Toolbar) findViewById(R.id.playlistToolbar);
        playlistToolbar.setTitle("Playlists");

        setSupportActionBar(playlistToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);

        playlistFragment = new PlaylistFragment();
        fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                .add(R.id.mainRelativeLayout, playlistFragment, "playlistFragment")
//                .commit();

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
