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
    Handler worker;
    boolean checkedLatest = false;
    boolean downloaded = false;
    Toast toast = null;
    Github github;

    private OnFragmentInteractionListener mListener;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
        github = new Github(context);
        thread = new HandlerThread("worker");
        thread.start();
        worker = new Handler(thread.getLooper());
        addPreferencesFromResource(R.xml.settingpreference);
        Preference checkupdate = (Preference) findPreference("checkupdate");
        checkupdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                updateSoftware();
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        if (toast != null) {
            toast.cancel();
        }

        Log.d("Setting", "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (toast != null) {
            toast.cancel();
        }
        thread.quit();
        thread = null;
    }

    public void updateSoftware() {

        if (!checkedLatest) {
            checkedLatest = true;
            worker.post(new Runnable() {
                @Override
                public void run() {

                    String in = github.getupdate();
                    if (!in.contentEquals("latest")) {
                        if (isStoragePermissionGranted()) {
                            if (!downloaded) {
                                showText(R.string.software_download);
                                DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                Request request = new Request(Uri.parse(in));
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                                        "SoundTube" + github.versionName + ".apk");
                                request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);

                                manager.enqueue(request);

                                downloaded = true;
                            } else {
                                showText(R.string.already_download);
                                Log.d("Setting", getString(R.string.already_download));
                            }

                        } else {
                            showText(R.string.deny_write_permission);
                            Log.d("Setting", getString(R.string.deny_write_permission));
                        }

                    } else {
                        showText(R.string.software_latest);

                        Log.d("Setting", getString(R.string.software_latest));
                    }

                }

            });

        }

    }

    private void showText(final int id) {
        if (toast == null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast = Toast.makeText(context, id, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });

        }
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
