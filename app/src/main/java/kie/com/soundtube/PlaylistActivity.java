package kie.com.soundtube;

import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;


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
        playlistToolbar = (Toolbar) findViewById(R.id.playListToolBar);
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
