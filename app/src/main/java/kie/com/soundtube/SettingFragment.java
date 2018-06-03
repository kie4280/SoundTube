package kie.com.soundtube;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import pub.devrel.easypermissions.EasyPermissions;

public class SettingFragment extends PreferenceFragment {

    Context context;
    HandlerThread thread;
    Handler worker;
    boolean checkedLatest = false;
    boolean downloaded = false;
    Toast toast = null;
    Github github;
    final int REQUEST_WRITE_PERMISSION = 1001;

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

    private void SignIn() {


    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (toast != null) {
            toast.cancel();
        }

        Log.d("Setting", "onStop");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (toast != null) {
            toast.cancel();
        }
        thread.quitSafely();
        thread = null;
    }

    private void updateSoftware() {

        if (!checkedLatest) {
            checkedLatest = true;
            worker.post(new Runnable() {
                @Override
                public void run() {
                    String in = github.getupdate();
                    if (downloaded) {
                        showText(R.string.already_download);
                        Log.d("Setting", getString(R.string.already_download));
                    } else if (!in.contentEquals("latest")) {
                        showText(R.string.software_latest);
                        Log.d("Setting", getString(R.string.software_latest));
                    } else if (EasyPermissions.hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        EasyPermissions.requestPermissions(getActivity(), "This app needs to access your storage",
                                REQUEST_WRITE_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else {

                        showText(R.string.software_download);
                        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Request request = new Request(Uri.parse(in));
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                                "SoundTube" + github.versionName + ".apk");
                        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);

                        manager.enqueue(request);

                        downloaded = true;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (resultCode != Activity.RESULT_OK) {
                    showText(R.string.deny_write_permission);
                    Log.d("Setting", getString(R.string.deny_write_permission));

                } else if (!PlayerActivity.netConncted) {
                    showText(R.string.no_network);
                    Log.d("Setting", getString(R.string.no_network));
                } else {
                    updateSoftware();
                }
                break;
            default:
                break;
        }

    }

    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }
}
