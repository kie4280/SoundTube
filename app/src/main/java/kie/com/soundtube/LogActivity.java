package kie.com.soundtube;

import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class LogActivity extends AppCompatActivity implements LogFragment.Buttonlistener {

    HandlerThread thread;
    Handler worker;
    String error;
    LogFragment log;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log1);
        setTitle("Log");
        RelativeLayout frame = (RelativeLayout) findViewById(R.id.logRelative);
        thread = new HandlerThread("worker");
        thread.start();
        worker = new Handler(thread.getLooper());
        error = getIntent().getStringExtra("error message");
        error = "SoundTube version: " + BuildConfig.VERSION_CODE + "\n" + error;
//        TextView message = (TextView) findViewById(R.id.logText);
//        message.setText(error);
        FragmentManager manager = getFragmentManager();
        log = new LogFragment();
        Bundle bundle = new Bundle();
        bundle.putCharSequence("error_message", error);
        log.setArguments(bundle);
        log.show(manager, "LogFragment");
    }

    @Override
    protected void onDestroy() {
        Log.d("LogActivity", "onDestroy");
        thread.quit();
        thread = null;
//        log.dismiss();
        super.onDestroy();
//        System.exit(0);

    }


    @Override
    public void onclicked(boolean submit) {
        if (submit) {
            worker.post(new Runnable() {
                @Override
                public void run() {
                    Github github = new Github(getApplicationContext());
//                    github.report(error);
                    Log.d("LogActivity", error);
                    finish();

                }

            });
        } else {
            finish();
        }

    }
}
