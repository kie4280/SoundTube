package kie.com.soundtube;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class PlaylistFragment extends Fragment {

    RecyclerView recyclerView;
    private OnFragmentInteractionListener mListener;
    YoutubeClient youtubeClient;
    Handler netHandler;
    HandlerThread thread;
    PlayListManager playListManager;
    Context context;

    public PlaylistFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        thread = new HandlerThread("playListNetworking");
        thread.start();
        netHandler = new Handler(thread.getLooper());
        View playlistview = inflater.inflate(R.layout.playlist_layout, container, false);
        recyclerView = playlistview.findViewById(R.id.playListRecycler);
        youtubeClient = new YoutubeClient(getActivity(), netHandler);
        playListManager = PlayListManager.getInstance(getActivity());
        return playlistview;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        playListManager.write();
    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }
}
