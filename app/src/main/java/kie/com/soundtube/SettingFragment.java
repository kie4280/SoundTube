package kie.com.soundtube;

import android.Manifest;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class SettingFragment extends PreferenceFragment {

    Context context;
    HandlerThread thread;

    private OnFragmentInteractionListener mListener;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
        thread = new HandlerThread("worker");
        thread.start();
        addPreferencesFromResource(R.xml.settingpreference);
        Preference preference = (Preference) findPreference("checkupdate");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                updateSoftware();
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quit();
        thread = null;
    }

    public void updateSoftware() {

        final Handler worker = new Handler(thread.getLooper());

        worker.post(new Runnable() {
            @Override
            public void run() {
                Github github = new Github();
                String in = github.getupdate();
                if (!in.contentEquals("latest")) {
                    if (isStoragePermissionGranted()) {
                        Toast toast = Toast.makeText(context, R.string.software_download, Toast.LENGTH_SHORT);
                        toast.show();
                        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Request request = new Request(Uri.parse(in));
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                                "SoundTube" + github.versionName + ".apk");
                        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
                        manager.enqueue(request);
                    } else {
                        Toast toast = Toast.makeText(context, R.string.deny_write_permission, Toast.LENGTH_SHORT);
                        toast.show();
                    }

                } else {
                    Toast toast = Toast.makeText(context, R.string.software_latest, Toast.LENGTH_SHORT);
                    toast.show();
                }

            }

        });


    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("SettingActivity", "Permission is granted");
                return true;
            } else {

                Log.v("SettingActivity", "Permission is revoked");
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("SettingActivity", "Permission is granted");
            return true;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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


    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }
}
